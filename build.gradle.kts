import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("org.asciidoctor.jvm.convert") version "3.0.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "pl.zalas"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://oss.jfrog.org/oss-snapshot-local") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("io.vlingo:vlingo-actors:1.3.0")
    implementation("io.vlingo:vlingo-lattice:1.3.0")
    implementation("io.vlingo:vlingo-symbio:1.3.0")
    implementation("io.vlingo:vlingo-symbio-jdbc:1.3.0")
    implementation("io.vlingo:vlingo-http:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("io.rest-assured:rest-assured:4.3.2")
    testImplementation("io.rest-assured:kotlin-extensions:4.3.2")
    testImplementation("org.testcontainers:testcontainers:1.15.0")
    testImplementation("org.testcontainers:junit-jupiter:1.15.0")
    testImplementation("org.testcontainers:postgresql:1.15.0")
}

application {
    mainClassName = "pl.zalas.mastermind.infrastructure.http.Application"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    shadowJar {
        archiveClassifier.set("")
    }
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
        }
    }
}

tasks {
    "asciidoctor"(AsciidoctorTask::class) {
        setBaseDir("docs")
        setSourceDir(file("docs"))
        setOutputDir(file("build/docs"))
        sources(delegateClosureOf<PatternSet> {
            include("index.adoc")
        })
        outputs.upToDateWhen { false }
        attributes(mapOf(
            "source-highlighter" to "coderay",
            "snippets" to file("build/generated-snippets")
        ))
    }
}
