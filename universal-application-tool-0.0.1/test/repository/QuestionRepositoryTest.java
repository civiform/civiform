package repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import models.Question;
import org.junit.Test;

public class QuestionRepositoryTest extends WithTruncatingTables {

  @Test
  public void createQuestion() {
    final QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);
    Question question = new Question();
    question.setObject(
        ImmutableMap.of(
            "nestedObject",
            ImmutableMap.of("foo", "bar"),
            "secondKey",
            "value",
            "target",
            "key.key"));

    repo.insertQuestion(question).toCompletableFuture().join();

    long id = question.id;
    Question q = repo.lookupQuestion(id).toCompletableFuture().join().get();
    assertThat(q.id).isEqualTo(id);
    assertThat(q.getObject()).containsAllEntriesOf(question.getObject());
  }
}
