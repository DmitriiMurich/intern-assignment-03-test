import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    val configureDetekt: Project.() -> Unit = {
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            parallel = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            source.setFrom(files(projectDir.resolve("src")))
            basePath.set(rootProject.projectDir)
        }

        tasks.withType<Detekt>().configureEach {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/resources/**")
            jvmTarget.set("11")

            reports {
                html.required.set(true)
                md.required.set(true)
                sarif.required.set(true)
                xml.required.set(true)
            }
        }

        tasks.withType<DetektCreateBaselineTask>().configureEach {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/resources/**")
            jvmTarget.set("11")
        }
    }

    pluginManager.withPlugin("com.android.application") {
        pluginManager.apply("dev.detekt")
        configureDetekt()
    }
    pluginManager.withPlugin("com.android.library") {
        pluginManager.apply("dev.detekt")
        configureDetekt()
    }
    pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
        pluginManager.apply("dev.detekt")
        configureDetekt()
    }
}