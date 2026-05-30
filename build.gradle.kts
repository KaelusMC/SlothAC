import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission
import org.gradle.api.file.DuplicatesStrategy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import tasks.PrintFilePathTask
import versioning.BuildConfig

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.20"
  id("com.gradleup.shadow") version "9.4.0"
  id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
  id("com.diffplug.spotless") version "8.4.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

BuildConfig.init(project)

group = "space.kaelus.sloth"

version = "1.0"

val packetEventsSpigot = "com.github.KaelusMC.packetevents:packetevents-spigot:v2026.04.26-f4aaadc"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://repo.codemc.io/repository/maven-releases/")
  maven("https://repo.codemc.io/repository/maven-snapshots/")
  maven("https://maven.enginehub.org/repo/") // WorldGuard
  maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
}

dependencies {
  // Bukkit APIs
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.15")
  compileOnly("me.clip:placeholderapi:2.12.2")

  // PacketEvents
  if (BuildConfig.shadePE) {
    implementation(packetEventsSpigot)
  } else {
    compileOnly(packetEventsSpigot)
  }
  implementation("org.bstats:bstats-bukkit:3.2.1")

  // Cloud Command Framework
  implementation("org.incendo:cloud-paper:2.0.0-beta.14")
  implementation("org.incendo:cloud-processors-requirements:1.0.0-rc.1")
  implementation("org.incendo:cloud-kotlin-extensions:2.0.0")
  implementation("org.incendo:cloud-kotlin-coroutines:2.0.0")

  // Adventure & MiniMessage
  implementation("net.kyori:adventure-platform-bukkit:4.4.1")
  implementation("net.kyori:adventure-text-minimessage:4.26.1")
  implementation("net.kyori:adventure-text-serializer-plain:4.26.1")

  // HikariCP
  implementation("com.zaxxer:HikariCP:7.0.2")
  implementation("org.slf4j:slf4j-jdk14:2.0.17")
  implementation("org.jetbrains.exposed:exposed-core:1.1.1")
  implementation("org.jetbrains.exposed:exposed-java-time:1.1.1")
  implementation("org.jetbrains.exposed:exposed-jdbc:1.1.1")
  implementation("org.flywaydb:flyway-core:12.1.1")
  implementation("org.flywaydb:flyway-mysql:12.1.1")
  implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")

  // Utilities
  implementation(kotlin("stdlib"))
  implementation("it.unimi.dsi:fastutil:8.5.15")
  implementation("org.jetbrains:annotations:26.1.0")
  implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
  implementation("org.spongepowered:configurate-yaml:4.2.0")
  implementation("ru.vyarus:yaml-config-updater:1.4.4")
  implementation("io.insert-koin:koin-core:4.2.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")

  // Testing
  testImplementation(kotlin("test"))
  testImplementation(packetEventsSpigot)
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
  testImplementation("io.mockk:mockk:1.14.9")
  testImplementation("org.testcontainers:junit-jupiter:1.21.4")
  testImplementation("org.testcontainers:mariadb:1.21.4")
  testCompileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testRuntimeOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testRuntimeOnly("org.xerial:sqlite-jdbc:3.51.3.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
  disableAutoTargetJvm()
}

kotlin { jvmToolchain(21) }

tasks.withType<JavaCompile> {
  options.release.set(17)
  options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.addAll("-jvm-default=enable")
  }
}

tasks.jar { archiveClassifier.set("thin") }

tasks.shadowJar {
  archiveBaseName.set(rootProject.name)
  archiveClassifier.set("")

  eachFile {
    if (path == "META-INF/services/org.flywaydb.core.extensibility.Plugin") {
      duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
  }

  minimize {
    exclude(dependency("org.slf4j:slf4j-api"))
    exclude(dependency("org.slf4j:slf4j-jdk14"))
    exclude(dependency("org.jetbrains.exposed:exposed-core"))
    exclude(dependency("org.jetbrains.exposed:exposed-jdbc"))
    exclude(dependency("org.jetbrains.exposed:exposed-java-time"))
    exclude(dependency("org.flywaydb:flyway-core"))
    exclude(dependency("org.flywaydb:flyway-mysql"))
    exclude(dependency("org.mariadb.jdbc:mariadb-java-client"))
  }

  mergeServiceFiles()

  if (BuildConfig.shadePE) {
    relocate("com.github.retrooper.packetevents", "space.kaelus.sloth.libs.packetevents.api")
    relocate("io.github.retrooper.packetevents", "space.kaelus.sloth.libs.packetevents.impl")
    relocate("net.kyori", "space.kaelus.sloth.libs.kyori")
  }
  relocate("org.bstats", "space.kaelus.sloth.libs.bstats")
  relocate("org.incendo", "space.kaelus.sloth.libs.incendo")
  relocate("io.leangen.geantyref", "space.kaelus.sloth.libs.geantyref")
  relocate("it.unimi.dsi.fastutil", "space.kaelus.sloth.libs.fastutil")
  relocate("com.google.flatbuffers", "space.kaelus.sloth.libs.flatbuffers")
  relocate("com.fasterxml.jackson", "space.kaelus.sloth.libs.jackson")
  relocate("com.zaxxer", "space.kaelus.sloth.libs.hikari")
  relocate("org.slf4j", "space.kaelus.sloth.libs.slf4j")
  relocate("org.jetbrains.exposed", "space.kaelus.sloth.libs.jetbrains.exposed")
  relocate("org.spongepowered.configurate", "space.kaelus.sloth.libs.configurate")
  relocate("org.yaml.snakeyaml", "space.kaelus.sloth.libs.snakeyaml")
  relocate("ru.vyarus.yaml.updater", "space.kaelus.sloth.libs.yamlupdater")
  relocate("org.joml", "space.kaelus.sloth.libs.joml")
  relocate("org.koin", "space.kaelus.sloth.libs.koin")
  relocate("org.flywaydb", "space.kaelus.sloth.libs.flyway")
  relocate("tools.jackson", "space.kaelus.sloth.libs.tools.jackson")
}

tasks.register<PrintFilePathTask>("printShadowJarPath") {
  description = "Prints the absolute path of the release shadow JAR."
  group = "help"
  file.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.test {
  useJUnitPlatform { excludeTags("container") }
  jvmArgs(
    "-XX:+EnableDynamicAgentLoading",
    "--add-opens",
    "java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens",
    "java.base/java.lang=ALL-UNNAMED",
  )
}

val containerTest by
  tasks.registering(Test::class) {
    description = "Runs container-backed integration tests."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("container") }
    shouldRunAfter(tasks.test)
    jvmArgs(
      "-XX:+EnableDynamicAgentLoading",
      "--add-opens",
      "java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens",
      "java.base/java.lang=ALL-UNNAMED",
    )
  }

tasks.build { dependsOn(tasks.shadowJar) }

detekt {
  toolVersion = "1.23.8"
  buildUponDefaultConfig = true
  allRules = false
  parallel = true
  baseline = file("config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  jvmTarget = "17"
  exclude("**/flatbuffers/**")
  exclude("**/build/**")
}

bukkit {
  name = "SlothAC"
  main = "space.kaelus.sloth.SlothAC"
  version = project.version.toString()
  apiVersion = "1.13"
  authors = listOf("KaelusMC")
  website = "https://dsc.gg/kaelus"
  foliaSupported = true
  if (!BuildConfig.shadePE) {
    depend = listOf("packetevents")
  }
  softDepend =
    listOf(
      "ProtocolLib",
      "ProtocolSupport",
      "Essentials",
      "ViaVersion",
      "ViaBackwards",
      "ViaRewind",
      "Geyser-Spigot",
      "floodgate",
      "FastLogin",
      "PlaceholderAPI",
      "WorldGuard",
    )

  permissions {
    register("sloth.help") {
      description = "Allows usage of the help command"
      default = Permission.Default.OP
    }
    register("sloth.alerts") {
      description = "Receive alerts for violations"
      default = Permission.Default.OP
    }
    register("sloth.alerts.enable-on-join") {
      description = "Automatically enables alerts on join"
      default = Permission.Default.OP
    }
    register("sloth.reload") {
      description = "Allows reloading the config"
      default = Permission.Default.OP
    }
    register("sloth.connect") {
      description = "Allows linking/unlinking this server to the Sloth web panel"
      default = Permission.Default.OP
    }
    register("sloth.exempt") {
      description = "Exempt from all checks"
      default = Permission.Default.FALSE
    }
    register("sloth.disable") {
      description = "Disables anti-cheat tracking for the player"
      default = Permission.Default.FALSE
    }
    register("sloth.datacollect") {
      description = "Parent permission for data collection commands"
      default = Permission.Default.OP
      children =
        listOf("sloth.datacollect.start", "sloth.datacollect.stop", "sloth.datacollect.status")
    }
    register("sloth.datacollect.start") {
      description = "Allows starting a data collection session"
      default = Permission.Default.FALSE
    }
    register("sloth.datacollect.stop") {
      description = "Allows stopping a data collection session"
      default = Permission.Default.FALSE
    }
    register("sloth.datacollect.status") {
      description = "Allows viewing data collection session status"
      default = Permission.Default.FALSE
    }
    register("sloth.prob") {
      description = "Allows usage of the probability display command"
      default = Permission.Default.OP
      children = listOf("sloth.prob.self", "sloth.prob.list")
    }
    register("sloth.prob.self") {
      description = "Allows enabling the probability display only on self"
      default = Permission.Default.FALSE
    }
    register("sloth.prob.list") {
      description = "Allows listing active monitor sessions"
      default = Permission.Default.OP
    }
    register("sloth.view") {
      description = "Allows toggling AI nametag view above players"
      default = Permission.Default.OP
    }
    register("sloth.profile") {
      description = "Allows usage of the profile command"
      default = Permission.Default.OP
    }
    register("sloth.brand") {
      description = "Receive client brand notifications"
      default = Permission.Default.OP
    }
    register("sloth.brand.enable-on-join") {
      description = "Automatically enables brand notifications on join"
      default = Permission.Default.OP
    }
    register("sloth.history") {
      description = "Allows viewing a player's violation history"
      default = Permission.Default.OP
    }
    register("sloth.logs") {
      description = "Allows viewing recent violations"
      default = Permission.Default.OP
    }
    register("sloth.stats") {
      description = "Allows viewing server statistics"
      default = Permission.Default.OP
    }
    register("sloth.exempt.manage") {
      description = "Allows managing punishment exemptions for players"
      default = Permission.Default.OP
    }
    register("sloth.punish.manage") {
      description = "Allows managing player punishments"
      default = Permission.Default.OP
    }
    register("sloth.suspicious") {
      description = "Permission for suspicious player commands"
      default = Permission.Default.OP
      children =
        listOf(
          "sloth.suspicious.alerts",
          "sloth.suspicious.list",
          "sloth.suspicious.top",
          "sloth.suspicious.flagged",
        )
    }
    register("sloth.suspicious.alerts") {
      description = "Allows toggling suspicious player alerts"
      default = Permission.Default.OP
    }
    register("sloth.suspicious.alerts.enable-on-join") {
      description = "Automatically enables suspicious alerts on join"
      default = Permission.Default.OP
    }
    register("sloth.suspicious.list") {
      description = "Allows listing suspicious players"
      default = Permission.Default.OP
    }
    register("sloth.suspicious.top") {
      description = "Allows viewing the top suspicious player"
      default = Permission.Default.OP
    }
    register("sloth.suspicious.flagged") {
      description = "Allows viewing online players with recorded flags"
      default = Permission.Default.OP
    }
  }
}

spotless {
  isEnforceCheck = true

  kotlin {
    target("src/**/*.kt")
    ktfmt().googleStyle()
  }

  kotlinGradle {
    target("*.gradle.kts")
    ktfmt().googleStyle()
  }
}
