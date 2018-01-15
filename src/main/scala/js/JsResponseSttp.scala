package js

import java.util
import java.util.concurrent.CompletableFuture

import com.fasterxml.jackson.databind.ObjectMapper
import com.softwaremill.sttp.Response
import scala.collection.JavaConverters._

class JsResponseSttp(val response: Response[String]) extends JsResponse {

  val status: Int = response.code
  val statusText: String = response.statusText
  val ok: Boolean = response.isSuccess

  val headers = NashornEngine.instance.newObject("Headers", response.headers.toMap.asJava)

  def text(): JsCompletionStage[String] = {
    new JsCompletionStage(CompletableFuture.completedFuture(response.unsafeBody))
  }

  def json(): JsCompletionStage[util.HashMap[_, _]] = {
    def parse(s: String) = new ObjectMapper().readValue(s, classOf[util.HashMap[_, _]])
    text().`then`(parse _)
  }
}
