pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
        flatDir {
            dir("./plugin-build/plugin/src/main/java/com/github/kormachevt/qa/ci/reporter/plugin/lib")
        }
        jcenter()
    }
}

rootProject.name = ("qa-ci-reporter-plugin")

include(":example")
includeBuild("plugin-build")
