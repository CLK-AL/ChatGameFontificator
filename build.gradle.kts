plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

group = "com.glitchcog"
version = "2.0.0"

// Mirror the Maven POM layout: Java sources under src/main/java, resources
// under src/main/resources, Kotlin tests under src/test/kotlin.
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
    test {
        java {
            setSrcDirs(emptyList<String>())
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Legacy sources use raw types and deprecated APIs; keep the build quiet to
    // mirror the Maven build's behaviour.
    options.compilerArgs.addAll(listOf("-Xlint:none", "-nowarn"))
    options.isWarnings = false
}

// Match Kotlin's target to the Java source/target compatibility.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    // Runtime dependencies ported 1:1 from pom.xml.
    implementation(libs.log4j)
    implementation(libs.pircbot)
    implementation(libs.gson)
    implementation(libs.jasypt)

    // Test dependencies.
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
