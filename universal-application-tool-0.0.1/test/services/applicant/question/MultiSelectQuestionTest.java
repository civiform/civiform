package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class MultiSelectQuestionTest {

  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicantData = new ApplicantData();
  }

  @Test
  public void multiOptionQuestion_withPresentApplicantData_tooManySelected() throws Exception {
    MultiOptionQuestionDefinition question =
        (MultiOptionQuestionDefinition)
            new QuestionDefinitionBuilder()
                .setQuestionType(QuestionType.CHECKBOX)
                .setVersion(1L)
                .setName("question name")
                .setPath(Path.create("applicant.path"))
                .setDescription("description")
                .setQuestionText(ImmutableMap.of(Locale.US, "question?"))
                .setQuestionHelpText(ImmutableMap.of(Locale.US, "help text"))
                .build();

    // Put too many selections.
    applicantData.putString(question.getPath().join("selection[0]"), "one");
    applicantData.putString(question.getPath().join("selection[1]"), "two");
    applicantData.putString(question.getPath().join("selection[2]"), "three");
    applicantData.putString(question.getPath().join("selection[3]"), "four");

    ApplicantQuestion applicantQuestion = new ApplicantQuestion(question, applicantData);
    SingleSelectQuestion singleSelectQuestion = applicantQuestion.createSingleSelectQuestion();

    assertThat(singleSelectQuestion.getQuestionErrors())
        .containsOnly(ValidationErrorMessage.tooManySelectionsError(1));
  }
}
