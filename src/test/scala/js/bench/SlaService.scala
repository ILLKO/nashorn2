package js.bench

import scala.concurrent.Future
import scala.util.Try

case class Sla(user: String, rps: Int)

trait SlaService {
  def getSlaByToken(token: String): Future[Sla]
}

class MapBasedSlaService(val map: Map[String, Sla]) extends SlaService {

  override def getSlaByToken(token: String): Future[Sla] =
    Future.fromTry(Try(map(token)))
}

