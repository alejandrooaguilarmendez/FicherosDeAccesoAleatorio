plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Dependencia de JitPack correctamente escrita para Kotlin DSL
    implementation("com.github.JRJimenezReyes:entrada:1.0.3")
}

tasks.test {
    useJUnitPlatform()
}