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

rootProject.name = ("com.tkormachev.kotlin.gradle.test")

include(":plugin")
