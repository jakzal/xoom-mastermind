plugins {
    kotlin("jvm") version "1.3.61"
}

group = "pl.zalas"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11.0"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11.0"
    }
}