package js

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend

import scala.compat.java8.FutureConverters
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

object FetchOnSttp {

  def fetch(url: String, headers: java.util.Map[String, String] = new java.util.HashMap): JsCompletionStage[JsResponse] = {
    val request = sttp.get(uri"$url").headers(headers.asScala.toMap)

    implicit val sttpBackend = AkkaHttpBackend()

    val f = request.send().map(r => new JsResponse(r))
    new JsCompletionStage(FutureConverters.toJava(f))
  }
}
