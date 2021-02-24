package app;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.*;

import com.google.common.collect.ImmutableMap;
import controllers.AssetsFinder;
import controllers.routes;
import io.ebean.Ebean;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Person;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Content;
import repository.PersonRepository;
import repository.WithPostgresContainer;

/**
 * A functional test starts a Play application for every test.
 *
 * <p>https://www.playframework.com/documentation/latest/JavaFunctionalTest
 */
public class FunctionalTest extends WithPostgresContainer {

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
    Person alice = new Person();
    alice.name = "Alice";
    alice.save();

    Person bob = new Person();
    bob.name = "Bob";
    bob.save();

    Http.RequestBuilder request =
        fakeRequest(routes.PostgresController.index())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .isEqualTo(String.format("%d: Alice\n%d: Bob\n", alice.id, bob.id));
  }

  @Test
  public void createPerson() {
    Http.RequestBuilder request =
        fakeRequest(routes.PostgresController.create())
            .method(POST)
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort())
            .bodyForm(ImmutableMap.of("name", "John"));
    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("person John with ID:", "created.");
  }

  @Test
  public void createPersonSynchronously() {
    final int oldNumRecord = Ebean.find(Person.class).findCount();

    Http.RequestBuilder request =
        fakeRequest(routes.PostgresController.createSync())
            .method(POST)
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort())
            .bodyForm(ImmutableMap.of("name", "John"));
    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("person John with ID:", "synchronously created.");

    final int newNumRecord = Ebean.find(Person.class).findCount();
    assertThat(newNumRecord).isEqualTo(oldNumRecord + 1);
  }

  @Test
  public void testPersonRepositoryLookup() {
    final PersonRepository personRepository = instanceOf(PersonRepository.class);

    Person bob = new Person();
    bob.name = "Bob";
    bob.save();
    final CompletionStage<Optional<Person>> stage = personRepository.lookup(bob.id);

    Optional<Person> foundBob = stage.toCompletableFuture().join();
    assertThat(foundBob).isNotEmpty();

    assertThat(stage.thenAccept(person -> assertThat(person).hasValue(bob))).isCompleted();
  }
}
