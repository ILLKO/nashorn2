package task.spray

import akka.actor.ActorSystem
import akka.util.Timeout
import js.bench._
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http._
import spray.routing._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class HttpServerSpray extends SimpleRoutingApp {

  implicit val system = ActorSystem("server")
  DeadLetterListener.subscribe(system)

  def start[T](service: Service[T], throttlingService: ThrottlingService, serviceEc: ExecutionContext) = {

    implicit val bindingTimeout: Timeout = 2.seconds

    startServer(interface = "localhost", port = 8080) {

      def throttle(service: Service[T]) = {
        parameter("token" ?) { token =>
          if (throttlingService.isRequestAllowed(token)) {
            implicit val ec = serviceEc
            onComplete(service.run.map(_.toString)) {
              case Success(result) => complete(result)
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          } else {
            respondWithStatus(TooManyRequests) {
              complete(TooManyRequests.reason)
            }
          }
        }
      }

      (pathSingleSlash & get) {
        complete(index)
      } ~
        (path("ping") & get) {
          complete("pong")
        } ~
        (path("service") & get) {
          throttle(service)
        }
    }
  }

  lazy val index = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h1>Say hello to spray!</h1>
          <p>Defined resources:</p>
          <ul>
            <li>
              <a href="/service">/service</a>
            </li>
          </ul>
        </body>
      </html>.toString()
    )
  )
}

object HttpServerSpray extends App {
  private val spray = new HttpServerSpray()
  spray.start(new NumService, UnlimitedThrottlingService, spray.system.dispatcher)
}