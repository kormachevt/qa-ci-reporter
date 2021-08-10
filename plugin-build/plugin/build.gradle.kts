import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("com.github.johnrengelman.shadow") version ("7.0.0")
}

configurations {
    create("provided")
    compile.extendsFrom(configurations.getByName("provided"))
}

repositories {
    flatDir {
        dir("./src/main/java/com/github/kormachevt/qa/ci/reporter/plugin/lib")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    implementation(gradleApi())
    implementation("com.github.jkcclemens:khttp:0.1.0")
    "provided"(fileTree("./src/main/java/com/github/kormachevt/qa/ci/reporter/plugin/lib") { include("*.jar") })
    testImplementation(TestingLib.JUNIT)
}

val jar by tasks.getting(Jar::class) {
    manifest { attributes["Main-Class"] = "ReportingPlugin" }
    from(sourceSets.main.get().output) {
        include("**/ReportingPlugin.class")
    }
    dependsOn(configurations.getByName("provided"))
    from({
        configurations.getByName("provided").filter {
            it.name.endsWith("jar")
        }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create(PluginCoordinates.ID) {
            id = PluginCoordinates.ID
            implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
            version = PluginCoordinates.VERSION
        }
    }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
    website = PluginBundle.WEBSITE
    vcsUrl = PluginBundle.VCS
    description = PluginBundle.DESCRIPTION
    tags = PluginBundle.TAGS

    plugins {
        getByName(PluginCoordinates.ID) {
            displayName = PluginBundle.DISPLAY_NAME
        }
    }

    mavenCoordinates {
        groupId = PluginCoordinates.GROUP
        artifactId = PluginCoordinates.ID
        version = PluginCoordinates.VERSION
    }
}

tasks.create("setupPluginUploadFromEnvironment") {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}
