package js.bench

import java.util.concurrent.atomic.AtomicLong
import java.util.function.LongUnaryOperator

trait RateLimiter {
  def isRequestAllowed: Boolean

  def ratePerSecond: Int

  def totalAllowed: Long

  def waitTime: Long
}

object RateLimiter {
  def apply(ratePerSecond: Int, descr: String = "")(implicit clock: TimeService): RateLimiter =
    new RateLimiterInterpolated(ratePerSecond, descr)
}

class RateLimiterInterpolated(val ratePerSecond: Int, val descr: String, val decrementStep: Int = 1)
                             (implicit val clock: TimeService) extends RateLimiter {

  import Config._

  @volatile private var lastTime = clock.now

  clock.schedule(() => {
    tryIncreaseAllowedRequests()
    ()
  })

  private val ratePerInterval = ratePerSecond.toDouble / intervalsPerSecond

  private val allowedRequests = new AtomicLong(ratePerSecond)

  // required to take into account fractional ratePerInterval values
  private var allowedRequestsRoundingAccum = 0.0

  // counts all allowed by this limiter requests
  private val totalAllowedCounter = new AtomicLong(0)

  object DecreaseAllowedRequests extends LongUnaryOperator {
    override def applyAsLong(operand: Long): Long =
      Math.max(operand - decrementStep, 0)
  }

  class IncreaseAllowedRequests(increase: Long) extends LongUnaryOperator {
    override def applyAsLong(operand: Long): Long =
      if (operand >= ratePerSecond) ratePerSecond else operand + increase
  }

  def isRequestAllowed: Boolean = {
    val allowed = allowedRequests.getAndUpdate(DecreaseAllowedRequests) > 0 ||
      // We cannot rely only on akka scheduler to call tryIncreaseAllowedRequests,
      // as it can lag for seconds
      (tryIncreaseAllowedRequests() && allowedRequests.getAndUpdate(DecreaseAllowedRequests) > 0)

    if (allowed) {
      totalAllowedCounter.incrementAndGet()
    }
    allowed
  }

  def tryIncreaseAllowedRequests(): Boolean = {
    val now = clock.now
    if (now - lastTime >= intervalInNanoSeconds) {
      synchronized {
        val elapsedIntervals = (now - lastTime) / intervalInNanoSeconds
        if (elapsedIntervals > 0) {

          lastTime = lastTime + elapsedIntervals * intervalInNanoSeconds

          val increase = Math.min(
            (elapsedIntervals * ratePerInterval + allowedRequestsRoundingAccum).round,
            ratePerSecond - allowedRequests.get
          )
          if (increase > 0) {
            val increaseUpdater = new IncreaseAllowedRequests(increase)

            allowedRequests.updateAndGet(increaseUpdater)
            allowedRequestsRoundingAccum = 0.0

            return true
          } else {
            allowedRequestsRoundingAccum += elapsedIntervals * ratePerInterval
          }
        }
      }
    }
    false
  }

  override def totalAllowed: Long = totalAllowedCounter.get()

  override def waitTime: Long =
    (lastTime + intervalInNanoSeconds - clock.now) / (1000 * 1000)
}
