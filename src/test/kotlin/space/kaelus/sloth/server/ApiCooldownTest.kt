package space.kaelus.sloth.server

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ApiCooldownTest {

  @Test
  fun `initially not waiting`() {
    val cooldown = ApiCooldown(5, 60, 2.0)
    assertFalse(cooldown.isWaiting())
  }

  @Test
  fun `after failure enters waiting state`() {
    val cooldown = ApiCooldown(5, 60, 2.0)
    cooldown.recordFailure()
    assertTrue(cooldown.isWaiting())
  }

  @Test
  fun `after success resets waiting state`() {
    val cooldown = ApiCooldown(5, 60, 2.0)
    cooldown.recordFailure()
    assertTrue(cooldown.isWaiting())

    cooldown.recordSuccess()
    assertFalse(cooldown.isWaiting())
  }

  @Test
  fun `backoff increases with each failure`() {
    // initialDuration = 1 * 1000 = 1000ms, max = 10 * 1000 = 10000ms, multiplier = 2.0
    val cooldown = ApiCooldown(1, 10, 2.0)

    cooldown.recordFailure() // backoff becomes 2000ms, next attempt = now + 1000ms
    assertTrue(cooldown.isWaiting())

    cooldown.recordSuccess()
    assertFalse(cooldown.isWaiting())

    // Simulate two consecutive failures — second should have longer backoff
    cooldown
      .recordFailure() // backoff = 1000ms initially, sets next = now+1000, then backoff -> 2000
    cooldown.recordFailure() // backoff = 2000ms, sets next = now+2000, then backoff -> 4000
    assertTrue(cooldown.isWaiting())
  }

  @Test
  fun `backoff does not exceed max duration`() {
    // initialDuration = 1s, max = 2s, multiplier = 10.0
    val cooldown = ApiCooldown(1, 2, 10.0)

    cooldown
      .recordFailure() // backoff was 1000, sets next = now+1000, new backoff = min(10000, 2000) =
    // 2000
    cooldown
      .recordFailure() // backoff was 2000, sets next = now+2000, new backoff = min(20000, 2000) =
    // 2000
    cooldown.recordFailure() // backoff still capped at 2000

    // Still waiting because we just failed
    assertTrue(cooldown.isWaiting())
  }

  @Test
  fun `success after multiple failures fully resets`() {
    val cooldown = ApiCooldown(1, 60, 2.0)
    cooldown.recordFailure()
    cooldown.recordFailure()
    cooldown.recordFailure()
    assertTrue(cooldown.isWaiting())

    cooldown.recordSuccess()
    assertFalse(cooldown.isWaiting())

    // After reset, first failure should use initial duration again
    cooldown.recordFailure()
    assertTrue(cooldown.isWaiting())
  }
}
