package js

import java.util
import java.util.concurrent.CompletableFuture

import com.fasterxml.jackson.databind.ObjectMapper
import com.softwaremill.sttp.Response

class JsResponse(val response: Response[String]) {

  val status: Int = response.code
//  val statusText: String =
  val ok: Boolean = response.isSuccess

  val headers = NashornEngine.instance.newObject("Headers")

  def text(): JsCompletionStage[String] = {
    new JsCompletionStage(CompletableFuture.completedFuture(response.unsafeBody))
  }

  def json(): JsCompletionStage[util.HashMap[_, _]] = {
    def parse(s: String) = new ObjectMapper().readValue(s, classOf[util.HashMap[_, _]])
    text().`then`(parse _)
  }
}