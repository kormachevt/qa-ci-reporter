pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
        maven {
            setUrl("https://jitpack.io")
        }
        flatDir {
            dir("./plugin-build/plugin/src/main/java/com/github/kormachevt/qa/ci/reporter/plugin/lib")
        }
    }
}

rootProject.name = ("qa-ci-reporter-plugin")

include(":example")
includeBuild("plugin-build")
