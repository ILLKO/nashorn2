package js.scalameter

import org.scalameter.api._

object RangeBenchmark extends Bench.ForkedTime {
  val responses = for {
    size <- Gen.range("size")(300000, 600000, 300000)
  } yield "x" * size

  measure method "map" in {
    using(responses) curve("Range") in {
      _.map(_ + 1)
    }
  }
}