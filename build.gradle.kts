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
    implementation(kotlin("reflect"))
    implementation("io.vlingo:vlingo-lattice:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

tasks.test {
    useJUnitPlatform()
}
