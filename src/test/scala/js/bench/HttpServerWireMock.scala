package js.bench

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

class HttpServerWireMock(val path: String, response: String) {

  val Port = 8080
  val Host = "localhost"
  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))

  def start() = {
    wireMockServer.start()
    WireMock.configureFor(Host, Port)
    stubResponse(path, 200, response)
  }

  def stop() = wireMockServer.stop()

  def url(path: String) = s"http://$Host:$Port$path"

  private def stubResponse(path: String, code: Int, body: String) = {
    stubFor(get(urlEqualTo(path))
      .willReturn(
        aResponse()
          .withStatus(code)
          .withBody(body)))
  }
}
