package js

import akka.dispatch.Futures

import scala.concurrent.{Future, Promise}
import JsPromise._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class JsPromise[T](code: JsPromiseFunc[T]) {

  val p: Promise[T] = Futures.promise[T]()
  val f: Future[T] = p.future

  code(resolve, reject)

  def resolve(t: T): Unit = {
    p.trySuccess(t)
  }

  def reject(ex: Throwable): Unit = {
    p.tryFailure(ex)
  }

  def reject(ex: Any): Unit = {
    p.tryFailure(JavaScriptException(ex))
  }

  def `then`[S](onFulfilled: T => S, onRejected: Throwable => Throwable): JsPromise[S] = {
    new JsPromise((resolve, reject) => f.onComplete {
      case Success(value) => resolve(onFulfilled(value))
      case Failure(ex) => reject(onRejected(ex))
    })
  }

  def `then`[S](onFulfilled: T => S): JsPromise[S] = {
    new JsPromise((resolve, reject) => f.onSuccess {
      case value => resolve(onFulfilled(value))
    })
  }

  def `catch`(onRejected: Throwable => T): JsPromise[T] = {
    new JsPromise((resolve, reject) => f.onComplete {
      case Success(value) => Success(value)
      case Failure(ex) => Success(onRejected(ex))
    })
  }
}

case class JavaScriptException(exception: scala.Any) extends RuntimeException {
  override def getMessage: String = exception.toString
}


object JsPromise {

  type JsPromiseFunc[T] = (T => Unit, Throwable => Unit) => Unit

}