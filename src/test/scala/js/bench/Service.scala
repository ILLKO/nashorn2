package js.bench

import java.util.concurrent.ThreadLocalRandom

import scala.concurrent.{ExecutionContext, Future}

trait Service[T] {
  def run()(implicit ec: ExecutionContext): Future[T]
}

class NumService extends Service[String] {
  override def run()(implicit ec: ExecutionContext) = {
    Future.successful(ThreadLocalRandom.current().nextInt().toString)
  }
}

class StringService(size: Int) extends Service[String] {
  val s = "x" * size
  override def run()(implicit ec: ExecutionContext) =
    Future.successful(s)
}

class NumServiceBlocking(latency: Int) extends Service[String] {
  override def run()(implicit ec: ExecutionContext) = {
    Future {
      Thread.sleep(latency)
      ThreadLocalRandom.current().nextInt().toString
    }
  }
}
