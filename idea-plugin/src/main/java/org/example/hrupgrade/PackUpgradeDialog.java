package org.example.hrupgrade;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

final class PackUpgradeDialog extends DialogWrapper {

    private final JBCheckBox backend = new JBCheckBox("包含后端 (Maven package)", true);
    private final JBCheckBox frontend = new JBCheckBox("包含前端 (pnpm build:antd)", true);
    private final JBCheckBox openFolder = new JBCheckBox("完成后打开输出目录", true);
    private final JBCheckBox skipUpload = new JBCheckBox("跳过 SCP 上传（即使配置了 upload）", false);

    PackUpgradeDialog() {
        super(true);
        setTitle("生成 HR 升级包");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(backend)
                .addComponent(frontend)
                .addComponent(openFolder)
                .addComponent(skipUpload)
                .addComponent(new JLabel("<html><body style='width:360px;color:gray'>"
                        + "将调用仓库 <b>deploy/upgrade/pack-upgrade</b> 脚本，"
                        + "在 Windows / Linux 上均可生成 zip，并附带 apply/rollback 脚本。"
                        + "<br/>配置文件：项目根目录 <b>hr-upgrade.json</b>（可从 example 复制）。"
                        + "</body></html>"))
                .getPanel();
    }

    boolean isIncludeBackend() {
        return backend.isSelected();
    }

    boolean isIncludeFrontend() {
        return frontend.isSelected();
    }

    boolean isOpenFolder() {
        return openFolder.isSelected();
    }

    boolean isSkipUpload() {
        return skipUpload.isSelected();
    }
}
