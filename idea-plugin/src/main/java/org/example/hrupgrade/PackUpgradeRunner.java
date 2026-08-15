package org.example.hrupgrade;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

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
            Consumer<String> lineConsumer
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
            if (skipUpload) {
                args.add("--skip-upload");
            }
        }
        cmd.addParameters(args);

        StringBuilder log = new StringBuilder();
        AtomicInteger exit = new AtomicInteger(-1);
        OSProcessHandler handler = new OSProcessHandler(cmd);
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
        handler.waitFor();

        return new Result(exit.get(), log.toString(), ProjectPaths.outputDir(repoRoot));
    }
}
