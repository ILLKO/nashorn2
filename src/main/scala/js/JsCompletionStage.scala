package js

import java.util.concurrent.{CompletableFuture, CompletionStage, Executor}
import java.util.function
import java.util.function.{BiConsumer, BiFunction, Consumer}

import scala.compat.java8.functionConverterImpls.{AsJavaFunction, FromJavaFunction}

class JsCompletionStage[T](val cs: CompletionStage[T]) extends CompletionStage[T] {

  def wrap[U](r: U): CompletionStage[U] = {
    r match {
      case jsc: JsCompletionStage[U] => jsc.cs
      case _ => CompletableFuture.completedFuture(r)
    }
  }

  def `then`[U](fn: function.Function[T, U]): JsCompletionStage[U] = {
    val f = new AsJavaFunction(new FromJavaFunction[T, U](fn) andThen wrap)
    new JsCompletionStage(cs.thenCompose(f))
  }

  override def runAfterBothAsync(other: CompletionStage[_], action: Runnable) = ???

  override def runAfterBothAsync(other: CompletionStage[_], action: Runnable, executor: Executor) = ???

  override def thenRunAsync(action: Runnable) = ???

  override def thenRunAsync(action: Runnable, executor: Executor) = ???

  override def applyToEitherAsync[U](other: CompletionStage[_ <: T], fn: function.Function[_ >: T, U]) = ???

  override def applyToEitherAsync[U](other: CompletionStage[_ <: T], fn: function.Function[_ >: T, U], executor: Executor) = ???

  override def runAfterEitherAsync(other: CompletionStage[_], action: Runnable) = ???

  override def runAfterEitherAsync(other: CompletionStage[_], action: Runnable, executor: Executor) = ???

  override def acceptEitherAsync(other: CompletionStage[_ <: T], action: Consumer[_ >: T]) = ???

  override def acceptEitherAsync(other: CompletionStage[_ <: T], action: Consumer[_ >: T], executor: Executor) = ???

  override def acceptEither(other: CompletionStage[_ <: T], action: Consumer[_ >: T]) = ???

  override def thenAcceptBoth[U](other: CompletionStage[_ <: U], action: BiConsumer[_ >: T, _ >: U]) = ???

  override def applyToEither[U](other: CompletionStage[_ <: T], fn: function.Function[_ >: T, U]) = ???

  override def runAfterEither(other: CompletionStage[_], action: Runnable) = ???

  override def thenApply[U](fn: function.Function[_ >: T, _ <: U]) = ???

  override def thenComposeAsync[U](fn: function.Function[_ >: T, _ <: CompletionStage[U]]) = ???

  override def thenComposeAsync[U](fn: function.Function[_ >: T, _ <: CompletionStage[U]], executor: Executor) = ???

  override def thenAccept(action: Consumer[_ >: T]) = ???

  override def thenRun(action: Runnable) = ???

  override def runAfterBoth(other: CompletionStage[_], action: Runnable) = ???

  override def exceptionally(fn: function.Function[Throwable, _ <: T]) = ???

  override def handleAsync[U](fn: BiFunction[_ >: T, Throwable, _ <: U]) = ???

  override def handleAsync[U](fn: BiFunction[_ >: T, Throwable, _ <: U], executor: Executor) = ???

  override def handle[U](fn: BiFunction[_ >: T, Throwable, _ <: U]) = ???

  override def thenCompose[U](fn: function.Function[_ >: T, _ <: CompletionStage[U]]) = ???

  override def thenAcceptBothAsync[U](other: CompletionStage[_ <: U], action: BiConsumer[_ >: T, _ >: U]) = ???

  override def thenAcceptBothAsync[U](other: CompletionStage[_ <: U], action: BiConsumer[_ >: T, _ >: U], executor: Executor) = ???

  override def thenCombineAsync[U, V](other: CompletionStage[_ <: U], fn: BiFunction[_ >: T, _ >: U, _ <: V]) = ???

  override def thenCombineAsync[U, V](other: CompletionStage[_ <: U], fn: BiFunction[_ >: T, _ >: U, _ <: V], executor: Executor) = ???

  override def whenComplete(action: BiConsumer[_ >: T, _ >: Throwable]) = ???

  override def thenCombine[U, V](other: CompletionStage[_ <: U], fn: BiFunction[_ >: T, _ >: U, _ <: V]) = ???

  override def whenCompleteAsync(action: BiConsumer[_ >: T, _ >: Throwable]) = ???

  override def whenCompleteAsync(action: BiConsumer[_ >: T, _ >: Throwable], executor: Executor) = ???

  override def toCompletableFuture = ???

  override def thenApplyAsync[U](fn: function.Function[_ >: T, _ <: U]) = ???

  override def thenApplyAsync[U](fn: function.Function[_ >: T, _ <: U], executor: Executor) = ???

  override def thenAcceptAsync(action: Consumer[_ >: T]) = ???

  override def thenAcceptAsync(action: Consumer[_ >: T], executor: Executor) = ???
}
