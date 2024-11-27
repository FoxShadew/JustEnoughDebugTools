plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("maven-publish")
}

val archives_base_name: String by project
val mod_version: String by project
val maven_group: String by project

val minecraft_version: String by project
val loader_version: String by project
val fabric_version: String by project



version = mod_version
group = maven_group

base {
    archivesName = archives_base_name
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    // AWA
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings(loom.officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:${loader_version}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabric_version}")
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    from("LICENSE")
}

if (project.hasProperty("rfxMavenUser") && project.hasProperty("rfxMavenPass")) {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = "jedt"
                from(components["java"])
            }
        }

        repositories {
            maven {
                name = "RunefoxMaven"
                url = uri("https://maven.shadew.net/")
                credentials {
                    username = project.property("rfxMavenUser") as String
                    password = project.property("rfxMavenPass") as String
                }
            }
        }
    }
}
