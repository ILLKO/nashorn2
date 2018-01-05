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

  "WireMock" should {
    val timeout = 1 second

    "stub get request" in {

      val path = "/my/resource"
      stubFor(get(urlEqualTo(path))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("Response Body")))

      val future = fetch(url(path))
      val response = Await.result(future, timeout)
      response.status === StatusCodes.OK

      val bodyFuture = response.entity.toStrict(timeout).map(_.data.utf8String)
      Await.result(bodyFuture, timeout) === "Response Body"
    }

    "stub in js" in {

      val path = "/my/resource"
      stubFor(get(urlEqualTo(path))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("Response Body")))

      val js =
        s"""(function (context) {
           |  'use strict';
           |
         |var cs = fetch("${url(path)}")
           |  return cs;
           |})(this);
       """.stripMargin

      val ne = NashornEngine.init()
      ne.evalResource("/fetch.js")
      val cs = ne.evalString(js).asInstanceOf[CompletionStage[HttpResponse]]

      val f = FutureConverters.toScala(cs)

      //      obj.getClass === classOf[ScriptObjectMirror]

      val response = Await.result(f, timeout)
      response.status === StatusCodes.OK

      val bodyFuture = response.entity.toStrict(timeout).map(_.data.utf8String)
      Await.result(bodyFuture, timeout) === "Response Body"
    }

    "stub in js withThen" in {

      val path = "/my/resource"
      stubFor(get(urlEqualTo(path))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("Response Body")))

      val js =
        s"""(function (context) {
           |  'use strict';
           |
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

      //      obj.getClass === classOf[ScriptObjectMirror]

      val response = Await.result(f, timeout)
      response === "200 OK"
    }
  }

}
