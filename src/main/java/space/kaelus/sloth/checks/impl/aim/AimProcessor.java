/*
 * This file is part of SlothAC - https://github.com/KaelusMC/SlothAC
 * Copyright (C) 2025 KaelusMC
 *
 * This file contains code derived from GrimAC.
 * The original authors of GrimAC are credited below.
 *
 * Copyright (c) 2021-2025 GrimAC, DefineOutside and contributors.
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
package space.kaelus.sloth.checks.impl.aim;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import space.kaelus.sloth.checks.AbstractCheck;
import space.kaelus.sloth.checks.CheckData;
import space.kaelus.sloth.checks.CheckFactory;
import space.kaelus.sloth.checks.type.RotationCheck;
import space.kaelus.sloth.player.SlothPlayer;
import space.kaelus.sloth.utils.lists.RunningMode;
import space.kaelus.sloth.utils.math.SlothMath;
import space.kaelus.sloth.utils.update.RotationUpdate;

@CheckData(name = "AimProcessor_Internal")
@Getter
public class AimProcessor extends AbstractCheck implements RotationCheck {

  private static final int SIGNIFICANT_SAMPLES_THRESHOLD = 15;
  private static final int TOTAL_SAMPLES_THRESHOLD = 80;

  public double sensitivityX;
  public double sensitivityY;
  public double divisorX;
  public double divisorY;
  public double modeX, modeY;
  public double deltaDotsX, deltaDotsY;
  private final RunningMode xRotMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);
  private final RunningMode yRotMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);
  private float lastXRot;
  private float lastYRot;

  private float lastDeltaYaw = 0.0f;
  private float lastDeltaPitch = 0.0f;

  private float lastYawAccel = 0.0f;
  private float lastPitchAccel = 0.0f;

  private float currentYawAccel = 0.0f;
  private float currentPitchAccel = 0.0f;

  @AssistedInject
  public AimProcessor(@Assisted SlothPlayer slothPlayer) {
    super(slothPlayer);
  }

  @AssistedFactory
  public interface Factory extends CheckFactory {
    @Override
    AimProcessor create(SlothPlayer slothPlayer);
  }

  public static double convertToSensitivity(double var13) {
    double var11 = var13 / 0.15F / 8.0D;
    double var9 = Math.cbrt(var11);
    return (var9 - 0.2f) / 0.6f;
  }

  @Override
  public void process(final RotationUpdate rotationUpdate) {
    float deltaYaw = rotationUpdate.getDeltaYaw();
    float deltaPitch = rotationUpdate.getDeltaPitch();
    float deltaYawAbs = Math.abs(deltaYaw);
    float deltaPitchAbs = Math.abs(deltaPitch);
    this.lastYawAccel = this.currentYawAccel;
    this.lastPitchAccel = this.currentPitchAccel;
    this.currentYawAccel = deltaYawAbs - Math.abs(this.lastDeltaYaw);
    this.currentPitchAccel = deltaPitchAbs - Math.abs(this.lastDeltaPitch);
    this.lastDeltaYaw = deltaYaw;
    this.lastDeltaPitch = deltaPitch;

    this.divisorX = SlothMath.gcd(deltaYawAbs, lastXRot);
    if (deltaYawAbs > 0 && deltaYawAbs < 5 && divisorX > SlothMath.MINIMUM_DIVISOR) {
      this.xRotMode.add(divisorX);
      this.lastXRot = deltaYawAbs;
    }

    this.divisorY = SlothMath.gcd(deltaPitchAbs, lastYRot);
    if (deltaPitchAbs > 0 && deltaPitchAbs < 5 && divisorY > SlothMath.MINIMUM_DIVISOR) {
      this.yRotMode.add(divisorY);
      this.lastYRot = deltaPitchAbs;
    }

    if (this.xRotMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
      this.xRotMode.updateMode();
      if (this.xRotMode.getModeCount() > SIGNIFICANT_SAMPLES_THRESHOLD) {
        this.modeX = this.xRotMode.getModeValue();
        this.sensitivityX = convertToSensitivity(this.modeX);
      }
    }

    if (this.yRotMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
      this.yRotMode.updateMode();
      if (this.yRotMode.getModeCount() > SIGNIFICANT_SAMPLES_THRESHOLD) {
        this.modeY = this.yRotMode.getModeValue();
        this.sensitivityY = convertToSensitivity(this.modeY);
      }
    }

    if (modeX > 0) this.deltaDotsX = deltaYawAbs / modeX;
    if (modeY > 0) this.deltaDotsY = deltaPitchAbs / modeY;
  }
}
