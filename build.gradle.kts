import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "pl.zalas"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("io.vlingo.xoom:xoom-actors:1.9.1")
    implementation("io.vlingo.xoom:xoom-lattice:1.9.1")
    implementation("io.vlingo.xoom:xoom-symbio:1.9.1")
    implementation("io.vlingo.xoom:xoom-symbio-jdbc:1.9.1")
    implementation("io.vlingo.xoom:xoom-http:1.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("io.rest-assured:rest-assured:4.3.2")
    testImplementation("io.rest-assured:kotlin-extensions:4.3.2")
    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
    testImplementation("org.testcontainers:postgresql:1.16.3")
}

application {
    mainClass.set("pl.zalas.mastermind.infrastructure.http.Application")
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
