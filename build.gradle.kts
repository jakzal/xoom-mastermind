import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm")
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "pl.zalas"
version = project.version
java.sourceCompatibility = JavaVersion.VERSION_11

val xoomVersion = "1.9.1"
val junitVersion = "5.8.2"
val restAssuredVersion = "4.5.0"
val testcontainersVersion = "1.16.3"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("io.vlingo.xoom:xoom-actors:$xoomVersion")
    implementation("io.vlingo.xoom:xoom-lattice:$xoomVersion")
    implementation("io.vlingo.xoom:xoom-symbio:$xoomVersion")
    implementation("io.vlingo.xoom:xoom-symbio-jdbc:$xoomVersion")
    implementation("io.vlingo.xoom:xoom-http:$xoomVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testImplementation("io.rest-assured:kotlin-extensions:$restAssuredVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
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
