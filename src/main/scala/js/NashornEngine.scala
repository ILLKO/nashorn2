package js

import java.util.function.Consumer
import javax.script.{ScriptContext, ScriptEngine, SimpleScriptContext}

import akka.actor.ActorSystem
import jdk.nashorn.api.scripting.JSObject
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Promise
import scala.io.Source

class NashornEngine(val engine: ScriptEngine, val sc: ScriptContext) {

  def evalResource(resource: String): AnyRef = {
    //    print(s"Running $resource ")
    val time = System.nanoTime()
    val code = NashornEngine.readResource(resource)
    val result = engine.eval(code, sc)
    val elapsed = (System.nanoTime() - time) / (1000 * 1000)
    //    println(s" done in $elapsed millis")
    result
  }

  def putFetcher[T <: JsResponse](fetcher: Fetch[T]): Unit = {
    sc.setAttribute("__FETCH_IMPL__", fetcher, ScriptContext.ENGINE_SCOPE)
  }

  def getFetcher[T <: JsResponse] = sc.getAttribute("__FETCH_IMPL__", ScriptContext.ENGINE_SCOPE).asInstanceOf[Fetch[T]]

  def evalResourceAsync[A, B](resource: String, handler: PartialFunction[A, B]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    //    print(s"Running $resource ")

    val p = Promise[A]()
    val f = p.future

    val code = NashornEngine.readResource(resource)
    engine.eval(code, sc)
    //    println(s" done")

    f.onSuccess(handler)
  }

  def evalString(script: String): AnyRef = {
    engine.eval(script, sc)
  }

  def newObject(name: String, param: Object): JSObject = {
    val func = engine.get(name).asInstanceOf[JSObject]
    func.newObject(param).asInstanceOf[JSObject]
  }

}

object JavascriptLogger {
  val logger: Logger = LoggerFactory.getLogger(JavascriptLogger.getClass)
}

object NashornEngine {

  def init(): NashornEngine = init(ActorSystem("client"))

  def init(system: ActorSystem): NashornEngine = init[JsResponseSttp]((ne: NashornEngine) => new FetchOnSttp(system, ne))

  def init[T <: JsResponse](newFetch: (NashornEngine => Fetch[T])): NashornEngine = {

    import javax.script.ScriptEngineManager

    val manager = new ScriptEngineManager(null)
    val engine: ScriptEngine = manager.getEngineByName("nashorn")
    assert(engine != null, "could not get nashorn engine")

    val sc: SimpleScriptContext = initScriptContext(engine)

    val ne = new NashornEngine(engine, sc)
    ne.putFetcher(newFetch(ne))
    ne.evalResource("/js/fetch.js")
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

  def initScriptContext[T <: JsResponse](engine: ScriptEngine): SimpleScriptContext = {

    val sc = new SimpleScriptContext()
    val bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE)

    sc.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
    sc.setAttribute("consoleLogInfo", consoleLogInfo, ScriptContext.ENGINE_SCOPE)
    sc.setAttribute("consoleLogError", consoleLogError, ScriptContext.ENGINE_SCOPE)

    //    val initialBindings = sc.getBindings(ScriptContext.ENGINE_SCOPE)
    sc
  }

  def readResource(resource: String): String = {
    val is = getClass.getResourceAsStream(resource)
    Source.fromInputStream(is).mkString
  }

}