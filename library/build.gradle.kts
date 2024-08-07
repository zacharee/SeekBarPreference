plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)

    id("maven-publish")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        lint.targetSdk = 34
        namespace = "tk.zwander.seekbarpreference"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    val jdkVersion = "17"

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(jdkVersion)
        targetCompatibility = JavaVersion.toVersion(jdkVersion)
    }

    kotlinOptions {
        jvmTarget = jdkVersion
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.material)
    implementation(libs.preference.ktx)
    implementation(libs.constraintlayout)
}

afterEvaluate {
    publishing {
        publications {
            create("release", MavenPublication::class.java) {
                from(components.getByName("release"))

                groupId = "dev.zwander.seekbarpreference"
                artifactId = "final"
                version = "1.0"
            }
        }
    }
}
