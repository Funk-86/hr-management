package org.example.hrupgrade;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class PackUpgradeAction extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Path repoRoot = ProjectPaths.hrRepoRoot(project);
        if (repoRoot == null) {
            showNotification(project, "无法识别项目根目录", NotificationType.ERROR);
            return;
        }
        Path script = ProjectPaths.upgradeScriptDir(repoRoot);
        if (!Files.isDirectory(script)) {
            showNotification(project, "未找到 deploy/upgrade，请在 hr-management 仓库中打开项目", NotificationType.ERROR);
            return;
        }

        PackUpgradeDialog dialog = new PackUpgradeDialog();
        if (!dialog.showAndGet()) {
            return;
        }
        if (!dialog.isIncludeBackend() && !dialog.isIncludeFrontend()) {
            showNotification(project, "请至少勾选后端或前端", NotificationType.WARNING);
            return;
        }

        boolean includeBackend = dialog.isIncludeBackend();
        boolean includeFrontend = dialog.isIncludeFrontend();
        boolean openFolder = dialog.isOpenFolder();
        boolean skipUpload = dialog.isSkipUpload();
        boolean remoteApply = dialog.isRemoteApply();

        String title = remoteApply ? "正在打包并远程更新 HR…" : "正在生成 HR 升级包…";
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    PackUpgradeRunner.Result result = PackUpgradeRunner.run(
                            repoRoot,
                            includeBackend,
                            includeFrontend,
                            skipUpload,
                            remoteApply,
                            line -> indicator.setText2(trimLine(line))
                    );
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (result.exitCode() == 0) {
                            Path latest = findLatestZip(result.outputDir()).orElse(result.outputDir());
                            String msg = remoteApply
                                    ? "已打包并远程更新服务器。产物: " + latest
                                    : "升级包已生成: " + latest;
                            showNotification(project, msg, NotificationType.INFORMATION);
                            if (openFolder) {
                                openPath(project, result.outputDir());
                            }
                        } else {
                            showNotification(project,
                                    "失败 (exit=" + result.exitCode() + ")，请查看日志末尾:\n"
                                            + tail(result.log(), 1200),
                                    NotificationType.ERROR);
                        }
                    });
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            showNotification(project, "异常: " + ex.getMessage(), NotificationType.ERROR));
                }
            }
        });
    }

    private static String trimLine(String line) {
        return line.length() > 120 ? line.substring(0, 117) + "..." : line;
    }

    private static String tail(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(t.length() - max);
    }

    private static Optional<Path> findLatestZip(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".zip") || n.endsWith(".tar.gz");
                    })
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    static void openPath(Project project, Path path) {
        try {
            Files.createDirectories(path);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
                return;
            }
        } catch (Exception ignored) {
            // fall through
        }
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(path.toString().replace('\\', '/'));
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        }
    }

    static void showNotification(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("HR Upgrade")
                .createNotification(content, type)
                .notify(project);
    }
}
