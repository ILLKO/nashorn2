package js.scalameter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

class AkkaHttpServer {
  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  var binding: Http.ServerBinding = _

  private def serve(pathToBody: Map[String, String], host: String, port: Int): Unit = {
    val route = path(pathToBody) { body =>
      get {
        complete(HttpEntity(body))
      }
    }
    val bindingFuture = Http().bindAndHandle(route, host, port)
    binding = Await.result(bindingFuture, 2 seconds)
  }

  def terminate() = {
    binding.unbind().onComplete(_ => system.terminate())
  }
}

object AkkaHttpServer {

  def serve(pathToBody: Map[String, String], host: String, port: Int): AkkaHttpServer = {
    val server = new AkkaHttpServer
    server.serve(pathToBody, host, port)
    server
  }
}
