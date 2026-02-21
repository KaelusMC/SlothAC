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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package space.kaelus.sloth.utils

enum class Message(val path: String) {
  PREFIX("prefix"),
  ALERTS_ENABLED("alerts-enabled"),
  ALERTS_DISABLED("alerts-disabled"),
  ALERTS_FORMAT("alerts-format"),
  PLAYER_NOT_FOUND("player-not-found"),
  RUN_AS_PLAYER("run-as-player"),
  RELOAD_START("reload-start"),
  RELOAD_SUCCESS("reload-success"),

  // Brand
  BRAND_ALERTS_ENABLED("brand.alerts-enabled"),
  BRAND_ALERTS_DISABLED("brand.alerts-disabled"),
  BRAND_NOTIFICATION("brand.notification"),
  BRAND_DISCONNECT_FORGE("brand.disconnect-forge"),

  // DataCollect
  DATACOLLECT_DETAILS_REQUIRED("datacollect.details-required"),
  DATACOLLECT_INVALID_TYPE("datacollect.invalid-type"),
  DATACOLLECT_START_SUCCESS("datacollect.start-success"),
  DATACOLLECT_START_RESTARTED("datacollect.start-restarted"),
  DATACOLLECT_STOP_SUCCESS("datacollect.stop-success"),
  DATACOLLECT_STOP_FAIL("datacollect.stop-fail"),
  DATACOLLECT_STATUS_HEADER("datacollect.status-header"),
  DATACOLLECT_STATUS_PLAYER("datacollect.status-player"),
  DATACOLLECT_STATUS_NONE("datacollect.status-none"),
  DATACOLLECT_STATUS_NO_SESSION("datacollect.status-no-session"),

  // Monitor
  MONITOR_ENABLED("monitor.enabled"),
  MONITOR_DISABLED("monitor.disabled"),
  MONITOR_NO_DATA("monitor.no-data"),
  MONITOR_NO_AICHECK("monitor.no-aicheck"),
  MONITOR_SETTING_UPDATED("monitor.setting-updated"),
  MONITOR_INVALID_SETTING("monitor.invalid-setting"),

  // Profile
  PROFILE_NO_DATA("profile.no-data"),
  PROFILE_LINES("profile.lines"),

  // History
  HISTORY_DISABLED("history.disabled"),
  HISTORY_HEADER("history.header"),
  HISTORY_ENTRY("history.entry"),
  HISTORY_NO_VIOLATIONS("history.no-violations"),

  // Logs
  LOGS_HEADER("logs.header"),
  LOGS_ENTRY("logs.entry"),
  LOGS_NO_VIOLATIONS("logs.no-violations"),

  // Punish
  PUNISH_RESET_SUCCESS("punish.reset-success"),

  // Exempt
  EXEMPT_INVALID_DURATION("exempt.invalid-duration"),
  EXEMPT_SUCCESS_PERM("exempt.success-perm"),
  EXEMPT_SUCCESS_TEMP("exempt.success-temp"),
  EXEMPT_REMOVE_SUCCESS("exempt.remove-success"),
  EXEMPT_REMOVE_FAIL("exempt.remove-fail"),
  EXEMPT_STATUS_PERM_PERMISSION("exempt.status-perm-permission"),
  EXEMPT_STATUS_PERM_COMMAND("exempt.status-perm-command"),
  EXEMPT_STATUS_NOT_EXEMPT("exempt.status-not-exempt"),
  EXEMPT_STATUS_EXPIRED("exempt.status-expired"),
  EXEMPT_STATUS_TEMP("exempt.status-temp"),

  // Suspicious
  SUSPICIOUS_ALERTS_ENABLED("suspicious.alerts-enabled"),
  SUSPICIOUS_ALERTS_DISABLED("suspicious.alerts-disabled"),
  SUSPICIOUS_ALERT_TRIGGERED("suspicious.alert-triggered"),
  SUSPICIOUS_LIST_EMPTY("suspicious.list-empty"),
  SUSPICIOUS_LIST_HEADER("suspicious.list-header"),
  SUSPICIOUS_LIST_ENTRY("suspicious.list-entry"),
  SUSPICIOUS_TOP_NONE("suspicious.top-none"),
  SUSPICIOUS_TOP_PLAYER("suspicious.top-player"),

  // Stats
  STATS_LINES("stats.lines"),

  // Help
  HELP_MESSAGE("help"),

  // Internal
  INTERNAL_ERROR("internal.error"),

  // Time formats
  TIME_AGO("time.ago"),
  TIME_DAYS("time.days"),
  TIME_HOURS("time.hours"),
  TIME_MINUTES("time.minutes"),
  TIME_SECONDS("time.seconds"),
}
