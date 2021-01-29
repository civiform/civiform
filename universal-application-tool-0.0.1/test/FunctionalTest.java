import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static play.api.test.Helpers.testServerPort;
import static play.test.Helpers.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.route;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import controllers.AssetsFinder;
import controllers.routes;
import io.ebean.DB;
import io.ebean.Ebean;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Person;
import models.Program;
import models.Question;
import org.junit.Test;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import play.twirl.api.Content;
import repository.DbRepository;
import repository.PersonRepository;

/**
 * A functional test starts a Play application for every test.
 *
 * <p>https://www.playframework.com/documentation/latest/JavaFunctionalTest
 */
public class FunctionalTest extends WithApplication {

  protected Application provideApplication() {
    return fakeApplication(
        ImmutableMap.of(
            "db.default.driver",
            "org.testcontainers.jdbc.ContainerDatabaseDriver",
            "db.default.url",
            /* This is a magic string.  The components of it are
             * jdbc: the standard java database connection uri scheme
             * tc: Testcontainers - the tool that starts a new container per test.
             * postgresql: which container to start
             * 9.6.8: which version of postgres to start
             * ///: hostless URI scheme - anything here would be ignored
             * databasename: the name of the db to connect to - any string is okay.
             */
            "jdbc:tc:postgresql:9.6.8:///databasename"));
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
        fakeRequest(routes.PostgresController.index())
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());
    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .isEqualTo("1: Alice\n2: Bob\n3: Charles\n4: Diana\n5: Eliza\n");
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
  public void createProgram() {
    final DbRepository repo = app.injector().instanceOf(DbRepository.class);
    Program program = new Program();
    program.name = "program one";
    program.version = 1L;
    program.object = ImmutableMap.of("key", "value");
    repo.insertProgram(program).toCompletableFuture().join();
    Program p = repo.lookupProgram("program one", 1L).toCompletableFuture().join().get();
    assertThat(p.version).isEqualTo(1L);
    assertThat(p.object).containsEntry("key", "value");
  }

  @Test
  public void createApplicant() {
    final DbRepository repo = app.injector().instanceOf(DbRepository.class);
    Applicant applicant = new Applicant();
    applicant.id = 1L;
    applicant.object =
        ImmutableMap.of("nestedObject", ImmutableMap.of("foo", "bar"), "secondKey", "value");
    repo.insertApplicant(applicant).toCompletableFuture().join();
    Applicant a = repo.lookupApplicant(1L).toCompletableFuture().join().get();
    assertThat(a.id).isEqualTo(1L);
    assertThat(a.object).containsAllEntriesOf(applicant.object);
  }

  @Test
  public void createQuestion() {
    final DbRepository repo = app.injector().instanceOf(DbRepository.class);
    Question question = new Question();
    question.id = 1L;
    question.object =
        ImmutableMap.of(
            "nestedObject",
            ImmutableMap.of("foo", "bar"),
            "secondKey",
            "value",
            "target",
            "key.key");
    repo.insertQuestion(question).toCompletableFuture().join();
    Question q = repo.lookupQuestion(1L).toCompletableFuture().join().get();
    assertThat(q.id).isEqualTo(1L);
    assertThat(q.object).containsAllEntriesOf(question.object);

    q = repo.lookupQuestion("key.key").toCompletableFuture().join().get();
    assertThat(q.id).isEqualTo(1L);
    assertThat(q.object).containsAllEntriesOf(question.object);
  }

  @Test(expected = Exception.class)
  public void createQuestionWithoutTarget() throws Exception {
    final DbRepository repo = app.injector().instanceOf(DbRepository.class);
    Question question = new Question();
    question.id = 1L;
    question.object =
        ImmutableMap.of("nestedObject", ImmutableMap.of("foo", "bar"), "secondKey", "value");
    // this should fail since there is no 'target' in the object.
    repo.insertQuestion(question).toCompletableFuture().get();
    // force a flush to ensure the transaction is committed and fails.
    repo.flush();
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
