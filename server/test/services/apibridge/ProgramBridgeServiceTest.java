package services.apibridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.applicant.question.Scalar;
import services.program.ProgramDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ProgramBridgeServiceTest extends ResetPostgres {
  private ProgramBridgeService programBridgeService;

  @Before
  public void setup() {
    programBridgeService = instanceOf(ProgramBridgeService.class);
  }

  @Test
  public void getAllowedQuestions_returnsExpectedQuestions() {
    QuestionDefinition questionAddress =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition questionText =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();
    QuestionDefinition questionName = testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition questionStatic = testQuestionBank.staticContent().getQuestionDefinition();
    QuestionDefinition questionFile =
        testQuestionBank.fileUploadApplicantFile().getQuestionDefinition();
    QuestionDefinition questionEnumerator =
        testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition();

    ProgramDefinition program =
        ProgramBuilder.newDraftProgram("program1")
            .withBlock()
            .withRequiredQuestionDefinition(questionAddress)
            .withRequiredQuestionDefinition(questionText)
            .withRequiredQuestionDefinition(questionName)
            .withRequiredQuestionDefinition(questionStatic)
            .withRequiredQuestionDefinition(questionFile)
            .withRequiredQuestionDefinition(questionEnumerator)
            .buildDefinition();

    ImmutableList<QuestionDefinition> result = programBridgeService.getAllowedQuestions(program);

    assertThat(result).hasSize(3);
  }

  @Test
  public void getAllowedQuestions_throwsWhenArgumentsAreNull() {
    assertThatThrownBy(() -> programBridgeService.getAllowedQuestions(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void getQuestionScalarMap_returnsExpectedData()
      throws InvalidQuestionTypeException, UnsupportedQuestionTypeException {
    QuestionDefinition questionAddress =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition questionText =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();
    QuestionDefinition questionName = testQuestionBank.nameApplicantName().getQuestionDefinition();

    var expectedAddressScalarsCount = Scalar.getScalars(questionAddress.getQuestionType()).size();
    var expectedTextScalarsCount = Scalar.getScalars(questionText.getQuestionType()).size();
    var expectedNameScalarsCount = Scalar.getScalars(questionName.getQuestionType()).size();

    ImmutableMap<String, ImmutableList<ProgramBridgeService.HtmlOptionElement>> result =
        programBridgeService.getQuestionScalarMap(
            ImmutableList.of(questionAddress, questionText, questionName));

    assertThat(result)
        .hasSize(3)
        .containsKeys(
            questionAddress.getQuestionNameKey(),
            questionText.getQuestionNameKey(),
            questionName.getQuestionNameKey())
        .hasEntrySatisfying(
            questionAddress.getQuestionNameKey(),
            list -> assertThat(list).hasSize(expectedAddressScalarsCount))
        .hasEntrySatisfying(
            questionText.getQuestionNameKey(),
            list -> assertThat(list).hasSize(expectedTextScalarsCount))
        .hasEntrySatisfying(
            questionName.getQuestionNameKey(),
            list -> assertThat(list).hasSize(expectedNameScalarsCount));
  }

  @Test
  public void getQuestionScalarMap_throwsWhenArgumentsAreNull() {
    assertThatThrownBy(() -> programBridgeService.getQuestionScalarMap(null))
        .isInstanceOf(NullPointerException.class);
  }
}
