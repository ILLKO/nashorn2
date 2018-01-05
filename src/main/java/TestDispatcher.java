import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.japi.Function;
import java.util.concurrent.Callable;
import static akka.dispatch.Futures.future;
import static java.util.concurrent.TimeUnit.SECONDS;


public class TestDispatcher {

    Future<String> f = future(new Callable<String>() {
        public String call() {
            return "Hello" + "World";
        }
    }, scala.concurrent.ExecutionContext$.MODULE$.global());
}
