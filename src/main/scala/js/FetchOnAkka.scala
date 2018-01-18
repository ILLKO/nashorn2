package js

import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.model.HttpRequest
import akka.stream.ActorMaterializer

trait Fetch[T <: JsResponse] {

  val system: ActorSystem

  def fetch(method: String, url: String,
            headers: java.util.Map[String, String] = new java.util.HashMap,
            requestObj: java.util.Map[String, AnyRef] = new java.util.HashMap): JsCompletionStage[T]
}

object FetchOnAkka extends Fetch[JsResponseAkka] {

  implicit val system = ActorSystem("client")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  override def fetch(method: String, url: String,
                     headers: java.util.Map[String, String] = new java.util.HashMap,
                     requestObj: java.util.Map[String, AnyRef]): JsCompletionStage[JsResponseAkka] = {
    val cs = Http.get(system).singleRequest(HttpRequest.create(url))
    new JsCompletionStage(cs).`then`(r => new JsResponseAkka(r))
  }
}
