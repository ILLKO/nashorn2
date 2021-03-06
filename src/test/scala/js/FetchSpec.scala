package js

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
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

  sequential

  val timeout = 1 second
  val body = "Response Body"
  val path = "/my/resource"
  val httpOk = StatusCodes.OK

  val engine = NashornEngine.init()

  "WireMock" should {

    "stub in js" in {
      stubResponse(path, httpOk.intValue, body)

      val js =
        s"""fetch("${url(path)}");"""

      val cs = evalJs(js)

      checkResponse(cs, httpOk, body)
    }

    "response js code" in {

      stubResponse(path, httpOk.intValue, body)

      val js =
        s"""
           |fetch("${url(path)}").then(function(response) {
           |  return response.statusText();
           |})
           |""".stripMargin

      val ne = NashornEngine.init()
      val jcs = ne.evalString(js).asInstanceOf[JsCompletionStage[String]]

      val f = FutureConverters.toScala(jcs.cs)

      val response = Await.result(f, timeout)
      response === "OK"
    }

    "response body text" in {

      stubResponse(path, httpOk.intValue, body)

      val js =
        s"""
           |fetch("${url(path)}").then(function(response) {
           |  return response.text();
           |})
       """.stripMargin

      val jcs = engine.evalString(js).asInstanceOf[JsCompletionStage[String]]

      val f = FutureConverters.toScala(jcs.cs)

      val response = Await.result(f, timeout)
      response === body
    }

    "headers" in {

      stubFor(get(urlEqualTo(path))
        .withHeader("Accept", WireMock.equalTo("application/json"))
        .willReturn(
          aResponse()
            .withStatus(httpOk.intValue)
            .withHeader("Content-Type", "application/json")
            .withBody("{}")))

      val js =
        s"""
           |fetch("${url(path)}", { headers: new Headers({"Accept": "application/json"})}).then(function(response) {
           |  return response.headers().get('content-type');
           |})
       """.stripMargin

      val jcs = engine.evalString(js).asInstanceOf[JsCompletionStage[String]]

      val f = FutureConverters.toScala(jcs.cs)

      val response = Await.result(f, timeout)
      response === "application/json"
    }

    "post request body string" in {
      val PostRequestBody = "POST Request Body"
      val PostResponseBody = "POST Response Body"
      stubFor(post(urlEqualTo(path))
        .withRequestBody(WireMock.equalTo(PostRequestBody))
        .willReturn(
          aResponse()
            .withStatus(httpOk.intValue)
            .withBody(PostResponseBody)))


      val js = s"""fetch("${url(path)}", { method: "POST", body: "$PostRequestBody"})"""

      val cs = evalJs(js)

      checkResponse(cs, httpOk, PostResponseBody)
    }

    "response json" in {
      val json =
        """{"query":{"statistics":{"pages":43928588,"articles":5547529,"edits":928676946,"images":851937,"users":32631130,"activeusers":121876,"admins":1240,"jobs":42273}}}"""

      stubResponse(path, httpOk.intValue, json)
      val js =
        s"""
           |fetch("${url(path)}").then(function(response) {
           |  return response.json();
           |}).then(function(json) {
           |  return json.query.statistics.pages;
           |});
           |""".stripMargin

      val jcs = engine.evalString(js).asInstanceOf[JsCompletionStage[Integer]]

      val f = FutureConverters.toScala(jcs.cs)

      val response = Await.result(f, timeout)
      response === 43928588
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
    engine.evalString(js).asInstanceOf[JsCompletionStage[JsResponse]]
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
