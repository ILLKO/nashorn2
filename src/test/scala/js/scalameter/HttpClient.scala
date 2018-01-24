package js.scalameter

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {

  val bytesAcc = new AtomicLong(0)
  val failures = new AtomicLong(0)

  def send(url: String, successLatch: CountDownLatch): Unit

  def send(numRequests: Int, host: String, port: Int, path: String): Unit = {
    val successLatch = new CountDownLatch(numRequests)
    val url = s"http://$host:$port/$path"

    for (_ <- 1 to numRequests) {
      send(url, successLatch)
    }

    successLatch.await()
  }

  def resendOnFailure[T](future: Future[T], url: String, successLatch: CountDownLatch)(implicit ec: ExecutionContext) = {
    future.onFailure { case _ =>
      failures.incrementAndGet()
      send(url, successLatch)
    }
  }

}

