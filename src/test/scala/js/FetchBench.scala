package js

import java.util.Date
import java.util.concurrent.atomic.AtomicLong

import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.{Request, sttp}
import org.slf4j.{Logger, LoggerFactory}

object FetchBench {
  val Port = 8080
  val Host = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))
  val logger: Logger = LoggerFactory.getLogger(FetchBench.getClass)


  def startWireMock() = {
    wireMockServer.start()
    WireMock.configureFor(Host, Port)
  }

  def stopWireMock() = {
    wireMockServer.stop()
  }

  def bench() = {
    startWireMock()

    val body = "x" //* 4000

    val path = "/my/resource"
    val httpOk = StatusCodes.OK

    val url = s"http://$Host:$Port$path"

    stubResponse(path, httpOk.intValue, body)


    val received: AtomicLong = new AtomicLong(0)
    val bytes: AtomicLong = new AtomicLong(0)

    val start = System.nanoTime()
    println(new Date())
    var elapsed = 0
    var sent = 0

    import com.softwaremill.sttp._
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val sttpBackend = AkkaHttpBackend() //AsyncHttpClientFutureBackend()
    //AkkaHttpBackend()
    val request = sttp.get(uri"$url")

    while (elapsed < 120) {
      request.send().foreach { r =>
        val body = r.unsafeBody
        val cv = received.addAndGet(1)
        val bb = bytes.addAndGet(body.length * 2)
        logger.info(s"Received: requests: $cv, bytes: $bb")
      }
      sent += 1
      val now = System.nanoTime()
      val delta = now - start - (elapsed + 1) * 1000 * 1000 * 1000
      if (delta > 0) {
        elapsed += 1
        logger.info(s"Elapsed: $elapsed, sent: $sent, received: ${received.get()}. Start: $start, Now $now, delta: $delta")
        Thread.sleep(100)
      }
    }
    println(new Date())

    stopWireMock()
  }

  def benchAkka() = {
    import akka.actor.ActorSystem
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.model._
    import akka.stream.ActorMaterializer

    import scala.compat.java8.FutureConverters
    import scala.concurrent.duration._

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    startWireMock()

    val body = "x" //* 4000

    val path = "/my/resource"
    val httpOk = StatusCodes.OK

    val url = // "https://uk.wikipedia.org"
     s"http://$Host:$Port$path"

    stubResponse(path, httpOk.intValue, body)


    val received: AtomicLong = new AtomicLong(0)
    val bytes: AtomicLong = new AtomicLong(0)

    val start = System.nanoTime()
    println(new Date())
    var elapsed = 0
    var sent = 0
    val timeout = 1 second
    val rps = 2000

    while (elapsed < 120) {
      val f = Http().singleRequest(HttpRequest(uri = url)).map{r =>
        val bodyF = r.entity.toStrict(timeout).map(_.data.utf8String)
        bodyF.map { body =>
          val cv = received.addAndGet(1)
          val bb = bytes.addAndGet(body.length * 2)
          logger.info(s"Received: requests: $cv, bytes: $bb")
        }
      }
      sent += 1
      val now = System.nanoTime()
      val delta = now - start - (elapsed + 1) * 1000 * 1000 * 1000
      if (delta > 0) {
        elapsed += 1
        logger.info(s"Elapsed: $elapsed, sent: $sent, received: ${received.get()}. Start: $start, Now $now, delta: $delta")
        Thread.sleep(100)
      }
    }
    println(new Date())

    stopWireMock()

  }


  //  def getSttp(request: Request[String, Nothing], count: AtomicLong, bytes: AtomicLong)() = {
  //
  //  }

  private def stubResponse(path: String, code: Int, body: String) = {
    stubFor(get(urlEqualTo(path))
      .willReturn(
        aResponse()
          .withStatus(code)
          .withBody(body)))
  }

  def main(args: Array[String]): Unit = {
        benchAkka()
//    startWireMock()
//
//    val body = "x" //* 4000
//
//    val path = "/my/resource"
//    val httpOk = StatusCodes.OK
//
//    val url = s"http://$Host:$Port$path"
//
//    stubResponse(path, httpOk.intValue, body)

  }
}
