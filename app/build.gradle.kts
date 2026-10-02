plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace="com.icyvpn"
    compileSdk=36
    defaultConfig { applicationId="com.icyvpn"; minSdk=26; targetSdk=36; versionCode=2; versionName="1.1.0" }
    buildFeatures { compose=true }
    packaging { jniLibs.useLegacyPackaging=true }
    sourceSets["main"].jniLibs.srcDir("src/main/jniLibs")
}
val prepareLibXray by tasks.registering(Exec::class) {
    workingDir(rootDir); commandLine("bash","scripts/fetch-libxray.sh")
}
val buildRust by tasks.registering(Exec::class) {
    workingDir(rootDir); commandLine("bash","scripts/build-rust.sh")
}
tasks.named("preBuild").configure { dependsOn(prepareLibXray,buildRust) }
dependencies {
    implementation(files("libs/libXray.aar"))
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}