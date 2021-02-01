package controllers;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.contentAsString;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import models.Person;
import org.junit.Test;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import repository.PersonRepository;

public class PostgresControllerTest {

  @Test
  public void testPostgresIndex() {
    FormFactory formFactory = mock(FormFactory.class);
    HttpExecutionContext ec = new HttpExecutionContext(ForkJoinPool.commonPool());
    PersonRepository repository = mock(PersonRepository.class);
    Person p = new Person();
    p.id = 1L;
    p.name = "Alice";
    when(repository.list()).thenReturn(supplyAsync(() -> Set.of(p)));
    final PostgresController controller = new PostgresController(formFactory, repository, ec);
    final CompletionStage<Result> future = controller.index();

    // Block until the result is completed
    await()
        .untilAsserted(
            () ->
                assertThat(future.toCompletableFuture())
                    .isCompletedWithValueMatching(
                        result -> contentAsString(result).equals("1: Alice\n")));
  }
}
