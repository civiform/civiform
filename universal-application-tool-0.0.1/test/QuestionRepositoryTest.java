import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;

import com.google.common.collect.ImmutableMap;
import models.Applicant;
import models.Question;
import org.junit.Test;
import play.Application;
import play.test.WithApplication;
import repository.ApplicantRepository;
import repository.QuestionRepository;

public class QuestionRepositoryTest extends WithApplication {
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
    public void createQuestion() {
        // arrange
        final QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
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
        // act
        repo.insertQuestion(question).toCompletableFuture().join();
        // assert
        Question q = repo.lookupQuestion(1L).toCompletableFuture().join().get();
        assertThat(q.id).isEqualTo(1L);
        assertThat(q.object).containsAllEntriesOf(question.object);
    }

}
