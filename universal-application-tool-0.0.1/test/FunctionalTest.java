import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.OK;
import static play.test.Helpers.route;

import com.google.common.collect.ImmutableMap;
import controllers.AssetsFinder;
import controllers.routes;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Person;
import org.junit.Test;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import play.twirl.api.Content;
import repository.PersonRepository;

/**
 * A functional test starts a Play application for every test.
 *
 * <p>https://www.playframework.com/documentation/latest/JavaFunctionalTest
 */
public class FunctionalTest extends WithApplication {

  protected Application provideApplication() {
    return fakeApplication(inMemoryDatabase("default", ImmutableMap.of("MODE", "PostgreSQL")));
  }

  @Test
  public void renderTemplate() {
    // If you are calling out to Assets, then you must instantiate an application
    // because it makes use of assets metadata that is configured from
    // the application.

    AssetsFinder assetsFinder = provideApplication().injector().instanceOf(AssetsFinder.class);

    Content html = views.html.index.render("Your new application is ready.", assetsFinder);
    assertThat("text/html").isEqualTo(html.contentType());
    assertThat(html.body()).contains("Your new application is ready.");
  }

  @Test
  public void listPersons() {
    Http.RequestBuilder request =
        fakeRequest(routes.PostgresController.list())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .isEqualTo("1: Alice\n2: Bob\n3: Charles\n4: Diana\n5: Eliza\n");
  }

  @Test
  public void addPerson() {
    Http.RequestBuilder request =
        fakeRequest(routes.PostgresController.add("John"))
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("person John with ID:", "added.");
  }

  @Test
  public void testPersonRepositoryLookup() {
    final PersonRepository personRepository = app.injector().instanceOf(PersonRepository.class);
    final CompletionStage<Optional<Person>> stage = personRepository.lookup(2L);

    await()
        .atMost(1, SECONDS)
        .until(
            () -> {
              final Optional<Person> bob = stage.toCompletableFuture().get();
              return bob.map(mac -> mac.name.equals("Bob")).orElseGet(() -> false);
            });
  }
}
