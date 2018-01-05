package js

import java.util.concurrent.Executors
import java.util.function.Consumer
import javax.script.{ScriptContext, ScriptEngine, SimpleScriptContext}

import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Promise
import scala.io.Source

class NashornEngine(engine: ScriptEngine, val sc: ScriptContext) {

  def evalResource(resource: String): AnyRef = {
    print(s"Running $resource ")
    val time = System.nanoTime()
    val code = NashornEngine.readResource(resource)
    val result = engine.eval(code, sc)
    val elapsed = (System.nanoTime() - time) / (1000 * 1000)
    println(s" done in $elapsed millis")
    result
  }

  def evalResourceAsync[A, B](resource: String, handler: PartialFunction[A, B]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    print(s"Running $resource ")

    val p = Promise[A]()
    val f = p.future

    val code = NashornEngine.readResource(resource)
    engine.eval(code, sc)
    println(s" done")

    f.onSuccess(handler)
  }

  def evalString(script: String): AnyRef = {
    engine.eval(script, sc)
  }
}

object JavascriptLogger {
  val logger: Logger = LoggerFactory.getLogger(JavascriptLogger.getClass)
}

object NashornEngine {

  val globalScheduledThreadPool = Executors.newScheduledThreadPool(20)

  def init(polyfills: Boolean = false): NashornEngine = {

    import javax.script.ScriptEngineManager

    val manager = new ScriptEngineManager
    val engine: ScriptEngine = manager.getEngineByName("nashorn")

    val sc: SimpleScriptContext = initScriptContext(engine)

    val ne = new NashornEngine(engine, sc)

    if (polyfills) {
      initPolyFills(ne)
    }

    ne
  }

  val consoleLogInfo: Consumer[Object] = new Consumer[Object] {
    override def accept(t: Object): Unit = {
      println(t)
      JavascriptLogger.logger.info("{}", t)
    }
  }

  val consoleLogError: Consumer[Object] = new Consumer[Object] {
    override def accept(t: Object): Unit = {
      println(t)
      JavascriptLogger.logger.error("{}", t)
    }
  }

  def initPolyFills(engine: NashornEngine): Unit = {

  }

  def initScriptContext(engine: ScriptEngine): SimpleScriptContext = {
    new SimpleScriptContext()
  }

  def readResource(resource: String): String = {
    val is = getClass.getResourceAsStream(resource)
    Source.fromInputStream(is).mkString
  }

}