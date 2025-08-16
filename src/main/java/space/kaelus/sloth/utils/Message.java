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
    PROB_ACTIONBAR_FORMAT("prob.actionbar-format"),

    // Profile
    PROFILE_NO_DATA("profile.no-data"),
    PROFILE_LINES("profile.lines"),

    // History
    HISTORY_DISABLED("history.disabled"),
    HISTORY_HEADER("history.header"),
    HISTORY_ENTRY("history.entry"),
    HISTORY_NO_VIOLATIONS("history.no-violations"),

    // Punish
    PUNISH_RESET_SUCCESS("punish.reset-success"),

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