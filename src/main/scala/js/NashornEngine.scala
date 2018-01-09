package js

import java.util.concurrent.Executors
import java.util.function.Consumer
import javax.script.{ScriptContext, ScriptEngine, SimpleScriptContext}

import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Promise
import scala.io.Source

class NashornEngine(val engine: ScriptEngine, val sc: ScriptContext) {

  def evalResource(resource: String): AnyRef = {
    print(s"Running $resource ")
    val time = System.nanoTime()
    val code = NashornEngine.readResource(resource)
    val result = engine.eval(code)
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
    engine.eval(code)
    println(s" done")

    f.onSuccess(handler)
  }

  def evalString(script: String): AnyRef = {
    engine.eval(script)
  }
}

object JavascriptLogger {
  val logger: Logger = LoggerFactory.getLogger(JavascriptLogger.getClass)
}

object NashornEngine {

  val globalScheduledThreadPool = Executors.newScheduledThreadPool(20)

  def init(): NashornEngine = {

    import javax.script.ScriptEngineManager

    val manager = new ScriptEngineManager(null)
    val engine: ScriptEngine = manager.getEngineByName("nashorn")
    assert(engine != null, "could not get nashorn engine")

    val sc: SimpleScriptContext = initScriptContext(engine)

    new NashornEngine(engine, sc)
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

  def initScriptContext(engine: ScriptEngine): SimpleScriptContext = {
    new SimpleScriptContext()
  }

  def readResource(resource: String): String = {
    val is = getClass.getResourceAsStream(resource)
    Source.fromInputStream(is).mkString
  }

}