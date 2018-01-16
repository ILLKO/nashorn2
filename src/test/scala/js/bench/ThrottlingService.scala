package js.bench

import com.github.blemale.scaffeine.{Cache, Scaffeine}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait ThrottlingService {
  val graceRps: Int // configurable
  val slaService: SlaService // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token: Option[String]): Boolean
}

object Config {
  val intervalsPerSecond: Int = 10
  val intervalInNanoSeconds: Long = Math.pow(10, 9).toLong / intervalsPerSecond
}

class ThrottlingServiceImpl(val slaService: SlaService, val graceRps: Int)
                           (implicit val timeService: TimeService) extends ThrottlingService {

  private val graceLimiter = RateLimiter(graceRps, "grace")

  private val tokenToUser: Cache[String, String] = Scaffeine().expireAfterWrite(1.hour).build()

  private val userToLimiter: Cache[String, RateLimiter] = Scaffeine().expireAfterWrite(1.hour).build()

  def getLimiter(maybeToken: Option[String]): RateLimiter = {

    def fetchSla(token: String) = {
      slaService.getSlaByToken(token).map { sla =>
        tokenToUser.put(token, sla.user)
        userToLimiter.get(sla.user, _ => RateLimiter(sla.rps, sla.user))
      }
    }

    maybeToken.flatMap { token =>
      val user = tokenToUser.get(token, _ => {
        fetchSla(token)
        ""
      })

      userToLimiter.getIfPresent(user)
    }.getOrElse(graceLimiter)
  }

  override def isRequestAllowed(token: Option[String]): Boolean = {
    getLimiter(token).isRequestAllowed
  }
}

object ThrottlingService {
  def apply(slaService: SlaService, graceRps: Int = 10)
           (implicit timeService: TimeService) =
    new ThrottlingServiceImpl(slaService, graceRps)

}

object UnlimitedThrottlingService extends ThrottlingService {
  override val graceRps: Int = 0

  override val slaService: SlaService = new MapBasedSlaService(Map.empty)

  override def isRequestAllowed(token: Option[String]): Boolean = true
}