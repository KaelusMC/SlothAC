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
package space.kaelus.sloth.alert;

import lombok.Getter;
import space.kaelus.sloth.utils.Message;

@Getter
public enum AlertType {
  REGULAR("sloth.alerts", Message.ALERTS_ENABLED, Message.ALERTS_DISABLED),
  BRAND("sloth.brand", Message.BRAND_ALERTS_ENABLED, Message.BRAND_ALERTS_DISABLED),
  SUSPICIOUS(
      "sloth.suspicious.alerts",
      Message.SUSPICIOUS_ALERTS_ENABLED,
      Message.SUSPICIOUS_ALERTS_DISABLED);

  private final String permission;
  private final Message enabledMessage;
  private final Message disabledMessage;

  AlertType(String permission, Message enabledMessage, Message disabledMessage) {
    this.permission = permission;
    this.enabledMessage = enabledMessage;
    this.disabledMessage = disabledMessage;
  }
}
