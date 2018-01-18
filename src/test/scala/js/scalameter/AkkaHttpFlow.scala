package js.scalameter

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration._

class AkkaHttpFlow {
  implicit val system = ActorSystem("client")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def send(numRequests: Int, interval: FiniteDuration, host: String, port: Int, path: String) = {
    val failuresAcc = new AtomicLong(0)
    val bytesAcc = new AtomicLong(0)

    import java.util.concurrent.CountDownLatch
    val successLatch = new CountDownLatch(numRequests)

    val ticks = Source.tick(interval, interval, (HttpRequest(uri = path, method = HttpMethods.GET), 1))

    val connFlow = Http(system).cachedHostConnectionPool[Int](host = host, port = port)

    val sink = Sink.foreach[(util.Try[HttpResponse], Int)] {
      case (util.Success(r), _) =>
        val entityFuture = r.entity.toStrict(10 seconds)
        entityFuture.foreach { entity =>
          bytesAcc.addAndGet(entity.contentLength)
          successLatch.countDown()
        }

      case (util.Failure(ex), _) =>
        failuresAcc.incrementAndGet()
    }

    ticks.takeWhile(_ => successLatch.getCount > 0).via(connFlow).to(sink).run
    successLatch.await()
  }

}

object AkkaHttpFlow {

  val Host = "localhost"
  val Port = 8080
  val Path = "/"

  def bench(numRequests: Int, responseSize: Int) = {
    val server = new AkkaHttpServer()
    import server.executionContext

    val bf = server.serve("x" * responseSize, Host, Port, Path)

    bf.map { b =>
      val flow = new AkkaHttpFlow()
      flow.send(numRequests, 3 millis, Host, Port, Path)
      server.terminate(b)
    }
  }

  def main(args: Array[String]): Unit = {
    bench(300, 600 * 1000)
  }
}
