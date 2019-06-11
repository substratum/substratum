import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.wrapper.Wrapper

buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.1")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.21.0"
}

allprojects {
    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

tasks {
    named<DependencyUpdatesTask>("dependencyUpdates") {
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }

    named<Wrapper>("wrapper") {
        gradleVersion = "5.4.1"
        distributionType = Wrapper.DistributionType.ALL
    }

    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}
