/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2026 KaelusMC
 *
 * SlothAC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SlothAC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.di

import java.util.logging.Logger
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.incendo.cloud.SenderMapper
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import space.kaelus.sloth.SlothAC
import space.kaelus.sloth.SlothCore
import space.kaelus.sloth.ai.AiResponseParser
import space.kaelus.sloth.ai.AiSerializer
import space.kaelus.sloth.ai.AiService
import space.kaelus.sloth.ai.DefaultAiService
import space.kaelus.sloth.ai.FlatBuffersAiSerializer
import space.kaelus.sloth.ai.GsonAiResponseParser
import space.kaelus.sloth.alert.AlertManager
import space.kaelus.sloth.api.SlothApi
import space.kaelus.sloth.api.event.SlothEventBus
import space.kaelus.sloth.api.event.internal.SlothEventBusImpl
import space.kaelus.sloth.api.internal.AiApiImpl
import space.kaelus.sloth.api.internal.CheckApiImpl
import space.kaelus.sloth.api.internal.MonitorApiImpl
import space.kaelus.sloth.api.internal.PunishmentApiImpl
import space.kaelus.sloth.api.internal.SlothApiImpl
import space.kaelus.sloth.api.service.AiApi
import space.kaelus.sloth.api.service.CheckApi
import space.kaelus.sloth.api.service.MonitorApi
import space.kaelus.sloth.api.service.PunishmentApi
import space.kaelus.sloth.checks.CheckFactory
import space.kaelus.sloth.checks.CheckManager
import space.kaelus.sloth.checks.impl.ai.ActionManager
import space.kaelus.sloth.checks.impl.ai.AiCheck
import space.kaelus.sloth.checks.impl.ai.DataCollectorCheck
import space.kaelus.sloth.checks.impl.ai.DataCollectorManager
import space.kaelus.sloth.checks.impl.combat.AimProcessor
import space.kaelus.sloth.checks.impl.misc.ClientBrand
import space.kaelus.sloth.command.CommandManager
import space.kaelus.sloth.command.CommandRegister
import space.kaelus.sloth.command.SlothCommand
import space.kaelus.sloth.command.commands.admin.AlertsCommand
import space.kaelus.sloth.command.commands.admin.BrandsCommand
import space.kaelus.sloth.command.commands.admin.DataCollectCommand
import space.kaelus.sloth.command.commands.admin.ExemptCommand
import space.kaelus.sloth.command.commands.admin.PunishCommand
import space.kaelus.sloth.command.commands.admin.ReloadCommand
import space.kaelus.sloth.command.commands.admin.SuspiciousCommand
import space.kaelus.sloth.command.commands.info.HelpCommand
import space.kaelus.sloth.command.commands.info.HistoryCommand
import space.kaelus.sloth.command.commands.info.LogsCommand
import space.kaelus.sloth.command.commands.info.MonitorCommand
import space.kaelus.sloth.command.commands.info.ProfileCommand
import space.kaelus.sloth.command.commands.info.StatsCommand
import space.kaelus.sloth.command.handler.SlothCommandFailureHandler
import space.kaelus.sloth.config.ConfigManager
import space.kaelus.sloth.config.LocaleManager
import space.kaelus.sloth.coroutines.SlothCoroutines
import space.kaelus.sloth.damage.AiDamageProcessor
import space.kaelus.sloth.damage.DamageProcessor
import space.kaelus.sloth.database.DatabaseManager
import space.kaelus.sloth.debug.DebugManager
import space.kaelus.sloth.event.DamageEvent
import space.kaelus.sloth.integration.WorldGuardManager
import space.kaelus.sloth.monitor.MonitorSettingsService
import space.kaelus.sloth.packet.PacketListener
import space.kaelus.sloth.platform.scheduler.PlatformScheduler
import space.kaelus.sloth.platform.scheduler.PlatformSchedulerFactory
import space.kaelus.sloth.player.ExemptManager
import space.kaelus.sloth.player.PlayerDataManager
import space.kaelus.sloth.punishment.PunishmentManager
import space.kaelus.sloth.region.RegionProvider
import space.kaelus.sloth.scheduler.SchedulerService
import space.kaelus.sloth.sender.Sender
import space.kaelus.sloth.sender.SenderFactory
import space.kaelus.sloth.server.AIServerProvider

fun slothModules(plugin: SlothAC) =
  listOf(coreModule(plugin), aiModule(), apiModule(), commandModule(), checkModule())

private fun coreModule(plugin: SlothAC) = module {
  single { plugin }
  single { BukkitAudiences.create(plugin) }
  single<Logger> { plugin.logger }
  single<PlatformScheduler> { PlatformSchedulerFactory.create() }
  single<SlothEventBus> { SlothEventBusImpl() }

  singleOf(::SchedulerService)
  singleOf(::SlothCoroutines)
  singleOf(::ConfigManager)
  singleOf(::LocaleManager)
  singleOf(::DatabaseManager)
  singleOf(::DebugManager)
  singleOf(::AIServerProvider)
  singleOf(::AlertManager)
  singleOf(::MonitorSettingsService)
  singleOf(::ExemptManager)
  singleOf(::DataCollectorManager)
  singleOf(::WorldGuardManager)
  single<RegionProvider> { get<WorldGuardManager>() }
  single<DamageProcessor> { AiDamageProcessor(get()) }

  singleOf(::SenderFactory).bind<SenderMapper<CommandSender, Sender>>()

  singleOf(::SlothCommandFailureHandler)

  singleOf(::PlayerDataManager)
  singleOf(::PacketListener)
  singleOf(::DamageEvent)

  singleOf(::SlothCore)
}

private fun aiModule() = module {
  singleOf(::FlatBuffersAiSerializer).bind<AiSerializer>()
  singleOf(::GsonAiResponseParser).bind<AiResponseParser>()
  singleOf(::DefaultAiService).bind<AiService>()
}

private fun apiModule() = module {
  singleOf(::AiApiImpl).bind<AiApi>()
  singleOf(::CheckApiImpl).bind<CheckApi>()
  singleOf(::MonitorApiImpl).bind<MonitorApi>()
  singleOf(::PunishmentApiImpl).bind<PunishmentApi>()
  singleOf(::SlothApiImpl).bind<SlothApi>()
}

private fun commandModule() = module {
  includes(adminCommandsModule(), infoCommandsModule())

  single { CommandRegister(getAll(), get()) }
  singleOf(::CommandManager)
}

private fun adminCommandsModule() = module {
  singleOf(::AlertsCommand).bind<SlothCommand>()
  singleOf(::BrandsCommand).bind<SlothCommand>()
  singleOf(::DataCollectCommand).bind<SlothCommand>()
  singleOf(::ExemptCommand).bind<SlothCommand>()
  singleOf(::PunishCommand).bind<SlothCommand>()
  singleOf(::ReloadCommand).bind<SlothCommand>()
  singleOf(::SuspiciousCommand).bind<SlothCommand>()
}

private fun infoCommandsModule() = module {
  singleOf(::HelpCommand).bind<SlothCommand>()
  singleOf(::HistoryCommand).bind<SlothCommand>()
  singleOf(::LogsCommand).bind<SlothCommand>()
  singleOf(::MonitorCommand).bind<SlothCommand>()
  singleOf(::ProfileCommand).bind<SlothCommand>()
  singleOf(::StatsCommand).bind<SlothCommand>()
}

private fun checkModule() = module {
  single<CheckFactory>(named("aim")) { CheckFactory { player -> AimProcessor(player) } }
  single<CheckFactory>(named("action")) { CheckFactory { player -> ActionManager(player, get()) } }
  single<CheckFactory>(named("ai")) {
    CheckFactory { player ->
      AiCheck(player, get(), get(), get(), get(), get(), get(), get(), get())
    }
  }
  single<CheckFactory>(named("collector")) {
    CheckFactory { player -> DataCollectorCheck(player, get(), get()) }
  }
  single<CheckFactory>(named("brand")) {
    CheckFactory { player -> ClientBrand(player, get(), get()) }
  }

  single<Set<CheckFactory>> { getAll<CheckFactory>().toSet() }

  single<CheckManager.Factory> { CheckManager.Factory { player -> CheckManager(player, get()) } }
  single<PunishmentManager.Factory> {
    PunishmentManager.Factory { player ->
      PunishmentManager(player, get(), get(), get(), get(), get(), get(), get())
    }
  }
}
