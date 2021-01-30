package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static play.test.Helpers.contentAsString;

import akka.actor.ActorSystem;
import java.util.concurrent.CompletionStage;
import org.junit.Test;
import play.mvc.Result;
import scala.concurrent.ExecutionContextExecutor;

public class AsyncControllerTest {

  @Test
  public void testAsync() {
    final ActorSystem actorSystem = ActorSystem.create("test");
    try {
      final ExecutionContextExecutor ec = actorSystem.dispatcher();
      final AsyncController controller = new AsyncController(actorSystem, ec);
      final CompletionStage<Result> future = controller.message();

      // Block until the result is completed
      await()
          .untilAsserted(
              () ->
                  assertThat(future.toCompletableFuture())
                      .isCompletedWithValueMatching(
                          result -> contentAsString(result).equals("Hi!")));
    } finally {
      actorSystem.terminate();
    }
  }
}
