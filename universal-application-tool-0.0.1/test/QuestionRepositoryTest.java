import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import models.Question;
import org.junit.Test;
import repository.QuestionRepository;

public class QuestionRepositoryTest extends WithPostgresContainer {

  @Test
  public void createQuestion() {
    // arrange
    final QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
    Question question = new Question();
    question.id = 1L;
    question.setObject(
        ImmutableMap.of(
            "nestedObject",
            ImmutableMap.of("foo", "bar"),
            "secondKey",
            "value",
            "target",
            "key.key"));
    // act
    repo.insertQuestion(question).toCompletableFuture().join();
    // assert
    Question q = repo.lookupQuestion(1L).toCompletableFuture().join().get();
    assertThat(q.id).isEqualTo(1L);
    assertThat(q.getObject()).containsAllEntriesOf(question.getObject());
  }
}
