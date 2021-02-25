package models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import repository.QuestionRepository;
import repository.WithPostgresContainer;
import services.question.AddressQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.TextQuestionDefinition;

public class QuestionTest extends WithPostgresContainer {

  private QuestionRepository repo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
  }

  @Test
  public void canSaveQuestion() {
    QuestionDefinition definition =
        new TextQuestionDefinition(1L, "test", "my.path", "", ImmutableMap.of(), ImmutableMap.of());
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getId()).isEqualTo(question.id);
    assertThat(found.getQuestionDefinition().getVersion()).isEqualTo(1L);
    assertThat(found.getQuestionDefinition().getName()).isEqualTo("test");
    assertThat(found.getQuestionDefinition().getPath()).isEqualTo("my.path");
    assertThat(found.getQuestionDefinition().getDescription()).isEqualTo("");
    assertThat(found.getQuestionDefinition().getQuestionText()).isEqualTo(ImmutableMap.of());
    assertThat(found.getQuestionDefinition().getQuestionHelpText()).isEqualTo(ImmutableMap.of());
  }

  @Test
  public void canSerializeLocalizationMaps() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            1L,
            "",
            "",
            "",
            ImmutableMap.of(Locale.ENGLISH, "hello"),
            ImmutableMap.of(Locale.ENGLISH, "help"));
    Question question = new Question(definition);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(ImmutableMap.of(Locale.ENGLISH, "hello"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(ImmutableMap.of(Locale.ENGLISH, "help"));
  }

  @Test
  public void canSerializeDifferentQuestionTypes() {
    AddressQuestionDefinition address =
        new AddressQuestionDefinition(1L, "address", "", "", ImmutableMap.of(), ImmutableMap.of());
    Question question = new Question(address);

    question.save();

    Question found = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition()).isInstanceOf(AddressQuestionDefinition.class);
  }

  @Test
  public void pathConflictsWith_returnsTrueForBadPaths() {
    AddressQuestionDefinition address =
        new AddressQuestionDefinition(
            1L, "name", "applicant.address", "", ImmutableMap.of(), Optional.empty());
    Question question = new Question(address);

    assertThat(question.pathConflictsWith("applicant")).isTrue();
    assertThat(question.pathConflictsWith("applicant.address")).isTrue();
    assertThat(question.pathConflictsWith("applicant.address.street")).isTrue();
    assertThat(question.pathConflictsWith("applicant.address.some.other.field")).isTrue();
  }

  @Test
  public void pathConflictsWith_returnsFalseForValidPaths() {
    AddressQuestionDefinition address =
        new AddressQuestionDefinition(
            1L, "name", "applicant.address", "", ImmutableMap.of(), Optional.empty());
    Question question = new Question(address);

    assertThat(question.pathConflictsWith("applicant.employment")).isFalse();
    assertThat(question.pathConflictsWith("other.path")).isFalse();
    assertThat(question.pathConflictsWith("other.applicant")).isFalse();
    assertThat(question.pathConflictsWith("other.applicant.address")).isFalse();
    assertThat(question.pathConflictsWith("other.applicant.address.street")).isFalse();
    assertThat(question.pathConflictsWith("other.applicant.address.some.other.field")).isFalse();
  }
}
