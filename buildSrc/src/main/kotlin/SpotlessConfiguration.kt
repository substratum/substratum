/*
 * Copyright (c) 2016-2019 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

val kotlinLicenseHeader = """/*
 * Copyright (c) 2016-2019 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */
""".trimIndent()

fun Project.configureSpotless() {
    apply<SpotlessPlugin>()

    configure<SpotlessExtension> {
        format("misc") {
            target(
                fileTree(
                    mapOf(
                        "dir" to ".",
                        "include" to listOf("**/*.md", "**/.gitignore", "**/*.yaml", "**/*.yml"),
                        "exclude" to listOf(".gradle/**", ".gradle-cache/**", "**/tools/**", "**/build/**")
                    )
                )
            )
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }

        format("xml") {
            target("**/res/**/*.xml")
            indentWithSpaces(4)
            trimTrailingWhitespace()
            endWithNewline()
        }

        kotlinGradle {
            target("**/*.gradle.kts", "*.gradle.kts")
            ktlint("0.33.0").userData(mapOf("indent_size" to "4", "continuation_indent_size" to "4"))
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeader(kotlinLicenseHeader, "import|tasks|apply|plugins|include")
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }
    }
}
