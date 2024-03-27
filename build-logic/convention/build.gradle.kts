plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("AndroidHilt") {
            id = "android.hilt"
            implementationClass = "com.yjy.convention.AndroidHiltConventionPlugin"
        }
        register("AndroidApplication") {
            id = "android.application"
            implementationClass = "com.yjy.convention.AndroidApplicationConventionPlugin"
        }
        register("AndroidLibrary") {
            id = "android.library"
            implementationClass = "com.yjy.convention.AndroidLibraryConventionPlugin"
        }
        register("AndroidData") {
            id = "android.data"
            implementationClass = "com.yjy.convention.AndroidDataConventionPlugin"
        }
        register("AndroidDomain") {
            id = "android.domain"
            implementationClass = "com.yjy.convention.AndroidDomainConventionPlugin"
        }
    }
}