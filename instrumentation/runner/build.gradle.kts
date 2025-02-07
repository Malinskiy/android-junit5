import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    google()
    mavenCentral()
    sonatypeSnapshots()
  }

  dependencies {
    val latest = Artifacts.Plugin.latestStableVersion
    classpath("de.mannodermaus.gradle.plugins:android-junit5:$latest")
  }
}

plugins {
  id("com.android.library")
  kotlin("android")
  id("explicit-api-mode")
}

apply {
  plugin("de.mannodermaus.android-junit5")
}

val javaVersion = JavaVersion.VERSION_1_8

android {
  compileSdk = Android.compileSdkVersion

  defaultConfig {
    minSdk = Android.testRunnerMinSdkVersion
    targetSdk = Android.targetSdkVersion
  }

  compileOptions {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  buildFeatures {
    buildConfig = false
    resValues = false
  }

  lint {
    // JUnit 4 refers to java.lang.management APIs, which are absent on Android.
    warning("InvalidPackage")
  }

  packagingOptions {
    resources.excludes.add("META-INF/LICENSE.md")
    resources.excludes.add("META-INF/LICENSE-notice.md")
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = javaVersion.toString()
}

tasks.withType<Test> {
  failFast = true
  testLogging {
    events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    exceptionFormat = TestExceptionFormat.FULL
  }
}

configurations.all {
  // The Instrumentation Test Runner uses the plugin,
  // which in turn provides the Instrumentation Test Runner again -
  // that's kind of deep.
  // To avoid conflicts, prefer using the local classes
  // and exclude the dependency from being pulled in externally.
  exclude(module = Artifacts.Instrumentation.Runner.artifactId)
}

dependencies {
  implementation(libs.androidXTestMonitor)
  implementation(libs.kotlinStdLib)
  implementation(libs.junit4)

  // This module's JUnit 5 dependencies cannot be present on the runtime classpath,
  // since that would prematurely raise the minSdkVersion requirement for target applications,
  // even though not all product flavors might want to use JUnit 5.
  // Therefore, only compile against those APIs, and have them provided at runtime
  // by the "instrumentation" companion library instead.
  compileOnly(libs.junitJupiterApi)
  compileOnly(libs.junitJupiterParams)
  compileOnly(libs.junitPlatformRunner)

  testImplementation(project(":testutil"))
  testImplementation(libs.robolectric)
  testRuntimeOnly(libs.junitVintageEngine)
  testRuntimeOnly(libs.junitJupiterEngine)
}

project.configureDeployment(Artifacts.Instrumentation.Runner)
