package org.example.hrupgrade;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class OpenConfigAction extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Path repo = ProjectPaths.hrRepoRoot(project);
        if (repo == null) {
            PackUpgradeAction.showNotification(project, "无法识别项目根目录", NotificationType.ERROR);
            return;
        }
        Path cfg = ProjectPaths.configFile(repo);
        try {
            if (!Files.exists(cfg)) {
                Path example = ProjectPaths.exampleConfig(repo);
                if (Files.isRegularFile(example)) {
                    Files.copy(example, cfg, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.writeString(cfg, """
                            {
                              "frontendPath": "D:/vue/vue-vben-admin-main",
                              "includeBackend": true,
                              "includeFrontend": true,
                              "skipTests": true,
                              "outputDir": "dist/upgrades",
                              "upload": { "enabled": false }
                            }
                            """);
                }
            }
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(cfg.toFile());
            if (vf != null) {
                FileEditorManager.getInstance(project).openFile(vf, true);
            } else {
                PackUpgradeAction.showNotification(project, "已创建配置: " + cfg, NotificationType.INFORMATION);
            }
        } catch (Exception ex) {
            PackUpgradeAction.showNotification(project, "打开配置失败: " + ex.getMessage(), NotificationType.ERROR);
        }
    }
}
