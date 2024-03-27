plugins {
    id("android.application")
    alias(libs.plugins.kotlin.android)
}
dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintLayout)
}
