package js

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend

import scala.compat.java8.FutureConverters
import scala.concurrent.ExecutionContext.Implicits.global

object FetchOnSttp {

  def fetch(url: String): JsCompletionStage[JsResponse] = {
    val request = sttp.get(uri"$url")

    implicit val sttpBackend = AkkaHttpBackend()

    val f = request.send().map(r => new JsResponse(r))
    new JsCompletionStage(FutureConverters.toJava(f))
  }
}
