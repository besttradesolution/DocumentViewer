plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
// Optional short build path for Windows installations with long-path tooling limits.
System.getenv("DOCUMENT_VIEWER_BUILD_DIR")?.let { layout.buildDirectory.set(file(it)) }
android {
    namespace = "com.local.documentviewer"
    compileSdk = 34
    defaultConfig { applicationId = "com.local.documentviewer"; minSdk = 28; targetSdk = 34; versionCode = 3; versionName = "0.2.1" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    testImplementation("junit:junit:4.13.2")
}
