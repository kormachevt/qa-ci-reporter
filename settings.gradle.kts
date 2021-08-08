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

rootProject.name = ("kotlin-gradle-reporting-plugin")

include(":example")
includeBuild("plugin-build")
