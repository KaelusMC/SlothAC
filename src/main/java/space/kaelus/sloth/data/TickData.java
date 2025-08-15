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
package space.kaelus.sloth.data;

import space.kaelus.sloth.checks.impl.aim.AimProcessor;
import space.kaelus.sloth.player.SlothPlayer;
import java.util.Locale;
import java.util.StringJoiner;

public class TickData {
    public final float deltaYaw, deltaPitch;
    public final float accelYaw, accelPitch;
    public final float jerkPitch, jerkYaw;
    public final float gcdErrorYaw, gcdErrorPitch;

    public TickData(SlothPlayer slothPlayer) {
        AimProcessor aimProcessor = slothPlayer.getCheckManager().getCheck(AimProcessor.class);

        this.deltaYaw = slothPlayer.yaw - slothPlayer.lastYaw;
        this.deltaPitch = slothPlayer.pitch - slothPlayer.lastPitch;

        this.accelYaw = aimProcessor.getCurrentYawAccel();
        this.accelPitch = aimProcessor.getCurrentPitchAccel();

        this.jerkYaw = this.accelYaw - aimProcessor.getLastYawAccel();
        this.jerkPitch = this.accelPitch - aimProcessor.getLastPitchAccel();

        if (aimProcessor.getModeX() > 0) {
            double errorX = Math.abs(this.deltaYaw % aimProcessor.getModeX());
            this.gcdErrorYaw = (float) Math.min(errorX, aimProcessor.getModeX() - errorX);
        } else {
            this.gcdErrorYaw = 0;
        }
        if (aimProcessor.getModeY() > 0) {
            double errorY = Math.abs(this.deltaPitch % aimProcessor.getModeY());
            this.gcdErrorPitch = (float) Math.min(errorY, aimProcessor.getModeY() - errorY);
        } else {
            this.gcdErrorPitch = 0;
        }
    }

    public static String getHeader() {
        return "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch," +
                "gcd_error_yaw,gcd_error_pitch";
    }

    public String toCsv(String status) {
        int cheatingStatus = status.equalsIgnoreCase("CHEAT") ? 1 : 0;
        StringJoiner joiner = new StringJoiner(",");
        joiner.add(String.valueOf(cheatingStatus));
        joiner.add(String.format(Locale.US, "%.6f", deltaYaw));
        joiner.add(String.format(Locale.US, "%.6f", deltaPitch));
        joiner.add(String.format(Locale.US, "%.6f", accelYaw));
        joiner.add(String.format(Locale.US, "%.6f", accelPitch));
        joiner.add(String.format(Locale.US, "%.6f", jerkYaw));
        joiner.add(String.format(Locale.US, "%.6f", jerkPitch));
        joiner.add(String.format(Locale.US, "%.6f", gcdErrorYaw));
        joiner.add(String.format(Locale.US, "%.6f", gcdErrorPitch));
        return joiner.toString();
    }
}