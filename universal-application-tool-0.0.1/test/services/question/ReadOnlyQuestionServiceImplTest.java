package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import services.Path;

public class ReadOnlyQuestionServiceImplTest {

  private NameQuestionDefinition nameQuestion;
  private AddressQuestionDefinition addressQuestion;
  private QuestionDefinition basicQuestion;

  private final Path invalidPath = Path.create("invalid.path");

  private ImmutableList<QuestionDefinition> questions;

  private ReadOnlyQuestionService service;

  private final ReadOnlyQuestionService emptyService =
      new ReadOnlyQuestionServiceImpl(ImmutableList.of());

  @Before
  public void setupQuestions() throws UnsupportedQuestionTypeException {
    // The tests mimic that the persisted questions are read into ReadOnlyQuestionService.
    // Therefore, question ids cannot be empty.
    nameQuestion =
        (NameQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.NAME)
                .setId(123L)
                .setVersion(1L)
                .setName("applicant name")
                .setPath(Path.create("applicant.name"))
                .setDescription("The name of the applicant")
                .setQuestionText(ImmutableMap.of(Locale.US, "What is your name?"))
                .build();
    addressQuestion =
        (AddressQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.ADDRESS)
                .setId(456L)
                .setVersion(1L)
                .setName("applicant addresss")
                .setPath(Path.create("applicant.address"))
                .setDescription("The address of the applicant")
                .setQuestionText(ImmutableMap.of(Locale.US, "What is your address?"))
                .build();
    basicQuestion =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.TEXT)
            .setId(789L)
            .setVersion(1L)
            .setName("applicant's favorite color")
            .setPath(Path.create("applicant.favoriteColor"))
            .setDescription("The favorite color of the applicant")
            .setQuestionText(ImmutableMap.of(Locale.US, "What is your favorite color?"))
            .build();
    questions = ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);
    service = new ReadOnlyQuestionServiceImpl(questions);
  }

  @Test
  public void getAll_returnsEmpty() {
    assertThat(emptyService.getAllQuestions()).isEmpty();
    assertThat(emptyService.getAllScalars()).isEmpty();
  }

  @Test
  public void getAllQuestions() {
    assertThat(service.getAllQuestions().size()).isEqualTo(3);
  }

  @Test
  public void getAllScalars() {
    ImmutableMap.Builder<Path, ScalarType> scalars = ImmutableMap.builder();
    scalars.putAll(nameQuestion.getScalars());
    scalars.putAll(addressQuestion.getScalars());
    scalars.putAll(basicQuestion.getScalars());
    assertThat(service.getAllScalars()).isEqualTo(scalars.build());
  }

  @Test
  public void getPathScalars_forQuestion() throws InvalidPathException {
    ImmutableMap<Path, ScalarType> result =
        service.getPathScalars(Path.create("applicant.address"));
    ImmutableMap<Path, ScalarType> expected = addressQuestion.getScalars();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getPathScalars_forScalar() throws InvalidPathException {
    ImmutableMap<Path, ScalarType> result =
        service.getPathScalars(Path.create("applicant.address.city"));
    ImmutableMap<Path, ScalarType> expected =
        ImmutableMap.of(Path.create("applicant.address.city"), ScalarType.STRING);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getPathScalars_forInvalidPath() {
    assertThatThrownBy(() -> service.getPathScalars(invalidPath))
        .isInstanceOf(InvalidPathException.class)
        .hasMessage("Path not found: " + invalidPath.path());
  }

  @Test
  public void getPathType_forInvalidPath() {
    assertThat(service.getPathType(invalidPath)).isEqualTo(PathType.NONE);
  }

  @Test
  public void getPathType_forQuestion() {
    assertThat(service.getPathType(Path.create("applicant.favoriteColor")))
        .isEqualTo(PathType.QUESTION);
  }

  @Test
  public void getPathType_forScalar() {
    assertThat(service.getPathType(Path.create("applicant.name.first"))).isEqualTo(PathType.SCALAR);
  }

  @Test
  public void getQuestionDefinition_forInvalidPath() {
    assertThatThrownBy(() -> service.getQuestionDefinition(invalidPath))
        .isInstanceOf(InvalidPathException.class)
        .hasMessage("Path not found: " + invalidPath.path());
  }

  @Test
  public void getQuestionDefinition_forQuestion() throws InvalidPathException {
    assertThat(service.getQuestionDefinition("applicant.address")).isEqualTo(addressQuestion);
  }

  @Test
  public void getQuestionDefinition_forScalar() throws InvalidPathException {
    assertThat(service.getQuestionDefinition("applicant.name.first")).isEqualTo(nameQuestion);
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    assertThat(service.getQuestionDefinition(123L)).isEqualTo(nameQuestion);
  }

  @Test
  public void isValid_returnsFalseForInvalid() {
    assertThat(service.isValid(Path.create("invalidPath"))).isFalse();
  }

  @Test
  public void isValid_returnsFalseWhenEmpty() {
    assertThat(emptyService.isValid(Path.create("invalidPath"))).isFalse();
  }

  @Test
  public void isValid_returnsTrueForQuestion() {
    assertThat(service.isValid(Path.create("applicant.name"))).isTrue();
  }

  @Test
  public void isValid_returnsTrueForScalar() {
    assertThat(service.isValid(Path.create("applicant.name.first"))).isTrue();
  }
}
