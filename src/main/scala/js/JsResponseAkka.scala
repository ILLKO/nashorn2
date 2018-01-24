package js

import java.util

import akka.http.javadsl.model.{HttpResponse, StatusCodes}
import com.fasterxml.jackson.databind.ObjectMapper

import scala.concurrent.duration._

trait JsResponse {

  def status: Int
  def statusText: String
  def ok: Boolean

  def text(): JsCompletionStage[String]

}

class JsResponseAkka(val response: HttpResponse, val fetch: FetchOnAkka) extends JsResponse {

  implicit val materializer = fetch.materializer
  implicit val executionContext = fetch.system.dispatcher

  val timeout = 10 seconds

  val status: Int = response.status.intValue()
  val statusText: String = response.status.reason()
  val ok: Boolean = response.status == StatusCodes.OK

  val headers = Map.empty //NashornEngine.instance.newObject("Headers", /*response.headers.toMap*/ Map.empty.asJava)

  def text(): JsCompletionStage[String] = {
    val f = response.entity.toStrict(1000, materializer)
    new JsCompletionStage(f).`then`(e => e.getData.utf8String)
  }

  def json(): JsCompletionStage[util.HashMap[_, _]] = {
    def parse(s: String) = new ObjectMapper().readValue(s, classOf[util.HashMap[_, _]])

    text().`then`(parse _)
  }
}
