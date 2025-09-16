package support;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

@RunWith(JUnitParamsRunner.class)
public class TestQuestionBankTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Before
  public void reset() {
    testQuestionBank.reset();
  }

  @Test
  public void withoutDatabase_canGetQuestion() {
    QuestionModel question = testQuestionBank.addressApplicantAddress();

    assertThat(question.id).isEqualTo(1L);
  }

  @Test
  public void withoutDatabase_setsId() {
    testQuestionBank.addressApplicantAddress();
    QuestionModel question = testQuestionBank.nameApplicantName();

    assertThat(question.id).isEqualTo(2L);
  }

  @Test
  @Parameters(source = QuestionType.class)
  public void getSampleQuestionForAllTypes_hasASampleForEachType(QuestionType questionType) {
    // A null question type is not allowed to be created and won't show in the list
    if (questionType == QuestionType.NULL_QUESTION) {
      return;
    }

    assertThat(testQuestionBank.getSampleQuestionsForAllTypes().get(questionType)).isNotNull();
  }

  @Test
  public void createQuestionDefinition_createsQuestionWithSpecificId() {
    QuestionDefinition question =
        TestQuestionBank.createQuestionDefinition(
            "test-question", 123L, QuestionType.TEXT, Optional.empty());

    assertThat(question.getId()).isEqualTo(123L);
    assertThat(question.getName()).isEqualTo("test-question");
    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
    assertThat(question.getEnumeratorId()).isEmpty();
  }

  @Test
  public void createQuestionDefinition_withEnumeratorId_setsEnumeratorId() {
    QuestionDefinition question =
        TestQuestionBank.createQuestionDefinition(
            "repeated-question", 456L, QuestionType.TEXT, Optional.of(789L));

    assertThat(question.getId()).isEqualTo(456L);
    assertThat(question.getName()).isEqualTo("repeated-question");
    assertThat(question.getEnumeratorId()).hasValue(789L);
  }

  @Test
  public void createYesNoQuestionDefinition_createsYesNoWithCustomOptions() {
    QuestionDefinition question =
        TestQuestionBank.createYesNoQuestionDefinition(
            "yes-no-question", 100L, ImmutableList.of("yes", "no", "maybe"));

    assertThat(question.getId()).isEqualTo(100L);
    assertThat(question.getName()).isEqualTo("yes-no-question");
    assertThat(question.getQuestionType()).isEqualTo(QuestionType.YES_NO);

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
    ImmutableList<QuestionOption> options = multiOption.getOptions();
    assertThat(options).hasSize(3);
    assertThat(options.stream().map(QuestionOption::adminName))
        .containsExactly("yes", "no", "maybe");
  }

  @Test
  public void createDropdownQuestionDefinition_createsDropdownWithCustomOptions() {
    QuestionDefinition question =
        TestQuestionBank.createDropdownQuestionDefinition(
            "dropdown-question", 200L, ImmutableList.of("option1", "option2", "option3"));

    assertThat(question.getId()).isEqualTo(200L);
    assertThat(question.getName()).isEqualTo("dropdown-question");
    assertThat(question.getQuestionType()).isEqualTo(QuestionType.DROPDOWN);

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
    ImmutableList<QuestionOption> options = multiOption.getOptions();
    assertThat(options).hasSize(3);
    assertThat(options.stream().map(QuestionOption::adminName))
        .containsExactly("option1", "option2", "option3");
  }

  @Test
  public void createCustomYesNoQuestion_createsQuestionModel() {
    QuestionModel question =
        testQuestionBank.createCustomYesNoQuestion("custom-yes-no", ImmutableList.of("yes", "no"));

    assertThat(question).isNotNull();
    assertThat(question.getQuestionDefinition().getName()).isEqualTo("custom-yes-no");
    assertThat(question.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.YES_NO);
  }

  @Test
  public void createCustomDropdownQuestion_createsQuestionModel() {
    QuestionModel question =
        testQuestionBank.createCustomDropdownQuestion(
            "custom-dropdown", ImmutableList.of("a", "b", "c"));

    assertThat(question).isNotNull();
    assertThat(question.getQuestionDefinition().getName()).isEqualTo("custom-dropdown");
    assertThat(question.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.DROPDOWN);
  }

  @Test
  public void createCustomTextQuestion_createsTextQuestionModel() {
    QuestionModel question = testQuestionBank.createCustomTextQuestion("custom-text");

    assertThat(question).isNotNull();
    assertThat(question.getQuestionDefinition().getName()).isEqualTo("custom-text");
    assertThat(question.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.TEXT);
    assertThat(question.getQuestionDefinition().getEnumeratorId()).isEmpty();
  }

  @Test
  public void createCustomTextQuestion_withEnumerator_setsEnumeratorId() {
    QuestionModel question =
        testQuestionBank.createCustomTextQuestion("custom-repeated-text", Optional.of(999L));

    assertThat(question).isNotNull();
    assertThat(question.getQuestionDefinition().getName()).isEqualTo("custom-repeated-text");
    assertThat(question.getQuestionDefinition().getEnumeratorId()).hasValue(999L);
  }

  @Test
  public void createCustomAddressQuestion_createsAddressQuestionModel() {
    QuestionModel question = testQuestionBank.createCustomAddressQuestion("custom-address");

    assertThat(question).isNotNull();
    assertThat(question.getQuestionDefinition().getName()).isEqualTo("custom-address");
    assertThat(question.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.ADDRESS);
  }

  @Test
  public void createCustomEnumeratorQuestion_createsEnumeratorQuestionModel() {
    QuestionModel question = testQuestionBank.createCustomEnumeratorQuestion("custom-enumerator");

    assertThat(question).isNotNull();
    assertThat(question.getQuestionDefinition().getName()).isEqualTo("custom-enumerator");
    assertThat(question.getQuestionDefinition().getQuestionType())
        .isEqualTo(QuestionType.ENUMERATOR);
  }
}
