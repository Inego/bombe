import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
    id("org.jetbrains.compose") version "0.3.2"
    id("com.google.protobuf") version "0.8.15"
    idea
}


group = "org.inego"
version = "1.0.0"


repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}


val protoVer = "3.+"
val kotestVer = "4.+"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.google.protobuf", "protobuf-java", protoVer)
    implementation("com.google.protobuf", "protobuf-java-util", protoVer)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVer")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVer")
}




tasks.withType<KotlinCompile> {

    kotlinOptions {
        jvmTarget = "11"
        useIR = true
        freeCompilerArgs = listOf("-Xopt-in=kotlin.io.path.ExperimentalPathApi")
    }
}


compose.desktop {
    application {
        mainClass = "org.inego.bombe.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "bombe"
            modules("java.sql")
        }
    }
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protoVer"
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}
