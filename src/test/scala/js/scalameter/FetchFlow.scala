package js.scalameter

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import js.{JsCompletionStage, NashornEngine}

import scala.compat.java8.FutureConverters
import scala.concurrent.Future
import scala.concurrent.duration._
import java.util.concurrent.CountDownLatch

class FetchFlow {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val Host = "localhost"
  val Port = 8080
  val Path = "/"

  val failuresAcc = new AtomicLong(0)
  val bytesAcc = new AtomicLong(0)
  val successLatch = new CountDownLatch(300)

  def serve(p: String, body: String): Future[Http.ServerBinding] = {
    val route = path(p) {
      get {
        complete(HttpEntity(body))
      }
    }
    Http().bindAndHandle(route, Host, Port)
  }

  def terminate(binding: Http.ServerBinding) = {
    binding.unbind().onComplete(_ => system.terminate())
  }

  def getNashornEngine: NashornEngine = {
    val ne = NashornEngine.instance
    ne.evalResource("/js/fetch.js")
    ne
  }

  def fetch(url: String): JsCompletionStage[String] = {
    val js =
      s"""
         |fetch("$url").then(function(response) {
         |  return response.text();
         |})
       """.stripMargin

    val ne = getNashornEngine
    ne.evalString(js).asInstanceOf[JsCompletionStage[String]]
  }

  def send(n: Int) = {

    val url = s"http://$Host:$Port$Path"

    val ticks = Source.tick(3 millis, 3 millis, 1)

    val fetchSource = Source.fromIterator(() => Iterator.continually(fetch(url)))

    val sink = Sink.foreach[JsCompletionStage[String]] { jcs =>
      consume(jcs)
    }

    ticks.zip(fetchSource).map(_._2).takeWhile(_ => successLatch.getCount > 0).runWith(sink)
    successLatch.await()
  }

  private def consume(jcs: JsCompletionStage[String]) = {
    FutureConverters.toScala(jcs.cs).foreach { body =>
      bytesAcc.addAndGet(body.length)
      successLatch.countDown()
      println(successLatch.getCount)
    }
  }
}

object FetchFlow {

  def bench(numRequests: Int, responseSize: Int) = {
    val flow = new FetchFlow()
    val bf = flow.serve("", "x" * responseSize)

    import flow.executionContext
    bf.map { b =>
      flow.send(numRequests)
      flow.terminate(b)
    }
  }

  def bench_0(numRequests: Int, responseSize: Int) = {
    val flow = new FetchFlow()
    val bf = flow.serve("", "x" * responseSize)

    import flow.executionContext
    bf.map { b =>
      for (elem <- 1 to numRequests) {
        flow.consume(flow.fetch("http://localhost:8080/"))
      }
      flow.successLatch.await()
      flow.terminate(b)
    }
  }

  def main(args: Array[String]): Unit = {
    //    bench(300, 600*1000)
    bench(300, 600 * 1000)
  }
}



