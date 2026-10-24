import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.jesosymamonjy.jmmadx"
    minSdk = 23
    targetSdk = 36
    versionCode = 6
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    viewBinding = true
  }
  androidResources {
    ignoreAssetsPatterns += listOf("*.jpg", "*.jpeg", "*.png", "*.webp", "tahirintsary*")
  }
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

val syncUploadedAssets by tasks.registering(Copy::class) {
  from(rootProject.projectDir) {
    include(
      "afaka_miaina_tsy_manota_ve_ny_kristiana.txt",
      "fiarandalamby_makany_andanitra.txt",
      "finoana_marina_nohandraisana_fanasitranana.txt",
      "lalana_roa.txt",
      "mety_ho_anao_ny_famonjena.txt",
      "mety_ho_sitrana_ianao(m.a_daoud).txt",
      "miverena_any_betela.txt",
      "ny_fanasitrananan_andriamanitra_anao.txt",
      "ny_hery_mahadion_ny_ran_i_jesosy.txt",
      "paska_iray_tsy_hay_hadinoina_tany_martinika.txt",
      "soa_ihany.txt",
      "tafasokatra_ve_ny_vavahadin_ny_helo.txt",
      "tena_misy_ny_lanitra.txt"
    )
  }
  into(layout.projectDirectory.dir("src/main/assets"))
}

val syncUploadedDrawables by tasks.registering(Copy::class) {
  from(rootProject.projectDir) {
    include(
      "logo.png",
      "logo_transparent.png",
      "logo_fond_blanc.jpg",
      "toriteny_sy_boky.webp",
      "afaka_miaina_tsy_manota_ve_ny_kristiana.jpg",
      "fiarandalamby_makany_andanitra.jpg",
      "finoana_marina_nohandraisana_fanasitranana.jpeg",
      "lalana_roa.jpg",
      "mety_ho_anao_ny_famonjena.jpeg",
      "mety_ho_sitrana_ianao_ma_daoud.jpeg",
      "miverena_any_betela.jpg",
      "ny_fanasitrananan_andriamanitra_anao.jpg",
      "ny_hery_mahadion_ny_ran_i_jesosy.jpg",
      "paska_iray_tsy_hay_hadinoina_tany_martinika.jpeg",
      "soa_ihany.jpg",
      "tafasokatra_ve_ny_vavahadin_ny_helo.webp",
      "tena_misy_ny_lanitra.webp"
    )
  }
  into(layout.projectDirectory.dir("src/main/res/drawable-nodpi"))
}

tasks.named("preBuild") {
  dependsOn(syncUploadedAssets, syncUploadedDrawables)
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.google.material)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.coordinatorlayout)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  // implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  // debugImplementation(libs.androidx.compose.ui.test.manifest)
  // debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
