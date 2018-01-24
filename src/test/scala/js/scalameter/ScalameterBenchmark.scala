package js.scalameter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.scalameter.api._
import org.scalameter.picklers.Implicits._

import scala.concurrent.Future

object ScalameterBenchmark extends Bench.LocalTime {

  override lazy val reporter = new LoggingReporter[Double]

  val minResponseSize = 200000
  val maxResponseSize = 600000
  val responseSizeStep = 200000
  //  val responseSize = Gen.range("size")(minResponseSize, maxResponseSize, responseSizeStep)
  //  val requestsPerSecond = Gen.range("requests")(100, 300, 100)

  val responseSize = Gen.single("size")(maxResponseSize)
  val requestsPerSecond = Gen.single("rps")(300)

  val pairs = Gen.crossProduct(responseSize, requestsPerSecond)

  val Port = 8080
  val Host = "localhost"
  val interval = 1

  val pathMap: Map[String, String] = (minResponseSize to maxResponseSize by responseSizeStep).map { size =>
    size.toString -> size.toString.head.toString * size
  }.toMap

  var server: AkkaHttpServer = _
  var clientSystem: ActorSystem = _
  var client: HttpClient = _

  def startServer() = {
    server = AkkaHttpServer.serve(pathMap, Host, Port)
  }

  def stopServer() = {
    server.terminate()
  }

  def beforeAfter[T](using: Using[T], newClient: ActorSystem => HttpClient): ScalameterBenchmark.Using[T] = {
    using beforeTests {
      clientSystem = ActorSystem("client")
      client = newClient(clientSystem)
      startServer()
    } afterTests {
      stopServer()
      clientSystem.terminate()
    }
  }

  measure method "akka http" in {
    beforeAfter(using(pairs), (s: ActorSystem) => new AkkaHttpClient()(s)) in {
      case (size, rps) =>
        client.send(rps * interval, Host, Port, size.toString)
    }
  }

  measure method "fetch http" in {
    val system = ActorSystem("client")
    val fetchClient = new FetchHttpClient()(system)

    beforeAfter(using(pairs), (s: ActorSystem) => new FetchHttpClient()(s)) in {
      case (size, rps) =>
        client.send(rps * interval, Host, Port, size.toString)
    }
  }
}