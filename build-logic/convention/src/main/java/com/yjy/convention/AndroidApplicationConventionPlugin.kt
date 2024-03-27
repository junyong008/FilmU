package com.yjy.convention

import com.android.build.api.dsl.ApplicationExtension
import com.yjy.convention.project.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("android.hilt")
                apply("kotlin-kapt")
            }

            extensions.configure<ApplicationExtension> {
                namespace = "com.yjy.presentation"
                defaultConfig {
                    applicationId = "com.yjy.filmu"
                    targetSdk = 34
                    versionCode = 1
                    versionName = "1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                configureKotlinAndroid(this)

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
                dataBinding {
                    enable = true
                }

                val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
                dependencies {
                    add("implementation", project(":data"))
                    add("implementation", project(":domain"))

                    add("implementation", libs.findLibrary("androidx-core").get())
                    add("implementation", libs.findLibrary("androidx-appcompat").get())
                    add("implementation", libs.findLibrary("material").get())

                    add("testImplementation", libs.findLibrary("junit").get())
                    add("androidTestImplementation", libs.findLibrary("androidJunit").get())
                    add("androidTestImplementation", libs.findLibrary("espresso").get())

                    add("implementation", libs.findBundle("lifecycle").get())
                    add("implementation", libs.findLibrary("androidx-activity").get())
                    add("implementation", libs.findLibrary("androidx-fragment").get())
                    add("implementation", libs.findLibrary("androidx-constraintLayout").get())
                    add("implementation", libs.findLibrary("androidx-paging").get())

                    add("implementation", libs.findLibrary("glide").get())
                }
            }
        }
    }
}