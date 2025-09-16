/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
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
package space.kaelus.sloth.utils;

import lombok.Getter;

@Getter
public enum Message {
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
  DATACOLLECT_GLOBAL_STOP_PREVIOUS("datacollect.global-stop-previous"),
  DATACOLLECT_GLOBAL_START_SUCCESS("datacollect.global-start-success"),
  DATACOLLECT_GLOBAL_STARTED_FOR_PLAYERS("datacollect.global-started-for-players"),
  DATACOLLECT_GLOBAL_STOP_FAIL("datacollect.global-stop-fail"),
  DATACOLLECT_GLOBAL_STOP_SUCCESS("datacollect.global-stop-success"),
  DATACOLLECT_GLOBAL_STOP_ARCHIVED("datacollect.global-stop-archived"),
  DATACOLLECT_GLOBAL_STATUS_FAIL("datacollect.global-status-fail"),
  DATACOLLECT_GLOBAL_STATUS_HEADER("datacollect.global-status-header"),
  DATACOLLECT_GLOBAL_STATUS_ACTIVE("datacollect.global-status-active"),
  DATACOLLECT_GLOBAL_STATUS_SESSION_ID("datacollect.global-status-session-id"),
  DATACOLLECT_GLOBAL_STATUS_PLAYERS_HEADER("datacollect.global-status-players-header"),
  DATACOLLECT_GLOBAL_STATUS_PLAYER_ENTRY("datacollect.global-status-player-entry"),

  // Prob
  PROB_ENABLED("prob.enabled"),
  PROB_DISABLED("prob.disabled"),
  PROB_NO_DATA("prob.no-data"),
  PROB_NO_AICHECK("prob.no-aicheck"),
  PROB_FORMAT_LABEL_PROB("prob.format.label-prob"),
  PROB_FORMAT_LABEL_BUFFER("prob.format.label-buffer"),
  PROB_FORMAT_LABEL_PING("prob.format.label-ping"),
  PROB_FORMAT_SEPARATOR("prob.format.separator"),
  PROB_FORMAT_SUFFIX_PING("prob.format.suffix-ping"),

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
  LOGS_INVALID_TIME("logs.invalid-time"),

  // Punish
  PUNISH_RESET_SUCCESS("punish.reset-success"),

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
  TIME_SECONDS("time.seconds");

  private final String path;

  Message(String path) {
    this.path = path;
  }
}
