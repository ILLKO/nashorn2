package js.bench

import akka.actor.{Actor, ActorSystem, DeadLetter, Props}

class DeadLetterListener extends Actor {
  def receive = {
    case d: DeadLetter => println(d)
  }
}

object DeadLetterListener {
  val printDeadLetters: Boolean = false

  def subscribe(system: ActorSystem) = {
    if (printDeadLetters) {
      val listener = system.actorOf(Props[DeadLetterListener])
      system.eventStream.subscribe(listener, classOf[DeadLetter])
    }
  }
}
