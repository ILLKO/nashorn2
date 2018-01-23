package js.scalameter

import scala.concurrent.duration.FiniteDuration

trait HttpClient {

  def send(numRequests: Int, interval: FiniteDuration, host: String, port: Int, path: String)


}
