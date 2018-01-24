package js.scalameter

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import js.{JsCompletionStage, NashornEngine}

import scala.compat.java8.FutureConverters

class FetchHttpClient(val system: ActorSystem) extends HttpClient {
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val bytesAcc = new AtomicLong(0)

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

  override def send(numRequests: Int, host: String, port: Int, path: String) = {
    val successLatch = new CountDownLatch(numRequests)
    val url = s"http://$host:$port/$path"

    val jcs = fetch(url)

    FutureConverters.toScala(jcs.cs).foreach { body =>
      bytesAcc.addAndGet(body.length)
      successLatch.countDown()
    }
  }

}