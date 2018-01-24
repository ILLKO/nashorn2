package js.scalameter

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import js.{JsCompletionStage, NashornEngine}

import scala.compat.java8.FutureConverters

class FetchHttpClient(implicit system: ActorSystem) extends HttpClient {
  implicit val executionContext = system.dispatcher

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

  override def send(url: String, successLatch: CountDownLatch) = {
    val jcs = fetch(url)

    val future = FutureConverters.toScala(jcs.cs)
    future.foreach { body =>
      bytesAcc.addAndGet(body.length)
      successLatch.countDown()
    }
    resendOnFailure(future, url, successLatch)
  }

}