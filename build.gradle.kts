import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import net.ltgt.gradle.errorprone.errorprone
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission

plugins {
    id("java")
    id("io.freefair.lombok") version "8.6"
    id("com.gradleup.shadow") version "9.0.0-beta6"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("net.ltgt.errorprone") version "4.3.0"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "space.kaelus.sloth"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.enginehub.org/repo/") // WorldGuard
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
}

dependencies {
    // Bukkit APIs
    compileOnly("io.papermc.paper:paper-api:1.21.9-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14")
    compileOnly("me.clip:placeholderapi:2.11.6")

    // PacketEvents
    implementation("com.github.retrooper:packetevents-spigot:2.10.1")

    // Cloud Command Framework
    implementation("org.incendo:cloud-paper:2.0.0-beta.13")
    implementation("org.incendo:cloud-processors-requirements:1.0.0-rc.1")

    // Adventure & MiniMessage
    implementation("net.kyori:adventure-platform-bukkit:4.4.0")
    implementation("net.kyori:adventure-text-minimessage:4.23.0")

    // HikariCP
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.slf4j:slf4j-jdk14:2.0.17")

    // Utilities
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
    implementation("com.google.code.gson:gson:2.11.0")

    // Dagger
    implementation("com.google.dagger:dagger:2.51")
    annotationProcessor("com.google.dagger:dagger-compiler:2.51")

    // Error Prone
    errorprone("com.google.errorprone:error_prone_core:2.44.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile> {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.errorprone.disableWarningsInGeneratedCode.set(true)
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")

    minimize {
        exclude(dependency("org.slf4j:slf4j-api"))
        exclude(dependency("org.slf4j:slf4j-jdk14"))
    }

    transformers.add(ServiceFileTransformer())

    relocate("com.github.retrooper.packetevents", "space.kaelus.sloth.libs.packetevents.api")
    relocate("io.github.retrooper.packetevents", "space.kaelus.sloth.libs.packetevents.impl")
    relocate("net.kyori", "space.kaelus.sloth.libs.kyori")
    relocate("com.google.gson", "space.kaelus.sloth.libs.gson")
    relocate("org.incendo", "space.kaelus.sloth.libs.incendo")
    relocate("io.leangen.geantyref", "space.kaelus.sloth.libs.geantyref")
    relocate("it.unimi.dsi.fastutil", "space.kaelus.sloth.libs.fastutil")
    relocate("com.google.flatbuffers", "space.kaelus.sloth.libs.flatbuffers")
    relocate("com.zaxxer", "space.kaelus.sloth.libs.hikari")
    relocate("org.slf4j", "space.kaelus.sloth.libs.slf4j")
    relocate("org.jetbrains", "space.kaelus.sloth.libs.jetbrains")
    relocate("org.intellij", "space.kaelus.sloth.libs.intellij")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.spotlessApply)
}

bukkit {
    name = "SlothAC"
    main = "space.kaelus.sloth.SlothAC"
    version = project.version.toString()
    apiVersion = "1.13"
    authors = listOf("KaelusMC")
    website = "https://dsc.gg/kaelus"
    //foliaSupported = true
    softDepend = listOf(
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
        register("sloth.exempt") {
            description = "Exempt from all checks"
            default = Permission.Default.FALSE
        }
        register("sloth.datacollect") {
            description = "Allows usage of the data collection commands"
            default = Permission.Default.OP
        }
        register("sloth.prob") {
            description = "Allows usage of the probability display command"
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
            children = listOf("sloth.suspicious.alerts", "sloth.suspicious.list", "sloth.suspicious.top")
        }
        register("sloth.suspicious.alerts") {
            description = "Allows toggling suspicious player alerts"
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
    }
}

spotless {
    isEnforceCheck = true

    java {
        importOrder()

        removeUnusedImports()

        googleJavaFormat("1.17.0")
    }
}