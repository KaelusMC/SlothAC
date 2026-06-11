<div align="center">
  <h1>SlothAC</h1>
  <p>Свободный AI-античит с открытым исходным кодом для Minecraft-серверов.</p>

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
    <a href="README.md">English</a>
    ·
    <a href="README.ru.md"><b>Русский</b></a>
  </p>
</div>

## Что такое Sloth

Sloth - это AI-античит с открытым исходным кодом для Minecraft-серверов.

## Важный момент перед установкой

AI-проверка Sloth использует официальный Sloth API. Доступ запрашивается в [Discord](https://dsc.gg/kaelus).

Чтобы подключить сервер к API, выполните `/sloth connect` и подтвердите привязку в [панели Sloth](https://panel.kaelus.dev).

При отсутствии доступа к API AI-проверку следует временно отключить.

## Требования

- Java 17+ для запуска плагина
- JDK 21+ для сборки проекта
- сервер на Paper или Folia
- настроенный AI API при включённой AI-проверке

## Установка

1. Скачать актуальный релиз из [GitHub Releases](https://github.com/KaelusMC/SlothAC/releases).
2. Поместить основной `SlothAC-<version>.jar` в каталог `plugins/`.
3. Один раз запустить сервер, чтобы Sloth создал конфиги.
4. Выполнить `/sloth connect` и подтвердить привязку в панели.
5. При необходимости настроить хранилище:
   - SQLite используется по умолчанию
   - MySQL и MariaDB тоже поддерживаются
6. При использовании WorldGuard можно исключить нужные регионы из AI-проверки.
7. Перезапустить сервер или перезагрузить конфигурацию плагина.

## Файлы конфигурации

- [`config.yml`](src/main/resources/config.yml): AI, база данных, Redis, межсерверные оповещения, алерты и обработка дублирующихся пакетов движения
- [`monitor.yml`](src/main/resources/monitor.yml): формат `/sloth monitor` и `/sloth view`
- [`punishments.yml`](src/main/resources/punishments.yml): правила наказаний
- [`messages/messages_en.yml`](src/main/resources/messages/messages_en.yml): английская локализация
- [`messages/messages_ru.yml`](src/main/resources/messages/messages_ru.yml): русская локализация

## Основные команды

| Команда | Что делает |
| --- | --- |
| `/sloth connect` | Привязывает сервер к панели |
| `/sloth connect status` | Показывает статус подключения к панели |
| `/sloth disconnect` | Отвязывает сервер от панели |
| `/sloth alerts` | Включает и выключает уведомления о нарушениях |
| `/sloth suspicious <list\|top\|flagged>` | Показывает подозрительных игроков и онлайн-игроков с флагами |
| `/sloth profile <player>` | Открывает профиль игрока |
| `/sloth monitor <player>` | Показывает AI-данные игрока в реальном времени |
| `/sloth view` | Переключает режим наблюдения за игроками |
| `/sloth logs [page]` | Показывает недавние нарушения |
| `/sloth history <player> [page]` | Показывает историю нарушений игрока |
| `/sloth stats` | Показывает статистику античита по серверу |
| `/sloth dc <start\|stop\|cancel\|status>` | Управляет сессиями сбора данных |
| `/sloth reload` | Перезагружает конфигурацию Sloth |

Полный список команд доступен через `/sloth help`.

## Сборка из исходников

```bash
git clone https://github.com/KaelusMC/SlothAC.git
cd SlothAC
./gradlew shadowJar
```

Основной jar-файл:

```text
build/libs/SlothAC-<version>.jar
```

## Помощь, баг-репорты и обсуждение

- Баг-репорты: [GitHub Issues](https://github.com/KaelusMC/SlothAC/issues)
- Сообщество и поддержка: [Discord](https://dsc.gg/kaelus)

При создании issue рекомендуется приложить:

- версию сервера
- версию Java
- версию плагина
- важные фрагменты конфига
- логи, stack trace и шаги воспроизведения

Это ускоряет воспроизведение и исправление проблемы.

## Благодарности

У Sloth собственная, независимо разработанная кодовая база. Тем не менее часть его кода адаптирована из open-source проекта [GrimAC](https://github.com/GrimAnticheat/Grim), и Sloth опирается на идеи, разработанные GrimAC, DefineOutside и другими участниками проекта GrimAC - им полная признательность и благодарность за работу.

## Лицензия

Sloth распространяется на условиях лицензии [GNU General Public License v3.0](LICENSE).
