plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
}

android {
    namespace = "com.solovinyykray.solovinyykray"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.solovinyykray.solovinyykray"
        minSdk = 24
        targetSdk = 34
        versionCode = 10
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("my-release-key.jks")
            storePassword = "Uu123456789"
            keyAlias = "Uu123456789"
            keyPassword = "Uu123456789"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release") // ВАЖНО: привязка подписки
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    packaging {
        resources.excludes.add("META-INF/*.version")
    }
}

dependencies {
    implementation("org.jsoup:jsoup:1.16.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation ("com.github.jd-alexander:library:1.1.0")
    implementation ("com.google.maps.android:android-maps-utils:2.3.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.github.ismaeldivita:chip-navigation-bar:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.github.dangiashish:Google-Direction-Api:1.6")
    implementation("com.google.android.libraries.mapsplatform.secrets-gradle-plugin:secrets-gradle-plugin:2.0.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.google.maps.android:android-maps-utils:2.4.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1") // оставляем один gson
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation ("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)

    // Тесты
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}