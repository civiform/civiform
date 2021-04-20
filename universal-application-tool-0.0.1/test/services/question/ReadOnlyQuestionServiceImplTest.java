package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.question.exceptions.InvalidPathException;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.ScalarType;
import services.question.types.TextQuestionDefinition;
import support.TestQuestionBank;

public class ReadOnlyQuestionServiceImplTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  private final Path invalidPath = Path.create("invalid.path");
  private final ReadOnlyQuestionService emptyService =
      new ReadOnlyQuestionServiceImpl(ImmutableList.of());
  private NameQuestionDefinition nameQuestion;
  private AddressQuestionDefinition addressQuestion;
  private TextQuestionDefinition basicQuestion;
  private ImmutableList<QuestionDefinition> questions;
  private ReadOnlyQuestionService service;

  @Before
  public void setupQuestions() {
    // The tests mimic that the persisted questions are read into ReadOnlyQuestionService.
    // Therefore, question ids cannot be empty.
    nameQuestion =
        (NameQuestionDefinition) testQuestionBank.applicantName().getQuestionDefinition();
    addressQuestion =
        (AddressQuestionDefinition) testQuestionBank.applicantAddress().getQuestionDefinition();
    basicQuestion =
        (TextQuestionDefinition) testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
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
  public void getRepeaterQuestions() {
    QuestionDefinition repeaterQuestion =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();

    ReadOnlyQuestionService repeaterService =
        new ReadOnlyQuestionServiceImpl(
            ImmutableList.<QuestionDefinition>builder()
                .addAll(questions)
                .add(repeaterQuestion)
                .build());

    assertThat(repeaterService.getAllRepeaterQuestions().size()).isEqualTo(1);
    assertThat(repeaterService.getAllRepeaterQuestions().get(0)).isEqualTo(repeaterQuestion);
    assertThat(repeaterService.getUpToDateRepeaterQuestions().size()).isEqualTo(1);
    assertThat(repeaterService.getUpToDateRepeaterQuestions().get(0)).isEqualTo(repeaterQuestion);
  }

  @Test
  public void makePath() throws Exception {
    Path path = service.makePath(Optional.empty(), "this is foRMA$$$tte34d_we_i!!rd", false);
    assertThat(path).isEqualTo(Path.create("applicant.this_is_formattedweird"));
  }

  @Test
  public void makePath_withRepeater() throws Exception {
    QuestionDefinition householdMembers =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
    service = new ReadOnlyQuestionServiceImpl(ImmutableList.of(householdMembers));

    Path path = service.makePath(Optional.of(householdMembers.getId()), "some question name", true);

    assertThat(path)
        .isEqualTo(Path.create("applicant.applicant_household_members[].some_question_name[]"));
  }

  @Test
  public void makePath_withBadRepeater_throws() {
    QuestionDefinition applicantName = testQuestionBank.applicantName().getQuestionDefinition();
    service = new ReadOnlyQuestionServiceImpl(ImmutableList.of(applicantName));

    assertThatThrownBy(
            () -> service.makePath(Optional.of(applicantName.getId()), "some question name", true))
        .isInstanceOf(InvalidQuestionTypeException.class)
        .hasMessage("NAME is not a valid question type.");
  }

  @Test
  public void makePath_isRepeated() throws Exception {
    Path path = service.makePath(Optional.empty(), "this is foRMA$$$tte34d_we_i!!rd", true);
    assertThat(path).isEqualTo(Path.create("applicant.this_is_formattedweird[]"));
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
        service.getPathScalars(Path.create("applicant.applicant_address"));
    ImmutableMap<Path, ScalarType> expected = addressQuestion.getScalars();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void getPathScalars_forScalar() throws InvalidPathException {
    ImmutableMap<Path, ScalarType> result =
        service.getPathScalars(Path.create("applicant.applicant_address.city"));
    ImmutableMap<Path, ScalarType> expected =
        ImmutableMap.of(Path.create("applicant.applicant_address.city"), ScalarType.STRING);
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
    assertThat(service.getPathType(Path.create("applicant.applicant_favorite_color")))
        .isEqualTo(PathType.QUESTION);
  }

  @Test
  public void getPathType_forScalar() {
    assertThat(service.getPathType(Path.create("applicant.applicant_name.first")))
        .isEqualTo(PathType.SCALAR);
  }

  @Test
  public void getQuestionDefinition_byId() throws QuestionNotFoundException {
    long questionId = nameQuestion.getId();
    assertThat(service.getQuestionDefinition(questionId)).isEqualTo(nameQuestion);
  }

  @Test
  public void isValid_returnsFalseForInvalid() {
    assertThat(service.isValid(Path.create("invalidPath"))).isFalse();
  }

  @Test
  public void isValid_returnsFalseWhenEmpty() {
    assertThat(emptyService.isValid(Path.empty())).isFalse();
  }

  @Test
  public void isValid_returnsTrueForQuestion() {
    assertThat(service.isValid(Path.create("applicant.applicant_name"))).isTrue();
  }

  @Test
  public void isValid_returnsTrueForScalar() {
    assertThat(service.isValid(Path.create("applicant.applicant_name.first"))).isTrue();
  }
}
