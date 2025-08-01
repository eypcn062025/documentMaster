plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.documentmaster.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.documentmaster.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // MultiDex desteği (Apache POI için gerekli)
    defaultConfig {
        multiDexEnabled = true
    }
}

dependencies {
    // Android Core
    implementation(libs.appcompat)
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // RecyclerView ve UI
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Apache POI - DOCX desteği için
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")
    implementation("org.apache.poi:poi-ooxml-lite:5.2.4")
    implementation("org.apache.poi:poi-scratchpad:5.2.4")

    // MultiDex desteği
    implementation("androidx.multidex:multidex:2.0.1")

    // Dosya işlemleri
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.opencsv:opencsv:5.8")


    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Daha gelişmiş rich text editör için (opsiyonel)
    implementation ("jp.wasabeef:richeditor-android:2.0.0")
    implementation ("com.itextpdf:itext7-core:7.2.5")
    implementation ("com.github.skydoves:colorpickerview:2.2.4")
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation ("androidx.documentfile:documentfile:1.0.1")
    implementation ("androidx.core:core-ktx:1.12.0")
}