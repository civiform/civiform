package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;

public class ReadOnlyQuestionServiceImplTest {
  NameQuestionDefinition nameQuestion =
      new NameQuestionDefinition(
          1L,
          1L,
          "applicant name",
          "applicant.name",
          "The name of the applicant",
          ImmutableMap.of(Locale.ENGLISH, "What is your name?"),
          Optional.empty());
  AddressQuestionDefinition addressQuestion =
      new AddressQuestionDefinition(
          2L,
          1L,
          "applicant addresss",
          "applicant.address",
          "The address of the applicant",
          ImmutableMap.of(Locale.ENGLISH, "What is your address?"),
          Optional.empty());
  QuestionDefinition basicQuestion =
      new QuestionDefinition(
          3L,
          1L,
          "applicant's favorite color",
          "applicant.favoriteColor",
          "The favorite color of the applicant",
          ImmutableMap.of(Locale.ENGLISH, "What is your favorite color?"),
          Optional.empty());

  private String invalidPath = "invalid.path";

  private ImmutableList<QuestionDefinition> questions =
      ImmutableList.of(nameQuestion, addressQuestion, basicQuestion);

  private ReadOnlyQuestionService service = new ReadOnlyQuestionServiceImpl(questions);

  private ReadOnlyQuestionService emptyService =
      new ReadOnlyQuestionServiceImpl(ImmutableList.of());

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
    int expectedScalars =
        nameQuestion.getScalars().size()
            + addressQuestion.getScalars().size()
            + basicQuestion.getScalars().size();
    assertThat(service.getAllScalars().size()).isEqualTo(expectedScalars);
    // TODO: Verify the contents not just the size.
  }

  @Test
  public void getPathScalars_forQuestion() throws InvalidPathException {
    ImmutableMap<String, ScalarType> result = service.getPathScalars("applicant.address");
    ImmutableMap<String, ScalarType> expected = addressQuestion.getFullyQualifiedScalars();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getPathScalars_forScalar() throws InvalidPathException {
    ImmutableMap<String, ScalarType> result = service.getPathScalars("applicant.address.city");
    ImmutableMap<String, ScalarType> expected =
        ImmutableMap.of("applicant.address.city", ScalarType.STRING);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getPathScalars_forInvalidPath() {
    assertThatThrownBy(() -> service.getPathScalars(invalidPath))
        .isInstanceOf(InvalidPathException.class)
        .hasMessage("Path not found: " + invalidPath);
  }

  @Test
  public void getPathType_forInvalidPath() {
    assertThat(service.getPathType(invalidPath)).isEqualTo(PathType.NONE);
  }

  @Test
  public void getPathType_forQuestion() {
    assertThat(service.getPathType("applicant.favoriteColor")).isEqualTo(PathType.QUESTION);
  }

  @Test
  public void getPathType_forScalar() {
    assertThat(service.getPathType("applicant.name.first")).isEqualTo(PathType.SCALAR);
  }

  @Test
  public void getQuestionDefinition_forInvalidPath() {
    assertThatThrownBy(() -> service.getQuestionDefinition(invalidPath))
        .isInstanceOf(InvalidPathException.class)
        .hasMessage("Path not found: " + invalidPath);
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
  public void isValid_returnsFalseForInvalid() {
    assertThat(service.isValid("invalidPath")).isFalse();
  }

  @Test
  public void isValid_returnsFalseWhenEmpty() {
    assertThat(emptyService.isValid("invalidPath")).isFalse();
  }

  @Test
  public void isValid_returnsTrueForQuestion() {
    assertThat(service.isValid("applicant.name")).isTrue();
  }

  @Test
  public void isValid_returnsTrueForScalar() {
    assertThat(service.isValid("applicant.name.first")).isTrue();
  }
}
