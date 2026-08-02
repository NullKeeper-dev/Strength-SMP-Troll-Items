plugins {
    java
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
val bukkitApiVersion = providers.gradleProperty("bukkitApiVersion")
    .orElse("26.1-R0.1-SNAPSHOT")

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:${bukkitApiVersion.get()}")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    testImplementation("net.dmulloy2:ProtocolLib:5.4.0")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.114.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("projectVersion", project.version.toString())
}

tasks.jar {
    archiveBaseName.set("strength-smp-troll-items")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}
