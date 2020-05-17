import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {
    kotlin("jvm") version "1.3.72"
    id("org.asciidoctor.jvm.convert") version "3.0.0"
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
    implementation("io.vlingo:vlingo-actors:1.2.20")
    implementation("io.vlingo:vlingo-lattice:1.2.20")
    implementation("io.vlingo:vlingo-symbio:1.2.20")
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