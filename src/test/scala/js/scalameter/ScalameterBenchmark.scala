package js.scalameter

import org.scalameter.api._

import scala.concurrent.Await
import scala.concurrent.duration._
import org.scalameter.picklers.Implicits._

import scala.collection.immutable

object ScalameterBenchmark extends Bench.LocalTime {

  override lazy val reporter = new LoggingReporter[Double]

  val minResponseSize = 200000
  val maxResponseSize = 600000
  val responseSizeStep = 200000

  //    val responseSize = Gen.range("size")(minResponseSize, maxResponseSize, responseSizeStep)
  //    val requestsPerSecond = Gen.range("size")(100, 2000, 100)
  val responseSize = Gen.single("size")(100000)
  val requestsPerSecond = Gen.single("rps")(200)

  val pairs = Gen.crossProduct(responseSize, requestsPerSecond)

  val Port = 8080
  val Host = "localhost"
  val interval = 1

  val pathMap: Map[String, String] = (minResponseSize to maxResponseSize by responseSizeStep).map { size =>
    size.toString -> size.toString.head.toString * size
  }.toMap

  val server = new AkkaHttpServer()
  val bindingFuture = server.serve(pathMap, Host, Port)

  val client = new AkkaHttpClient()

  measure method "akka http" in {
    using(pairs) curve ("responseSize") config(
      exec.benchRuns -> 2,
      //      exec.independentSamples -> 2,
      exec.minWarmupRuns -> 1,
      exec.maxWarmupRuns -> 1
    ) in { case (size, rps) =>
      val tickInteval = 1000 * 1000 / rps
      client.send(rps * interval, tickInteval micros, Host, Port, size.toString)
    }
  }

}