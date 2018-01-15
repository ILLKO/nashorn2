package js

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import com.softwaremill.sttp._

import scala.compat.java8.FutureConverters

trait Fetch {

  val system: ActorSystem

  def fetch(method: String, url: String,
            headers: java.util.Map[String, String] = new java.util.HashMap,
            requestObj: java.util.Map[String, AnyRef] = new java.util.HashMap): JsCompletionStage[JsResponse]
}

object FetchOnSttp extends Fetch {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

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

  override def fetch(method: String, url: String,
                     headers: java.util.Map[String, String] = new java.util.HashMap,
                     requestObj: java.util.Map[String, AnyRef]): JsCompletionStage[JsResponse] = {
//    val request = sttp
//      .copy[Id, String, Nothing](uri = uri"$url", method = methodsMaps(method))
//      .headers(headers.asScala.toMap)
//
//    val withBody = Option(requestObj.get("body")).fold(request) {
//      case bodyString: String => request.body(bodyString)
//      case _ => request
//    }

    val f = Http().singleRequest(HttpRequest(uri = url)).map(r => new JsResponse(r))
    new JsCompletionStage(FutureConverters.toJava(f))
  }
}
