package js.scalameter

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.duration._

class AkkaHttpClient(implicit system: ActorSystem) extends HttpClient {

  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override def send(url: String, successLatch: CountDownLatch) = {
    val future = Http().singleRequest(HttpRequest(uri = url))
    future.onSuccess { case r =>
      val entityFuture = r.entity.toStrict(10 seconds)
      entityFuture.foreach { entity =>
        bytesAcc.addAndGet(entity.contentLength)
        successLatch.countDown()
      }
      resendOnFailure(future, url, successLatch)
    }
    resendOnFailure(future, url, successLatch)
  }

}