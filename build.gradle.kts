import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.2.1")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.20.0"
}

allprojects {
    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    checkForGradleUpdate = true
        resolutionStrategy {
            componentSelection {
                all {
                    val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview")
                      .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
                      .any { it.matches(candidate.version)
                }
                if (rejected) {
                    reject("Release candidate")
                }
            }
        }
    }
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}
