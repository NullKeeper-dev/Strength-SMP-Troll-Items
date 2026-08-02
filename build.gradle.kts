import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
}

data class ServerTarget(
    val id: String,
    val apiVersion: String,
    val coordinate: String,
)

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
val releaseVersion = project.version.toString()

val serverTargets = listOf(
    ServerTarget("paper-26.1.1", "26.1", "io.papermc.paper:paper-api:26.1.1.build.29-alpha"),
    ServerTarget("paper-26.1.2", "26.1", "io.papermc.paper:paper-api:26.1.2.build.74-stable"),
    ServerTarget("paper-26.2", "26.2", "io.papermc.paper:paper-api:26.2.build.87-stable"),
    ServerTarget("purpur-26.1.2", "26.1", "org.purpurmc.purpur:purpur-api:26.1.2.build.2592-stable"),
    ServerTarget("purpur-26.2", "26.2", "org.purpurmc.purpur:purpur-api:26.2.build.2618-stable"),
)

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.purpurmc.org/snapshots")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.1.build.29-alpha")

    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.74-stable")
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
    inputs.property("version", releaseVersion)
    inputs.property("apiVersion", "26.1")
    filesMatching("plugin.yml") {
        expand("version" to releaseVersion, "apiVersion" to "26.1")
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("projectVersion", project.version.toString())
}

tasks.jar {
    enabled = false
    archiveBaseName.set("strength-smp-troll-items")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

val targetJarTasks = serverTargets.map { target ->
    val suffix = target.id.split('-', '.').joinToString("") { segment ->
        segment.replaceFirstChar { character -> character.uppercase() }
    }
    val api = configurations.create("${suffix.replaceFirstChar { it.lowercase() }}Api") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(
                Category.CATEGORY_ATTRIBUTE,
                objects.named(Category::class.java, Category.LIBRARY),
            )
            attribute(
                Bundling.BUNDLING_ATTRIBUTE,
                objects.named(Bundling::class.java, Bundling.EXTERNAL),
            )
            attribute(
                TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
            )
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
            attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements::class.java, LibraryElements.CLASSES),
            )
            attribute(
                Usage.USAGE_ATTRIBUTE,
                objects.named(Usage::class.java, Usage.JAVA_API),
            )
        }
    }
    dependencies.add(api.name, target.coordinate)

    val compile = tasks.register<JavaCompile>("compile${suffix}Java") {
        source(sourceSets.main.get().java)
        classpath = api
        destinationDirectory.set(layout.buildDirectory.dir("classes/targets/${target.id}"))
        javaCompiler.set(javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    val resources = tasks.register<ProcessResources>("process${suffix}Resources") {
        from(sourceSets.main.get().resources)
        destinationDir = layout.buildDirectory.dir("resources/targets/${target.id}").get().asFile
        inputs.property("version", releaseVersion)
        inputs.property("apiVersion", target.apiVersion)
        filesMatching("plugin.yml") {
            expand(
                "version" to releaseVersion,
                "apiVersion" to target.apiVersion,
            )
        }
    }
    tasks.register<Jar>("jar${suffix}") {
        dependsOn(compile, resources)
        archiveBaseName.set("strength-smp-troll-items")
        archiveClassifier.set(target.id)
        from(compile.flatMap { it.destinationDirectory })
        from(resources)
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            )
        }
    }
}

tasks.assemble {
    dependsOn(targetJarTasks)
}

val expectedDistributables = serverTargets
    .map { target -> "strength-smp-troll-items-$releaseVersion-${target.id}.jar" }
    .toSet()

tasks.register("verifyDistributables") {
    dependsOn(targetJarTasks)
    doLast {
        val actual = layout.buildDirectory.dir("libs").get().asFile
            .listFiles { file -> file.extension == "jar" }
            .orEmpty()
            .map { file -> file.name }
            .toSet()
        check(actual == expectedDistributables) {
            "Expected distributables $expectedDistributables but found $actual"
        }
        check(actual.none { name -> "sources" in name }) {
            "Source jars are not distributable targets: $actual"
        }
    }
}
