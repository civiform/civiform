package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;

public class MultiSelectQuestionTest {

  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicantData = new ApplicantData();
  }

  @Test
  public void multiOptionQuestion_withPresentApplicantData_tooManySelected() throws Exception {
    MultiOptionQuestionDefinition question =
        new CheckboxQuestionDefinition(
            1L,
            "name",
            Path.create("applicant.path"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "question?"),
            ImmutableMap.of(Locale.US, "help text"),
            ImmutableListMultimap.of(Locale.US, "option 1"),
            MultiOptionQuestionDefinition.MultiOptionValidationPredicates.builder()
                .setMinChoicesRequired(2)
                .setMaxChoicesAllowed(3)
                .build());

    // Put too many selections.
    applicantData.putString(question.getPath().join("selection[0]"), "one");
    applicantData.putString(question.getPath().join("selection[1]"), "two");
    applicantData.putString(question.getPath().join("selection[2]"), "three");
    applicantData.putString(question.getPath().join("selection[3]"), "four");

    ApplicantQuestion applicantQuestion = new ApplicantQuestion(question, applicantData);
    MultiSelectQuestion multiSelectQuestion = applicantQuestion.createMultiSelectQuestion();

    assertThat(multiSelectQuestion.getQuestionErrors())
        .containsOnly(ValidationErrorMessage.tooManySelectionsError(3));
  }
}
