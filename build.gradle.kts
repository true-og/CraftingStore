import org.gradle.kotlin.dsl.provideDelegate

/* ------------------------------ Plugins ------------------------------ */
plugins {
    id("java") // Import Java plugin.
    id("java-library") // Import Java Library plugin.
    id("com.diffplug.spotless") version "7.0.4" // Import Spotless plugin.
    id("com.gradleup.shadow") version "8.3.6" // Import Shadow plugin.
    id("checkstyle") // Import Checkstyle plugin.
    eclipse // Import Eclipse plugin.
    kotlin("jvm") version "2.1.21" // Import Kotlin JVM plugin.
}

extra["kotlinAttribute"] = Attribute.of("kotlin-tag", Boolean::class.javaObjectType)

val kotlinAttribute: Attribute<Boolean> by rootProject.extra

/* --------------------------- JDK / Kotlin ---------------------------- */
java {
    sourceCompatibility = JavaVersion.VERSION_17 // Compile with JDK 17 compatibility.
    toolchain { // Select Java toolchain.
        languageVersion.set(JavaLanguageVersion.of(17)) // Use JDK 17.
        vendor.set(JvmVendorSpec.GRAAL_VM) // Use GraalVM CE.
    }
}

kotlin { jvmToolchain(17) }

/* ----------------------------- Metadata ------------------------------ */
group = "net.craftingstore" // Declare bundle identifier.

version = "2.10.0-RELEASE" // Declare plugin version (will be in .jar).

val apiVersion = "1.19" // Declare minecraft server target version.

extra["apiVersion"] = apiVersion

/* ----------------------------- Resources ----------------------------- */
tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props) // Indicates to rerun if version changes.
    filesMatching("plugin.yml") { expand(props) }
    from("LICENSE") { into("/") } // Bundle licenses into jarfiles.
}

/* ---------------------------- Repos ---------------------------------- */
allprojects {
    repositories {
        mavenCentral() // Import the Maven Central Maven Repository.
        gradlePluginPortal() // Import the Gradle Plugin Portal Maven Repository.
        maven { url = uri("https://repo.purpurmc.org/snapshots") } // Import the PurpurMC Maven Repository.
        maven { url = uri("file://${System.getProperty("user.home")}/.m2/repository") }
        maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
        maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
        maven { url = uri("https://rayzr.dev/repo/") }
        maven { url = uri("https://repo.codemc.org/repository/maven-public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.spongepowered.org/maven") }
        maven { url = uri("https://repo.velocitypowered.com/snapshots/") }
        System.getProperty("SELF_MAVEN_LOCAL_REPO")?.let {
            val dir = file(it)
            if (dir.isDirectory) {
                println("Using SELF_MAVEN_LOCAL_REPO at: $it")
                maven { url = uri("file://${dir.absolutePath}") }
            } else {
                mavenLocal()
            }
        }
    }
}

/* ---------------------- Java project deps ---------------------------- */
dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT") // Declare Purpur API version to be packaged.
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3") // Import MiniPlaceholders API.
}

/* ---------------------- Reproducible jars ---------------------------- */
tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible .jars
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

/* ----------------------------- Shadow -------------------------------- */
tasks.shadowJar {
    exclude("io.github.miniplaceholders.*") // Exclude the MiniPlaceholders package from being shadowed.
    archiveClassifier.set("") // Use empty string instead of null.
    minimize()
}

tasks.jar { archiveClassifier.set("part") } // Applies to root jarfile only.

tasks.build { dependsOn(tasks.spotlessApply, tasks.shadowJar) } // Build depends on spotless and shadow.

/* --------------------------- Javac opts ------------------------------- */
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters") // Enable reflection for java code.
    options.isFork = true // Run javac in its own process.
    options.compilerArgs.add("-Xlint:deprecation") // Trigger deprecation warning messages.
    options.encoding = "UTF-8" // Use UTF-8 file encoding.
}

/* ----------------------------- Auto Formatting ------------------------ */
spotless {
    java {
        eclipse().configFile("config/formatter/eclipse-java-formatter.xml") // Eclipse java formatting.
        leadingTabsToSpaces() // Convert leftover leading tabs to spaces.
        removeUnusedImports() // Remove imports that aren't being called.
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) } // JetBrains Kotlin formatting.
        target("build.gradle.kts", "settings.gradle.kts") // Gradle files to format.
    }
}

checkstyle {
    toolVersion = "10.18.1" // Declare checkstyle version to use.
    configFile = file("config/checkstyle/checkstyle.xml") // Point checkstyle to config file.
    isIgnoreFailures = true // Don't fail the build if checkstyle does not pass.
    isShowViolations = true // Show the violations in any IDE with the checkstyle plugin.
}

tasks.named("compileJava") {
    dependsOn("spotlessApply") // Run spotless before compiling with the JDK.
}

tasks.named("spotlessCheck") {
    dependsOn("spotlessApply") // Run spotless before checking if spotless ran.
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "eclipse")
    eclipse.project.name = "${project.name}-${rootProject.name}"
    tasks.withType<Jar>().configureEach { archiveBaseName.set("${project.name}-${rootProject.name}") }
}

project(":core") {
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
    dependencies {
        compileOnly("com.google.code.gson:gson:2.8.9")
        implementation("com.github.thijsa:socket.io-client-java:1.0.3")
        implementation("org.apache.httpcomponents:httpclient:4.5.13")
        implementation("commons-lang:commons-lang:2.6")
        implementation("org.apache.commons:commons-lang3:3.18.0")
        implementation("commons-logging:commons-logging:1.2")
        implementation("org.apache.commons:commons-text:1.10.0")
    }
}

project(":bukkit") {
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
    dependencies {
        implementation(project(":core"))
        compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT")
        compileOnly("net.md-5:bungeecord-chat:1.20-R0.2")
        compileOnly("me.clip:placeholderapi:2.11.6")
        implementation("com.github.cryptomorin:XSeries:9.4.0")
        compileOnly("com.github.MilkBowl:VaultAPI:1.7")
        implementation("org.apache.commons:commons-text:1.10.0")
    }
    tasks.named<ProcessResources>("processResources") {
        val props =
            mapOf(
                "version" to rootProject.version.toString(),
                "apiVersion" to (rootProject.extra["apiVersion"]?.toString().orEmpty()),
            )
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
    }
}

project(":sponge") {
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
    dependencies {
        implementation(project(":core"))
        compileOnly("org.spongepowered:spongeapi:7.1.0")
    }
    tasks.named<ProcessResources>("processResources") {
        val props = mapOf("version" to rootProject.version.toString())
        inputs.properties(props)
        from("src/main/java") { expand(props) }
    }
}

project(":velocity") {
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
    dependencies {
        implementation(project(":core"))
        compileOnly("com.velocitypowered:velocity-api:3.0.0")
        implementation("com.googlecode.json-simple:json-simple:1.1.1") {
            exclude(group = "junit", module = "junit")
            exclude(group = "org.hamcrest", module = "hamcrest-core")
        }
    }
    tasks.named<ProcessResources>("processResources") {
        val props = mapOf("version" to rootProject.version.toString())
        inputs.properties(props)
        filesMatching("**/*") { expand(props) }
    }
}

project(":assembly") {
    apply(plugin = "com.gradleup.shadow")
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
    dependencies {
        implementation(project(":core"))
        implementation(project(":bukkit"))
        implementation(project(":sponge"))
        implementation(project(":velocity"))
    }
    tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveFileName.set("CraftingStore-${rootProject.version}.jar")
        relocate("org.apache.http", "net.craftingstore.libraries.apache.http")
        relocate("org.apache.commons.lang", "me.dadus33.libs.commonslang2")
        relocate("org.apache.commons.lang3", "net.craftingstore.libraries.apache.commons.lang3")
        relocate("org.apache.commons.logging", "net.craftingstore.libraries.apache.commons.logging")
        relocate("org.apache.commons.codec", "net.craftingstore.libraries.apache.commons.codec")
        relocate("org.apache.commons.text", "net.craftingstore.libraries.apache.commons.text")
        relocate("org.json", "net.craftingstore.libraries.json")
        relocate("io.socket", "net.craftingstore.libraries.socket")
        relocate("okhttp3", "net.craftingstore.libraries.okhttp3")
        relocate("okio", "net.craftingstore.libraries.okio")
        relocate("com.cryptomorin.xseries", "net.craftingstore.libraries.xseries")
        exclude("com/cryptomorin/xseries/messages/**")
        exclude("com/cryptomorin/xseries/particles/**")
        exclude("com/cryptomorin/xseries/XBiome*")
        exclude("com/cryptomorin/xseries/NMSExtras*")
        exclude("com/cryptomorin/xseries/NoteBlockMusic*")
        exclude("com/cryptomorin/xseries/XSound*")
        exclude("com/cryptomorin/xseries/XPotion*")
        exclude("com/cryptomorin/xseries/XEnchantment*")
        exclude("com/cryptomorin/xseries/XEntity*")
    }
    tasks.named<Jar>("jar") { enabled = false }
    tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
}
