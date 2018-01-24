package js

trait Fetch[T <: JsResponse] {

  def fetch(method: String, url: String,
            headers: java.util.Map[String, String] = new java.util.HashMap,
            requestObj: java.util.Map[String, AnyRef] = new java.util.HashMap): JsCompletionStage[T]
}