package com.yjy.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("android.library")
                apply("android.hilt")
            }

            extensions.configure<LibraryExtension> {
                namespace = "com.yjy.data"

                val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
                dependencies {
                    add("implementation", project(":domain"))

                    add("testImplementation", libs.findLibrary("junit").get())

                    add("implementation", libs.findBundle("room").get())
                    add("ksp", libs.findLibrary("androidx-room-compiler").get())
                    add("implementation", libs.findBundle("retrofit").get())
                    add("implementation", libs.findLibrary("okhttp3").get())
                    add("implementation", libs.findLibrary("androidx-paging").get())
                    add("implementation", libs.findLibrary("androidx-datastore").get())
                }
            }
        }
    }
}