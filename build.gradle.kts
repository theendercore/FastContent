import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.3.8"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.teamvoided.iridium") version "3.1.9"
}

group = project.properties["maven_group"]!!
version = project.properties["mod_version"]!!
base.archivesName.set(project.properties["archives_base_name"] as String)
description = "eUtil"
val modid = project.properties["modid"]!! as String

repositories {
    mavenCentral()
}

modSettings {
    modId(modid)
    modName("eUtil")

    entrypoint("main", "org.teamvoided.eutil.EUtilMod::commonInit")
    entrypoint("client", "org.teamvoided.eutil.EUtilMod::clientInit")
    accessWidener("eutil.accesswidener")
}

loom {
    accessWidenerPath.set(file("src/main/resources/eutil.accesswidener"))
}

tasks {
    val targetJavaVersion = 17
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = targetJavaVersion.toString()
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(JavaVersion.toVersion(targetJavaVersion).toString()))
        withSourcesJar()
    }
}