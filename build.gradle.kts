import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType

buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.3.2")
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
        resolutionStrategy {
            componentSelection {
                all {
                    val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview")
                      .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
                      .any { it.matches(candidate.version) }
                    if (rejected) {
                        reject("Release candidate")
                    } else if (candidate.group.equals("commons-io") && candidate.version.toFloat() >= 2.6F) {
                        reject("commons-io v2.6+ cause runtime crashes due to missing java.nio.File class in Android 7")
                    }
                }
            }
        }
    }
    wrapper {
        gradleVersion = "5.3"
        distributionType = DistributionType.ALL
    }
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }
}
