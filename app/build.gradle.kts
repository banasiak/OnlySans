import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.kover)
  alias(libs.plugins.ksp)
}

// Get a Google Fonts API key (https://developers.google.com/fonts/docs/developer_api#APIKey) and
// put it in local.properties as:
//   fontsApiKey=<your personal API key>
// Surrounding quotes are optional. A missing key is deliberately *not* a build failure -- the app
// builds and reports the missing key on screen, so a fresh clone compiles before it is configured.
val fontsApiKey: String =
  Properties()
    .apply {
      val file = rootProject.file("local.properties")
      if (file.exists()) file.inputStream().use { load(it) }
    }.getProperty("fontsApiKey")
    .orEmpty()
    .trim()
    .removeSurrounding("\"")

android {
  namespace = "app.onlysans.android"
  compileSdk = 37
  compileSdkMinor = 2

  defaultConfig {
    applicationId = "app.onlysans.android"
    minSdk = 26
    targetSdk = 37
    versionCode = 2
    versionName = "2.0"
  }
  buildFeatures {
    buildConfig = true
    compose = true
  }
  buildTypes {
    debug {
      buildConfigField("String", "FONTS_API_KEY", "\"$fontsApiKey\"")
    }
    release {
      buildConfigField("String", "FONTS_API_KEY", "\"$fontsApiKey\"")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  testOptions {
    unitTests.all {
      it.useJUnitPlatform()
    }
  }
}

composeCompiler {
  // `./gradlew :app:assembleRelease -PcomposeMetrics` writes the stability and skippability report
  // that the file below was derived from; app-composables.txt is the one worth reading.
  if (providers.gradleProperty("composeMetrics").isPresent) {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
  }
  stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
  }
}

kover {
  reports {
    filters {
      excludes {
        // Hilt/Dagger and the Compose compiler emit these; nobody wrote them and nobody can test them
        annotatedBy("dagger.internal.DaggerGenerated")
        classes(
          "*.BuildConfig",
          "*.Hilt_*",
          "*_Factory*",
          "*_MembersInjector",
          "*_HiltModules*",
          "*ComposableSingletons*"
        )
        packages("hilt_aggregated_deps", "dagger.hilt.internal")

        // the UI layer is out of scope while there are no Compose tests
        annotatedBy("androidx.compose.runtime.Composable")
        packages("app.onlysans.android.ui.theme")
      }
    }
  }
}

val ktlint: Configuration = configurations.create("ktlint")

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.collection)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.google.material)
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.serialization)
  implementation(libs.timber)
  debugImplementation(libs.androidx.compose.ui.tooling)
  ksp(libs.hilt.android.compiler)
  ksp(libs.kotlin.metadata.jvm)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.kluent.android)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockk.agent)
  testImplementation(libs.mockk.android)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.turbine)
  testRuntimeOnly(libs.junit.platform.launcher)

  ktlint(libs.ktlint) {
    attributes {
      attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
  }
}

val ktlintCheck =
  tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
      "**/src/**/*.kt",
      "**.kts",
      "!**/build/**"
    )
  }

tasks.check {
  dependsOn(ktlintCheck)
}

tasks.register<JavaExec>("format") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Check Kotlin code style and format"
  classpath = ktlint
  mainClass.set("com.pinterest.ktlint.Main")
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
  args(
    "-F",
    "**/src/**/*.kt",
    "**.kts",
    "!**/build/**"
  )
}