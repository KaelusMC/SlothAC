import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission

plugins {
    id("java")
    id("io.freefair.lombok") version "8.6"
    id("com.gradleup.shadow") version "9.0.0-beta6"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
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
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.10")
    compileOnly("me.clip:placeholderapi:2.11.6")

    // PacketEvents
    implementation("com.github.retrooper:packetevents-spigot:2.9.1-SNAPSHOT")

    // Cloud Command Framework
    implementation("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("org.incendo:cloud-processors-requirements:1.0.0-rc.1")

    // Adventure & MiniMessage
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")

    // Utilities
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")

    minimize()

    relocate("io.github.retrooper.packetevents", "space.kaelus.sloth.libs.packetevents")
    relocate("com.github.retrooper.packetevents", "space.kaelus.sloth.libs.packetevents")
    relocate("net.kyori", "space.kaelus.sloth.libs.kyori")
    relocate("com.google.gson", "space.kaelus.sloth.libs.gson")
    relocate("org.incendo", "space.kaelus.sloth.libs.incendo")
    relocate("io.leangen.geantyref", "space.kaelus.sloth.libs.geantyref")
    relocate("it.unimi.dsi.fastutil", "space.kaelus.sloth.libs.fastutil")
}

tasks.build {
    dependsOn(tasks.shadowJar)
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

    commands {
        register("sloth") {
            aliases = listOf("slothac")
            description = "Main command for SlothAC"
            permission = "sloth.help"
        }
    }

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
    }
}