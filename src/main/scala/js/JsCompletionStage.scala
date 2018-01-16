package js

import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function

import scala.compat.java8.functionConverterImpls.{AsJavaFunction, FromJavaFunction}

class JsCompletionStage[T](val cs: CompletionStage[T]) {

  def wrap[U](r: U): CompletionStage[U] = {
    r match {
      case jsc: JsCompletionStage[_] => jsc.cs.asInstanceOf[CompletionStage[U]]
      case _ => CompletableFuture.completedFuture(r)
    }
  }

  def `then`[U](fn: function.Function[T, U]): JsCompletionStage[U] = {
    val f = new AsJavaFunction(new FromJavaFunction[T, U](fn) andThen wrap)
    new JsCompletionStage(cs.thenCompose(f))
  }

  def `then`[U](fn: Function[T, U]): JsCompletionStage[U] = {
    val f = new AsJavaFunction(fn andThen wrap)
    new JsCompletionStage(cs.thenCompose(f))
  }

  def `catch`(fn: function.Function[Throwable, T]): JsCompletionStage[T] = {
    new JsCompletionStage(cs.exceptionally(fn))
  }
}
