package js

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import js.FetchOnAkka.system

import scala.compat.java8.FutureConverters
import scala.concurrent.duration._

class JsResponse(val response: HttpResponse) {

  implicit val materializer = FetchOnAkka.materializer
  implicit val executionContext = system.dispatcher

  val timeout = 1 second

  val status: Int = response.status.intValue()
  val statusText: String = response.status.reason()
  val ok: Boolean = response.status == StatusCodes.OK

  def text(): JsCompletionStage[String] = {
    val f = response.entity.toStrict(timeout).map(_.data.utf8String)
    new JsCompletionStage(FutureConverters.toJava(f))
  }
}
