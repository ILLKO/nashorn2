package js.bench

import akka.actor.ActorSystem

trait TimeService {
  def now: Long

  def schedule(f: () => Unit)
}

class TimerImpl(system: ActorSystem) extends TimeService {

  import system.dispatcher
  import scala.concurrent.duration._

  override def now = System.nanoTime

  override def schedule(f: () => Unit) =
    system.scheduler.schedule(100.millis, 100.millis)(f())

}