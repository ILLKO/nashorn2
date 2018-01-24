package js.scalameter.ticked

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import js.scalameter.{HttpClient, HttpClientTicked}
import js.{JsCompletionStage, NashornEngine}

import scala.compat.java8.FutureConverters
import scala.concurrent.duration._

class FetchHttpClientTicked extends HttpClientTicked {
  implicit val system = ActorSystem("client")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val failuresAcc = new AtomicLong(0)
  val bytesAcc = new AtomicLong(0)
  val successLatch = new CountDownLatch(300)

  def getNashornEngine: NashornEngine = {
    val ne = NashornEngine.instance
    ne.evalResource("/js/fetch.js")
    ne
  }

  def fetch(url: String): JsCompletionStage[String] = {
    val js =
      s"""
         |fetch("$url").then(function(response) {
         |  return response.text();
         |})
       """.stripMargin

    val ne = getNashornEngine
    ne.evalString(js).asInstanceOf[JsCompletionStage[String]]
  }

  override def send(numRequests: Int, interval: FiniteDuration, host: String, port: Int, path: String) = {

    val url = s"http://$host:$port$path"

    val ticks = Source.tick(3 millis, 3 millis, 1)

    val fetchSource = Source.fromIterator(() => Iterator.continually(fetch(url)))

    val sink = Sink.foreach[JsCompletionStage[String]](consume)

    ticks.zip(fetchSource).map(_._2).takeWhile(_ => successLatch.getCount > 0).runWith(sink)
    successLatch.await()
  }

  private def consume(jcs: JsCompletionStage[String]) = {
    FutureConverters.toScala(jcs.cs).foreach { body =>
      bytesAcc.addAndGet(body.length)
      successLatch.countDown()
      println(successLatch.getCount)
    }
  }
}