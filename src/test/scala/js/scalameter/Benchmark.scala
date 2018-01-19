package js.scalameter

import scala.concurrent.duration._

trait HttpClient {

  def send(numRequests: Int, interval: FiniteDuration, host: String, port: Int, path: String)
}

object Benchmark {

    val Host = "localhost"
    val Port = 8080
    val Path = "/"

    def bench(client: HttpClient, numRequests: Int, responseSize: Int) = {
      val server = new AkkaHttpServer()
      import server.executionContext

      val bf = server.serve("x" * responseSize, Host, Port, Path)

      bf.map { b =>
        client.send(numRequests, 3 millis, Host, Port, Path)
        server.terminate(b)
      }
    }

    def main(args: Array[String]): Unit = {
      val client = new AkkaHttpClient()

      bench(client, 300, 600 * 1000)
    }
}
