plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.luxpro.max"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.luxpro.max"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            ndkBuild {
                arguments += "NDK_APPLICATION_MK=src/main/cpp/Application.mk"
                abiFilters.clear()
                abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
            }
        }
        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    signingConfigs {
        create("release") {
            // تم إنشاء ملف المفتاح افتراضياً في المسار الصحيح
            val keystoreFile = rootProject.file("lux_pro_key.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "luxpro2027"
                keyAlias = "lux_pro_key_alias" // تم تغيير الاسم ليتوافق مع متطلبات التوقيع
                keyPassword = "11223344Aass@@"
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                strippedNativeLibsDir = "build/intermediates/stripped_native_libs/release/out/lib"
                unstrippedNativeLibsDir = "build/intermediates/merged_native_libs/release/out/lib"
            }
        }
        debug {
            // استخدام توقيع الـ release للنسخة التجريبية أيضاً لسهولة الاختبار
            signingConfig = signingConfigs.getByName("release")
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
                strippedNativeLibsDir = "build/intermediates/stripped_native_libs/debug/out/lib"
                unstrippedNativeLibsDir = "build/intermediates/merged_native_libs/debug/out/lib"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        prefab = true
    }
    
    packaging {
        // منع تضارب المكتبات في النسخة النهائية
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.airbnb.android:lottie:6.4.0") // تحديث
    implementation("androidx.appcompat:appcompat:1.7.0") // تحديث
    implementation("com.google.android.material:material:1.12.0") // تحديث
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.browser:browser:1.8.0")

    // مكتبة فحص الأخطاء
    implementation("com.google.firebase:firebase-crashlytics:19.0.3")
    implementation("com.google.firebase:firebase-crashlytics-ndk:19.0.3")
    implementation("com.google.firebase:firebase-analytics:22.0.2")

    // ShadowHook for robust EGL interception
    implementation("com.bytedance.android:shadowhook:1.0.9")

    // Gson for AccountVaultManager JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Chucker Network Debugger & OkHttp
    implementation("com.github.chuckerteam.chucker:library:4.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}