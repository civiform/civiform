package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.Question;
import org.junit.Test;
import services.question.QuestionDefinition;

public class QuestionRepositoryTest extends WithPostgresContainer {

  @Test
  public void createQuestion() {
    QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
    QuestionDefinition questionDefinition =
        new QuestionDefinition(
            165L,
            2L,
            "question",
            "applicant.name",
            "applicant's name",
            ImmutableMap.of(Locale.US, "What is your name?"),
            Optional.empty());
    Question question = new Question(questionDefinition);

    repo.insertQuestion(question).toCompletableFuture().join();

    long id = question.id;
    Question q = repo.lookupQuestion(id).toCompletableFuture().join().get();
    assertThat(q.id).isEqualTo(id);
  }
}
