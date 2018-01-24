package js

import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.model.{HttpMethod, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer

class FetchOnAkka(actorSystem: ActorSystem) extends Fetch[JsResponseAkka] {

  import HttpMethods._
  val methodsMaps: Map[String, HttpMethod] = Seq(
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    OPTIONS,
    PATCH,
    CONNECT,
    TRACE).map { x => x.name() -> x }.toMap

  implicit val system = ActorSystem("client")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  override def fetch(method: String, url: String,
                     headers: java.util.Map[String, String] = new java.util.HashMap,
                     requestObj: java.util.Map[String, AnyRef]): JsCompletionStage[JsResponseAkka] = {
    val request = HttpRequest.create(url).withMethod(methodsMaps(method))

    val cs = Http.get(system).singleRequest(request)
    new JsCompletionStage(cs).`then`(r => new JsResponseAkka(r, this))
  }
}