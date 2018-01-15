package task

import akka.actor.ActorSystem
import com.google.common.util.concurrent.AtomicLongMap
import js.bench._
import js.{Fetch, FetchOnSttp, NashornEngine}

import scala.compat.java8.FutureConverters
import scala.collection.JavaConverters._
import scala.util.Try

case class BenchmarkConfig(users: Int, serverRps: Option[Int], clientRps: Option[Int], seconds: Int) {
  override def toString = s"users: $users, serverRps: $serverRps, clientRps: $clientRps, seconds: $seconds"
}

class LoadTest(client: Fetch[_], cfg: BenchmarkConfig) {

  implicit val timer = new TimerImpl(client.system)

  val ratePerInterval: Option[Int] = cfg.clientRps.map { rps => rps / Config.intervalsPerSecond }

  val limiter: Option[RateLimiter] = for (rps <- cfg.clientRps;
                                          rpi <- ratePerInterval) yield
    new RateLimiterInterpolated(cfg.users * rps, "client", decrementStep = cfg.users * rpi)

  def countMap = AtomicLongMap.create[String]()

  val counts :: rejections :: failures :: Nil = List.fill(3)(countMap)

  @volatile var start = timer.now

  def bench() = {
    import client.system.dispatcher
    println(cfg)

    def callRest(token: String) = {
      val jcs = client.fetch("GET", "http://localhost:8080/service")
      FutureConverters.toScala(jcs.cs).map { response =>
        counts.incrementAndGet(token)
      }.failed.map { f =>
        println(f)
        failures.incrementAndGet(token)
      }
    }

    val batchSize = ratePerInterval.getOrElse(20)
    val start = timer.now
    do {
      for (_ <- 1 to batchSize;
           token <- (1 to cfg.users).map(LoadTest.tokenByIndex)) {
        callRest(token)
      }

      limiter.foreach { l =>
        if (!l.isRequestAllowed) {
          Thread.sleep(l.waitTime)
        }
      }
    } while (timer.now - start < cfg.seconds * 1000L * 1000 * 1000)

    report()
    LoadTest.terminate(client.system)
  }

  def report() = {
    def mapTotal[T](map: AtomicLongMap[T]) =
      map.asMap().asScala.values.map(_.longValue).sum

    val total :: totalRejections :: totalFailures :: Nil = Seq(counts, rejections, failures).map(mapTotal)

    println(s"Total requests: $total")

    cfg.serverRps.foreach {
      rps =>
        val expected = cfg.users * rps * cfg.seconds
        val delta = total - expected
        val percentage = total * 100 / expected

        println(s"expected: $expected, actual - expected: $delta, percentage: $percentage")
    }
    println(s"Rejections: $totalRejections, Failures: $totalFailures")
  }
}

object LoadTest {

  def tokenByIndex(index: Int) = index.toString

  def terminate(system: ActorSystem) = system.terminate()

  def argsToCfg(args: Array[String]) = {
    val users = args(0).toInt
    val serverRps = Try(args(1).toInt).toOption
    val clientRps = Try(args(2).toInt).toOption
    val seconds = Try(args(3).toInt).getOrElse(10)

    BenchmarkConfig(users = users,
      serverRps = serverRps,
      seconds = seconds,
      clientRps = clientRps)
  }

  def getEngine: NashornEngine = {
    NashornEngine.instance
  }

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("Usage: LoadTest users [serverRps] [clientRps] [seconds]")
      return
    }
    println("Max Memory:" + Runtime.getRuntime.maxMemory / (1024 * 1024) + " Mb")

    val cfg = argsToCfg(args)

    val server = new HttpServerWireMock("/service", "x" * 600 * 1000)
    val client = FetchOnSttp
    val ne = getEngine
    ne.evalResource("/js/fetch.js")

    server.start()

    new LoadTest(client, cfg).bench()
    terminate(client.system)
    server.stop()
  }
}