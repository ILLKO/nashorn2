package js.scalameter.ticked

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import js.scalameter.{HttpClient, HttpClientTicked}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AkkaHttpClientTicked extends HttpClientTicked {
  implicit val system = ActorSystem("client")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  override def send(numRequests: Int, interval: FiniteDuration, host: String, port: Int, path: String) = {
    val failuresAcc = new AtomicLong(0)
    val bytesAcc = new AtomicLong(0)

    import java.util.concurrent.CountDownLatch
    val successLatch = new CountDownLatch(numRequests)

    val ticks = Source.tick(interval, interval, (HttpRequest(uri = path, method = HttpMethods.GET), 1))

    val connFlow = Http(system).cachedHostConnectionPool[Int](host = host, port = port)

    val sink = Sink.foreach[(util.Try[HttpResponse], Int)] {
      case (Success(r), _) =>
        val entityFuture = r.entity.toStrict(10 seconds)
        entityFuture.foreach { entity =>
          bytesAcc.addAndGet(entity.contentLength)
//          println("success, to go: " +successLatch.getCount)

          successLatch.countDown()
        }

      case (Failure(ex), _) =>
        val failures = failuresAcc.incrementAndGet()
  //      println("failures: " + failures)
    }

    ticks.takeWhile(_ => successLatch.getCount > 0).via(connFlow).to(sink).run
    successLatch.await()
  }

}