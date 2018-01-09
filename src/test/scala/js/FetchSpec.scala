package js

import java.util.concurrent.CompletionStage

import akka.http.scaladsl.model.HttpResponse
import org.specs2.specification.BeforeAfterAll

import scala.compat.java8.FutureConverters
import scala.concurrent.Await
import akka.http.scaladsl.model.StatusCodes
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import js.FetchOnAkka._
import org.specs2.mutable.Specification

import scala.concurrent.duration._

trait StubServer extends BeforeAfterAll {
  val Port = 8080
  val Host = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))

  override def beforeAll = {
    wireMockServer.start()
    WireMock.configureFor(Host, Port)
  }

  override def afterAll = wireMockServer.stop()

  def url(path: String) = s"http://$Host:$Port$path"
}

class FetchSpec extends Specification with StubServer {
  val timeout = 1 second
  val body = "Response Body"
  val path = "/my/resource"
  val httpOk = StatusCodes.OK

  "WireMock" should {

    "stub get request" in {

      stubResponse(path, httpOk.intValue, body)

      val future = fetch(url(path))
      val response = Await.result(future, timeout)
      response.status === httpOk

      val bodyFuture = response.entity.toStrict(timeout).map(_.data.utf8String)
      Await.result(bodyFuture, timeout) === body
    }

    "stub in js" in {
      stubResponse(path, httpOk.intValue, body)

      val js =
        s"""(function (context) {
           |  var cs = fetch("${url(path)}")
           |  return cs;
           |})(this);
       """.stripMargin

      val cs = evalJs(js)

      checkResponse(cs, httpOk, body)
    }

    "stub in js withThen" in {

      stubResponse(path, httpOk.intValue, body)

      val js =
        s"""(function (context) {
           |var cs = fetch("${url(path)}").thenApply(function(response) {
           |  print("Got response")
           |  var status = response.status().value();
           |  print(status);
           |  return status;
           |});
           |return cs;
           |})(this);
       """.stripMargin

      val ne = NashornEngine.init()
      ne.evalResource("/fetch.js")
      val cs = ne.evalString(js).asInstanceOf[CompletionStage[String]]

      val f = FutureConverters.toScala(cs)

      val response = Await.result(f, timeout)
      response === "200 OK"
    }
  }

  private def stubResponse(path: String, code: Int, body: String) = {
    stubFor(get(urlEqualTo(path))
      .willReturn(
        aResponse()
          .withStatus(code)
          .withBody(body)))
  }

  private def evalJs(js: String) = {
    val ne = NashornEngine.init()
    ne.evalResource("/fetch.js")
    val cs = ne.evalString(js).asInstanceOf[CompletionStage[HttpResponse]]
    cs
  }

  private def checkResponse(cs: CompletionStage[HttpResponse], httpOk: StatusCodes.Success, body: String) = {
    val f = FutureConverters.toScala(cs)
    val response = Await.result(f, timeout)
    response.status === httpOk

    val bodyFuture = response.entity.toStrict(timeout).map(_.data.utf8String)
    Await.result(bodyFuture, timeout) === body
  }
}
