pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        flatDir {
            dirs("./plugin-build/plugin/src/main/java/com/tkormachev/kotlin/gradle/test/reporting/plugin/lib")
        }
        maven {
            setUrl("https://jitpack.io")
        }
    }
}

rootProject.name = ("kotlin-gradle-reporting-plugin")

include(":example")
includeBuild("plugin-build")
