plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    api(libs.coroutines.core)
    // Android trae org.json en el sistema; en la JVM pura hace falta la librería.
    implementation(libs.org.json)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
