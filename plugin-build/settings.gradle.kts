pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
        jcenter()
    }
}

rootProject.name = ("com.github.kormachevt.qa.ci.reporter.plugin")

include(":plugin")
