package js

import akka.actor.ActorSystem
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters

class FetchOnSttp(val system: ActorSystem, val engine: NashornEngine) extends Fetch[JsResponseSttp] {

  import com.softwaremill.sttp.Method._
  import system.dispatcher

  implicit val sttpBackend = AkkaHttpBackend.usingActorSystem(system)(system.dispatcher)

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
            requestObj: java.util.Map[String, AnyRef]): JsCompletionStage[JsResponseSttp] = {
    val request = sttp
      .copy[Id, String, Nothing](uri = uri"$url", method = methodsMaps(method))
      .headers(headers.asScala.toMap)

    val withBody = Option(requestObj.get("body")).fold(request) {
      case bodyString: String => request.body(bodyString)
      case _ => request
    }

    val f = withBody.send().map { response =>
      val headers = engine.newObject("Headers", response.headers.toMap.asJava)
      new JsResponseSttp(response, headers)
    }
    new JsCompletionStage(FutureConverters.toJava(f))
  }

}
