package org.example.hrupgrade;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

final class PackUpgradeDialog extends DialogWrapper {

    private final JBCheckBox backend = new JBCheckBox("包含后端 (Maven package)", true);
    private final JBCheckBox frontend = new JBCheckBox("包含前端 (pnpm build:antd)", true);
    private final JBCheckBox openFolder = new JBCheckBox("完成后打开输出目录", false);
    private final JBCheckBox skipUpload = new JBCheckBox("跳过 SCP 上传（即使配置了 upload）", false);
    private final JBCheckBox remoteApply = new JBCheckBox("上传后 SSH 直连服务器自动更新（Docker apply）", true);

    PackUpgradeDialog() {
        super(true);
        setTitle("生成 / 远程更新 HR");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addComponent(backend)
                .addComponent(frontend)
                .addComponent(remoteApply)
                .addComponent(skipUpload)
                .addComponent(openFolder)
                .addComponent(new JLabel("<html><body style='width:380px;color:gray'>"
                        + "调用仓库 <b>deploy/upgrade/pack-upgrade</b>："
                        + "本机打包 →（可选）SCP →（可选）SSH 在服务器解压并 <b>apply</b>。"
                        + "<br/>远程更新需 <b>hr-upgrade.json</b> 中 upload.enabled=true，"
                        + "且本机已配置 SSH 免密。"
                        + "<br/>服务器路径默认 HR_HOME=<b>/opt/hr-management/deploy</b>。"
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

    boolean isRemoteApply() {
        return remoteApply.isSelected() && !skipUpload.isSelected();
    }
}
