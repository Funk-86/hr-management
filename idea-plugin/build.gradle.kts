plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    val javaVersion = providers.gradleProperty("javaVersion").get()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion"),
        )
        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "org.example.hrupgrade"
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = "241"
            // 兼容到 2025.2+（IU-252.*）；后续大版本再按需放宽
            untilBuild = "253.*"
        }
        description = """
            一键为智汇人事系统生成跨平台升级包（Windows / Linux）。
            工具菜单 → HR Upgrade → 生成升级包。
            会调用仓库内 deploy/upgrade/pack-upgrade 脚本，产出 zip 与 apply 脚本。
        """.trimIndent()
        changeNotes = """
            <ul>
              <li>1.0.2 修复 Windows PowerShell 编码导致的打包脚本解析失败</li>
              <li>1.0.1 兼容 IntelliJ IDEA 2025.2（build 252）</li>
              <li>1.0.0 首次发布：一键打包后端 jar / 前端 dist，可选 SCP 上传</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    wrapper {
        gradleVersion = "8.10.2"
    }
}
