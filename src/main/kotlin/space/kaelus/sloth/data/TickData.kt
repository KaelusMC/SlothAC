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
package space.kaelus.sloth.data

import kotlin.math.abs
import kotlin.math.roundToLong
import space.kaelus.sloth.checks.impl.combat.AimProcessor
import space.kaelus.sloth.player.SlothPlayer

class TickData(slothPlayer: SlothPlayer) {
  val deltaYaw: Float
  val deltaPitch: Float
  val accelYaw: Float
  val accelPitch: Float
  val jerkPitch: Float
  val jerkYaw: Float
  val gcdErrorYaw: Float
  val gcdErrorPitch: Float

  init {
    val aimProcessor =
      requireNotNull(slothPlayer.checkManager.getCheck(AimProcessor::class.java)) {
        "AimProcessor is not available for TickData"
      }

    deltaYaw = slothPlayer.movement.yaw - slothPlayer.movement.lastYaw
    deltaPitch = slothPlayer.movement.pitch - slothPlayer.movement.lastPitch

    accelYaw = aimProcessor.currentYawAccel
    accelPitch = aimProcessor.currentPitchAccel

    jerkYaw = accelYaw - aimProcessor.lastYawAccel
    jerkPitch = accelPitch - aimProcessor.lastPitchAccel

    gcdErrorYaw =
      if (aimProcessor.modeX > 0) {
        val errorX = kotlin.math.abs(deltaYaw.toDouble() % aimProcessor.modeX)
        kotlin.math.min(errorX, aimProcessor.modeX - errorX).toFloat()
      } else {
        0f
      }
    gcdErrorPitch =
      if (aimProcessor.modeY > 0) {
        val errorY = kotlin.math.abs(deltaPitch.toDouble() % aimProcessor.modeY)
        kotlin.math.min(errorY, aimProcessor.modeY - errorY).toFloat()
      } else {
        0f
      }
  }

  fun toCsv(status: String): String {
    return buildString { appendCsv(this, status) }
  }

  fun appendCsv(out: Appendable, status: String) {
    val cheatingStatus = if (status.equals("CHEAT", ignoreCase = true)) 1 else 0
    out.append(if (cheatingStatus == 1) '1' else '0')
    out.append(',')
    appendFixed6(out, deltaYaw)
    out.append(',')
    appendFixed6(out, deltaPitch)
    out.append(',')
    appendFixed6(out, accelYaw)
    out.append(',')
    appendFixed6(out, accelPitch)
    out.append(',')
    appendFixed6(out, jerkYaw)
    out.append(',')
    appendFixed6(out, jerkPitch)
    out.append(',')
    appendFixed6(out, gcdErrorYaw)
    out.append(',')
    appendFixed6(out, gcdErrorPitch)
  }

  companion object {
    @JvmStatic
    fun getHeader(): String {
      return "is_cheating,delta_yaw,delta_pitch,accel_yaw,accel_pitch,jerk_yaw,jerk_pitch," +
        "gcd_error_yaw,gcd_error_pitch"
    }

    private const val SCALE = 1_000_000L

    private fun appendFixed6(out: Appendable, value: Float) {
      if (!value.isFinite()) {
        out.append("0.000000")
        return
      }

      var v = value.toDouble()
      val negative = v < 0.0
      if (negative) {
        v = -v
      }
      val scaled = (v * SCALE).roundToLong()
      val integerPart = scaled / SCALE
      val fractionPart = (scaled % SCALE).toInt()

      if (negative) {
        out.append('-')
      }
      out.append(integerPart.toString())
      out.append('.')
      val fractionText = fractionPart.toString()
      for (i in fractionText.length until 6) {
        out.append('0')
      }
      out.append(fractionText)
    }
  }
}
