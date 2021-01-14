import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.contentAsString;

import akka.actor.ActorSystem;
import controllers.AsyncController;
import controllers.CountController;
import controllers.PostgresController;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import models.PostgresDatabase;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import scala.concurrent.ExecutionContextExecutor;

/**
 * Unit testing does not require Play application start up.
 *
 * <p>https://www.playframework.com/documentation/latest/JavaTest
 */
public class UnitTest {

  @Test
  public void simpleCheck() {
    int a = 1 + 1;
    assertThat(a).isEqualTo(2);
  }

  // Unit test a controller
  @Test
  public void testCount() {
    final CountController controller = new CountController(() -> 49);
    Result result = controller.count();
    assertThat(contentAsString(result)).isEqualTo("49");
  }

  // Unit test a controller with async return
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

  // Unit test Postgres controller
  @Test
  public void testPostgres() {
    HttpExecutionContext ec = new HttpExecutionContext(ForkJoinPool.commonPool());
    PostgresDatabase database = mock(PostgresDatabase.class);
    when(database.updateSomething()).thenReturn(supplyAsync(() -> 10));
    final PostgresController controller = new PostgresController(database, ec);
    final CompletionStage<Result> future = controller.retrieve();

    // Block until the result is completed
    await()
        .untilAsserted(
            () ->
                assertThat(future.toCompletableFuture())
                    .isCompletedWithValueMatching(
                        result ->
                            contentAsString(result)
                                .equals("This page has been retrieved 10 times.")));
  }
}
