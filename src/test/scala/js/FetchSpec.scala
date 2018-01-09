package js

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import js.FetchOnAkka._
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import scala.compat.java8.FutureConverters
import scala.concurrent.Await
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

    "response js code withThen" in {

      stubResponse(path, httpOk.intValue, body)

      val js =
        s"""(function (context) {
           |var cs = fetch("${url(path)}").then(function(response) {
           |  return response.statusText();
           |});
           |return cs;
           |})(this);
       """.stripMargin

      val ne = NashornEngine.init()
      ne.evalResource("/fetch.js")
      val jcs = ne.evalString(js).asInstanceOf[JsCompletionStage[String]]

      val f = FutureConverters.toScala(jcs.cs)

      val response = Await.result(f, timeout)
      response === "OK"
    }

    "response body text withThen" in {

      stubResponse(path, httpOk.intValue, body)

      val js =
        s"""(function (context) {
           |var cs = fetch("${url(path)}").then(function(response) {
           |  return response.text();
           |});
           |return cs;
           |})(this);
       """.stripMargin

      val ne = NashornEngine.init()
      ne.evalResource("/fetch.js")
      val jcs = ne.evalString(js).asInstanceOf[JsCompletionStage[String]]

      val f = FutureConverters.toScala(jcs.cs)

      val response = Await.result(f, timeout)
      response === body
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
    ne.evalString(js).asInstanceOf[JsCompletionStage[JsResponse]]
  }

  private def checkResponse(jcs: JsCompletionStage[JsResponse], statusCode: StatusCode, body: String) = {
    val f = FutureConverters.toScala(jcs.cs)
    val response = Await.result(f, timeout)
    response.status === statusCode.intValue
    response.statusText === statusCode.reason
    response.ok === statusCode.isSuccess()

    val bodyFuture = FutureConverters.toScala(response.text().cs)
    Await.result(bodyFuture, timeout) === body
  }
}
