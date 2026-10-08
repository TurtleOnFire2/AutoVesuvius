import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
    `maven-publish`
}

group = property("maven_group")!!
version = property("mod_version")!!

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")

    // Built from the pinned source submodule and kept out of the addon jar.
    compileOnly("com.odtheking:Odin:${property("odin_version")}")
    runtimeOnly("com.odtheking:Odin:${property("odin_version")}")

    implementation(
        "com.github.stivais:Commodore:${property("commodore_version")}"
    )
}

loom {
    runConfigs.named("client") {
        generateRunConfig.set(true)
        jvmArguments.addAll(
            "-Dmixin.debug.export=true",
            "-Ddevauth.enabled=true",
            "-Ddevauth.account=main",
            "-XX:+AllowEnhancedClassRedefinition",
            "-XX:+IgnoreUnrecognizedVMOptions",
        )
    }

    runConfigs.named("server") {
        generateRunConfig.set(false)
    }

    accessWidenerPath = file("src/main/resources/odinaddon.accesswidener")
}

afterEvaluate {
    loom.runs.named("client") {
        jvmArguments.add("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
    }
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(getProperties())
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            freeCompilerArgs.add("-Xlambdas=class") //Commodore
        }
    }

    compileJava {
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)

    withSourcesJar()
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}
