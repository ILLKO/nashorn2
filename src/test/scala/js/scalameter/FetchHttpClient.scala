package js.scalameter

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import js.{FetchOnAkka, FetchOnSttp, JsCompletionStage, NashornEngine}

import scala.compat.java8.FutureConverters

class FetchHttpClient(implicit system: ActorSystem) extends HttpClient {
  implicit val executionContext = system.dispatcher

  val ne = getNashornEngine

  def getNashornEngine = {
    NashornEngine.init(new FetchOnAkka(system, _))
  }

  def fetch(url: String): JsCompletionStage[String] = {
    val js =
      s"""
         |fetch("$url").then(function(response) {
         |  return response.text();
         |})
       """.stripMargin

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