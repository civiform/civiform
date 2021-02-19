package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import repository.QuestionRepository;
import repository.WithResettingPostgresContainer;
import services.question.QuestionDefinition;

import java.util.Optional;

public class QuestionTest extends WithResettingPostgresContainer {

    @Test
    public void canSaveQuestion() {
        QuestionRepository repo = app.injector().instanceOf(QuestionRepository.class);

        QuestionDefinition definition = new QuestionDefinition(1L,
                1L,
                "",
                "",
                "",
                ImmutableMap.of(),
                Optional.empty());

        Question question = new Question(definition);

        question.save();

        Question found = repo.lookupQuestion(definition.getId()).toCompletableFuture().join().get();

        assertThat(found.getQuestionDefinition()).isEqualTo(definition);
    }
}
