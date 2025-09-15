package space.kaelus.sloth.utils.update;

import lombok.Getter;
import lombok.Setter;
import space.kaelus.sloth.utils.data.HeadRotation;

@Getter
@Setter
public final class RotationUpdate {
  private HeadRotation from;
  private HeadRotation to;
  private float deltaYaw;
  private float deltaPitch;

  public RotationUpdate(HeadRotation from, HeadRotation to, float deltaYaw, float deltaPitch) {
    this.from = from;
    this.to = to;
    this.deltaYaw = deltaYaw;
    this.deltaPitch = deltaPitch;
  }
}
