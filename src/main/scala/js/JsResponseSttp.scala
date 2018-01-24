package js

import java.util
import java.util.concurrent.CompletableFuture

import com.fasterxml.jackson.databind.ObjectMapper
import com.softwaremill.sttp.Response
import jdk.nashorn.api.scripting.JSObject

class JsResponseSttp(val response: Response[String], val headers: JSObject) extends JsResponse {

  val status: Int = response.code
  val statusText: String = response.statusText
  val ok: Boolean = response.isSuccess

  def text(): JsCompletionStage[String] = {
    new JsCompletionStage(CompletableFuture.completedFuture(response.unsafeBody))
  }

  def json(): JsCompletionStage[util.HashMap[_, _]] = {
    def parse(s: String) = new ObjectMapper().readValue(s, classOf[util.HashMap[_, _]])
    text().`then`(parse _)
  }
}
