package js

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.compat.java8.FutureConverters

object FetchOnAkka {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  def fetch(url: String/*, options: Map[String, AnyRef] = Map.empty*/): JsCompletionStage[JsResponse] = {

    //    val options: Map[String, AnyRef] = Map.empty
    //    val method = options.getOrElse("method", "GET")
    //    val headers = options.getOrElse("headers", Map.empty)
    //    val body = options.get("body")

    val f = Http().singleRequest(HttpRequest(uri = url)).map(r => new JsResponse(r))
    new JsCompletionStage(FutureConverters.toJava(f))
  }

  //  def convertHeaders() = {
  //
  //  }

  def main(args: Array[String]): Unit = {
    fetch("http://akka.io")
  }
}
