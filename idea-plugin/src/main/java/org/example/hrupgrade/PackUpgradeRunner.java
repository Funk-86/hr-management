package org.example.hrupgrade;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class PackUpgradeRunner {

    record Result(int exitCode, String log, Path outputDir) {
    }

    private PackUpgradeRunner() {
    }

    static Result run(
            Path repoRoot,
            boolean includeBackend,
            boolean includeFrontend,
            boolean skipUpload,
            boolean remoteApply,
            Consumer<String> lineConsumer
    ) throws Exception {
        return run(repoRoot, includeBackend, includeFrontend, skipUpload, remoteApply, lineConsumer, null);
    }

    static Result run(
            Path repoRoot,
            boolean includeBackend,
            boolean includeFrontend,
            boolean skipUpload,
            boolean remoteApply,
            Consumer<String> lineConsumer,
            @Nullable ProgressIndicator indicator
    ) throws Exception {
        Path scriptDir = ProjectPaths.upgradeScriptDir(repoRoot);
        GeneralCommandLine cmd = new GeneralCommandLine();
        cmd.setCharset(StandardCharsets.UTF_8);
        cmd.setWorkDirectory(repoRoot.toFile());
        cmd.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);

        List<String> args = new ArrayList<>();
        if (SystemInfo.isWindows) {
            Path ps1 = scriptDir.resolve("pack-upgrade.ps1");
            if (!Files.isRegularFile(ps1)) {
                throw new IllegalStateException("未找到脚本: " + ps1);
            }
            // Windows PowerShell 5.1：用 powershell.exe + -File；脚本需 UTF-8 BOM
            cmd.setExePath("powershell.exe");
            args.add("-NoLogo");
            args.add("-NoProfile");
            args.add("-ExecutionPolicy");
            args.add("Bypass");
            args.add("-File");
            args.add(ps1.toAbsolutePath().toString());
            args.add("-IncludeBackend");
            args.add(includeBackend ? "true" : "false");
            args.add("-IncludeFrontend");
            args.add(includeFrontend ? "true" : "false");
            args.add("-RemoteApply");
            args.add(remoteApply ? "true" : "false");
            if (skipUpload) {
                args.add("-SkipUpload");
            }
        } else {
            Path sh = scriptDir.resolve("pack-upgrade.sh");
            if (!Files.isRegularFile(sh)) {
                throw new IllegalStateException("未找到脚本: " + sh);
            }
            cmd.setExePath("bash");
            args.add(sh.toAbsolutePath().toString());
            args.add("--include-backend");
            args.add(includeBackend ? "true" : "false");
            args.add("--include-frontend");
            args.add(includeFrontend ? "true" : "false");
            args.add("--remote-apply");
            args.add(remoteApply ? "true" : "false");
            if (skipUpload) {
                args.add("--skip-upload");
            }
        }
        cmd.addParameters(args);

        StringBuilder log = new StringBuilder();
        AtomicInteger exit = new AtomicInteger(-1);
        // Killable：取消时可强杀；Windows 下还需 taskkill /T 清掉 ssh/scp 子进程
        KillableProcessHandler handler = new KillableProcessHandler(cmd);
        handler.setShouldKillProcessSoftly(false);
        handler.addProcessListener(new ProcessAdapter() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                String text = event.getText();
                log.append(text);
                if (lineConsumer != null) {
                    for (String line : text.split("\\R")) {
                        if (!line.isBlank()) {
                            lineConsumer.accept(line);
                        }
                    }
                }
                if (outputType == ProcessOutputTypes.STDERR) {
                    // still captured in log
                }
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                exit.set(event.getExitCode());
            }
        });
        handler.startNotify();

        try {
            waitUntilDoneOrCancel(handler, indicator);
        } catch (ProcessCanceledException cancel) {
            forceKill(handler);
            throw cancel;
        }

        return new Result(exit.get(), log.toString(), ProjectPaths.outputDir(repoRoot));
    }

    /** 分段 wait，以便响应进度条「停止」；卡住的 ssh 也会被取消逻辑杀掉。 */
    private static void waitUntilDoneOrCancel(
            @NotNull ProcessHandler handler,
            @Nullable ProgressIndicator indicator
    ) {
        while (!handler.waitFor(400)) {
            if (indicator != null && indicator.isCanceled()) {
                if (lineSafe(indicator)) {
                    indicator.setText2("正在停止打包/SSH…");
                }
                forceKill(handler);
                // 再等最多 3s，避免停止按钮也一直转圈
                handler.waitFor(3000);
                throw new ProcessCanceledException();
            }
        }
    }

    private static boolean lineSafe(@Nullable ProgressIndicator indicator) {
        try {
            return indicator != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static void forceKill(@NotNull ProcessHandler handler) {
        Long pid = null;
        try {
            if (handler instanceof OSProcessHandler) {
                Process process = ((OSProcessHandler) handler).getProcess();
                if (process != null && process.isAlive()) {
                    pid = process.pid();
                }
            }
        } catch (Exception ignored) {
            // older JDKs / mock
        }

        try {
            handler.destroyProcess();
        } catch (Exception ignored) {
            // continue with tree kill
        }

        if (pid != null && SystemInfo.isWindows) {
            // PowerShell 下的 ssh/scp/mvn 是子进程，只杀父进程会留下孤儿导致「停止也卡住」
            try {
                new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid))
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();
            } catch (Exception ignored) {
                // best-effort
            }
        } else if (pid != null) {
            try {
                new ProcessBuilder("kill", "-9", "-" + pid)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();
            } catch (Exception ignored) {
                try {
                    new ProcessBuilder("kill", "-9", String.valueOf(pid))
                            .redirectErrorStream(true)
                            .start()
                            .waitFor();
                } catch (Exception ignored2) {
                    // best-effort
                }
            }
        }
    }
}
