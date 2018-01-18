package js.scalameter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.stream.ActorMaterializer

import scala.concurrent.Future

class AkkaHttpServer {
  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  def serve(body: String, host: String, port: Int, p: String): Future[Http.ServerBinding] = {
    val route = path(p) {
      get {
        complete(HttpEntity(body))
      }
    }
    Http().bindAndHandle(route, host, port)
  }

  def terminate(binding: Http.ServerBinding) = {
    binding.unbind().onComplete(_ => system.terminate())
  }
}
