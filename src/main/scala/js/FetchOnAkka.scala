package js

import akka.actor.ActorSystem
import akka.http.javadsl.Http
import akka.http.javadsl.model.headers.RawHeader
import akka.http.javadsl.model.{HttpHeader, HttpMethod, HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

class FetchOnAkka(actorSystem: ActorSystem, val engine: NashornEngine) extends Fetch[JsResponseAkka] {

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

  implicit val system = actorSystem
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  override def fetch(method: String, url: String,
                     headers: java.util.Map[String, String] = new java.util.HashMap,
                     requestObj: java.util.Map[String, AnyRef]): JsCompletionStage[JsResponseAkka] = {

    val akkaHeaders = headers.asScala.map {
      case (name, value) => RawHeader.create(name, value).asInstanceOf[HttpHeader]
    }.asJava

    val request = HttpRequest.create(url)
      .withMethod(methodsMaps(method))
      .addHeaders(akkaHeaders)

    val withBody = Option(requestObj.get("body")).fold(request) {
      case bodyString: String => request.withEntity(bodyString)
      case _ => request
    }

    val cs = Http.get(system).singleRequest(withBody)
    new JsCompletionStage(cs).`then` { response =>
      val headers = engine.newObject("Headers", headersFromAkka(response))
      new JsResponseAkka(response, headers, this)
    }
  }

  private def headersFromAkka(hr: HttpResponse): java.util.Map[String, String] = {
    import akka.http.scaladsl.model.headers._
    val ch = `Content-Type`.name -> hr.entity.getContentType.toString
    val clOption = hr.entity.getContentLengthOption
    val cl = if (clOption.isPresent)
      Some(`Content-Length`.name -> clOption.getAsLong.toString)
    else None
    val other = hr.getHeaders.asScala.map(h => (h.name, h.value))
    (ch :: (cl.toList ++ other)).toMap.asJava
  }

}