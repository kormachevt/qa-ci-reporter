pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://jitpack.io")
        }
    }
}

rootProject.name = ("com.github.kormachevt.qa.ci.reporter.plugin")

include(":plugin")
