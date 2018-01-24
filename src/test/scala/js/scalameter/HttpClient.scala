package js.scalameter

import scala.concurrent.duration.FiniteDuration

trait HttpClient {

  def send(numRequests: Int, host: String, port: Int, path: String)

}

trait HttpClientTicked {

  def send(numRequests: Int, interval: FiniteDuration, host: String, port: Int, path: String)

}

