<div align="center">
  <h1>SlothAC</h1>
  <p>An AI-powered, free and open-source anti-cheat for Minecraft servers.</p>

  <p>
    <a href="https://github.com/KaelusMC/SlothAC/actions/workflows/ci.yml">
      <img alt="CI" src="https://github.com/KaelusMC/SlothAC/actions/workflows/ci.yml/badge.svg">
    </a>
    <a href="https://www.codefactor.io/repository/github/kaelusmc/slothac">
      <img alt="CodeFactor" src="https://www.codefactor.io/repository/github/kaelusmc/slothac/badge">
    </a>
    <a href="https://dsc.gg/kaelus">
      <img alt="Discord" src="https://img.shields.io/discord/1297490292349468715?style=flat&label=Discord&logo=discord&color=7289DA&logoColor=white">
    </a>
    <a href="https://github.com/KaelusMC/SlothAC/">
      <img alt="Views" src="https://api.visitorbadge.io/api/visitors?path=https%3A%2F%2Fgithub.com%2FKaelusMC%2FSlothAC%2FREADME.md&label=Views&countColor=%23555555&style=flat&labelStyle=none">
    </a>
  </p>

  <p>
    <a href="README.md"><b>English</b></a>
    ·
    <a href="README.ru.md">Русский</a>
  </p>
</div>

## What Sloth is

Sloth is an open-source anti-cheat plugin for Minecraft servers.

## Important before you install

Sloth's AI check uses the official Sloth API. Access is arranged in the [Discord server](https://dsc.gg/kaelus).

To connect a server to the API, run `/sloth connect` and authorize it in the [Sloth panel](https://panel.kaelus.dev).

If API access is not available yet, disable the AI check for now.

## Requirements

- Java 17 or newer to run the plugin
- JDK 21 or newer if you want to build from source
- A Paper or Folia-based server
- A configured AI inference API if the AI check is enabled

## Installation

1. Download the latest release from [GitHub Releases](https://github.com/KaelusMC/SlothAC/releases).
2. Place the main `SlothAC-<version>.jar` in the server `plugins/` directory.
3. Start the server once so Sloth can generate its configuration files.
4. Run `/sloth connect` and authorize the server in the panel.
5. If needed, configure storage:
   - SQLite is the default
   - MySQL and MariaDB are also supported
6. If WorldGuard is installed, specific regions can be excluded from the AI check.
7. Restart the server or reload the Sloth configuration.

## Configuration files

- [`config.yml`](src/main/resources/config.yml): AI, database, Redis and cross-server alerts, alerts, duplicate packet handling
- [`monitor.yml`](src/main/resources/monitor.yml): formatting for `/sloth monitor` and `/sloth view`
- [`punishments.yml`](src/main/resources/punishments.yml): punishment rules
- [`messages/messages_en.yml`](src/main/resources/messages/messages_en.yml): English messages
- [`messages/messages_ru.yml`](src/main/resources/messages/messages_ru.yml): Russian messages

## Main commands

| Command | Purpose |
| --- | --- |
| `/sloth connect` | Link this server to the panel |
| `/sloth connect status` | Show the panel connection status |
| `/sloth disconnect` | Unlink this server from the panel |
| `/sloth alerts` | Toggle violation alerts |
| `/sloth suspicious <list\|top\|flagged>` | Review suspicious or previously flagged online players |
| `/sloth profile <player>` | Open a player's live profile |
| `/sloth monitor <player>` | Watch AI data for one player in real time |
| `/sloth view` | Toggle observation mode for nearby players |
| `/sloth logs [page]` | View recent violations |
| `/sloth history <player> [page]` | View a player's stored violation history |
| `/sloth stats` | View server-side anti-cheat stats |
| `/sloth dc <start\|stop\|status>` | Manage labeled data collection sessions |
| `/sloth reload` | Reload Sloth configuration |

For the full command list, use `/sloth help` in game.

## Building from source

```bash
git clone https://github.com/KaelusMC/SlothAC.git
cd SlothAC
./gradlew shadowJar
```

The main plugin jar will be written to:

```text
build/libs/SlothAC-<version>.jar
```

## Help, bugs, and discussion

- Bug reports: [GitHub Issues](https://github.com/KaelusMC/SlothAC/issues)
- Community / support: [Discord](https://dsc.gg/kaelus)

Issue reports should include:

- server version
- Java version
- plugin version
- relevant config values
- logs, stack traces, and steps to reproduce

That makes problems easier to reproduce and fix.

## Credits

Sloth has its own, independently developed codebase. That said, some parts of its code are adapted from the open-source [GrimAC](https://github.com/GrimAnticheat/Grim) project, and Sloth builds on ideas developed by GrimAC, DefineOutside, and other GrimAC contributors - full credit and thanks to them for their work.

## License

Sloth is distributed under the terms of the [GNU General Public License v3.0](LICENSE).
