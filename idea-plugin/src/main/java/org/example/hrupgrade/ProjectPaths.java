package org.example.hrupgrade;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class ProjectPaths {

    private ProjectPaths() {
    }

    static @Nullable Path projectRoot(Project project) {
        String base = project.getBasePath();
        if (base == null || base.isBlank()) {
            return null;
        }
        return Paths.get(base);
    }

    /** 兼容把 idea-plugin 当独立工程打开：向上找 deploy/upgrade */
    static @Nullable Path hrRepoRoot(Project project) {
        Path root = projectRoot(project);
        if (root == null) {
            return null;
        }
        if (Files.isRegularFile(root.resolve("deploy/upgrade/pack-upgrade.ps1"))
                || Files.isRegularFile(root.resolve("deploy/upgrade/pack-upgrade.sh"))) {
            return root;
        }
        Path parent = root.getParent();
        if (parent != null
                && (Files.isRegularFile(parent.resolve("deploy/upgrade/pack-upgrade.ps1"))
                || Files.isRegularFile(parent.resolve("deploy/upgrade/pack-upgrade.sh")))) {
            return parent;
        }
        return root;
    }

    static Path upgradeScriptDir(Path repoRoot) {
        return repoRoot.resolve("deploy/upgrade");
    }

    static Path outputDir(Path repoRoot) {
        return repoRoot.resolve("dist/upgrades");
    }

    static Path configFile(Path repoRoot) {
        return repoRoot.resolve("hr-upgrade.json");
    }

    static Path exampleConfig(Path repoRoot) {
        return repoRoot.resolve("deploy/upgrade/hr-upgrade.example.json");
    }
}
