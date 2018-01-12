package js

import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend

import scala.compat.java8.FutureConverters
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

object FetchOnSttp {

  import com.softwaremill.sttp.Method._

  val methodsMaps: Map[String, Method] = Seq(
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    PATCH,
    CONNECT,
    TRACE).map { x => x.m -> x }.toMap

  def fetch(method: String, url: String,
            headers: java.util.Map[String, String] = new java.util.HashMap,
            requestObj: java.util.Map[String, AnyRef]): JsCompletionStage[JsResponse] = {
    val request = sttp
      .copy[Id, String, Nothing](uri = uri"$url", method = methodsMaps(method))
      .headers(headers.asScala.toMap)

    val withBody = Option(requestObj.get("body")).fold(request) {
      case bodyString: String => request.body(bodyString)
      case _ => request
    }

    implicit val sttpBackend = AkkaHttpBackend()

    val f = withBody.send().map(r => new JsResponse(r))
    new JsCompletionStage(FutureConverters.toJava(f))
  }
}
