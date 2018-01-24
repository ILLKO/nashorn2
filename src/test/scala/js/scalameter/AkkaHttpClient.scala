package js.scalameter

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.duration._

class AkkaHttpClient(val system: ActorSystem) extends HttpClient {

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val bytesAcc = new AtomicLong(0)

  override def send(numRequests: Int, host: String, port: Int, path: String) = {
    val successLatch = new CountDownLatch(numRequests)

    val url = s"http://$host:$port/$path"
    val future = Http().singleRequest(HttpRequest(uri = url))

    future.onSuccess { case r =>
      val entityFuture = r.entity.toStrict(10 seconds)
      entityFuture.foreach { entity =>
        bytesAcc.addAndGet(entity.contentLength)
        successLatch.countDown()
      }
    }
  }
}