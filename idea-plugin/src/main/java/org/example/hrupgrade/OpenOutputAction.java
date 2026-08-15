package org.example.hrupgrade;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class OpenOutputAction extends AnAction implements DumbAware {

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
        PackUpgradeAction.openPath(project, ProjectPaths.outputDir(repo));
    }
}
