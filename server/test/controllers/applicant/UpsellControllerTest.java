package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.CfTestHelpers.requestBuilderWithSettings;

import controllers.WithMockedProfiles;
import models.Applicant;
import models.Application;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import support.ProgramBuilder;
import support.QuestionAnswerer;

public class UpsellControllerTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void considerRegister_redirectsToUpsellViewForDefaultProgramType() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    Applicant applicant = createApplicantWithMockedProfile();
    Application application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());
    String redirectLocation = "someUrl";

    Result result =
        instanceOf(UpsellController.class)
            .considerRegister(
                addCSRFToken(requestBuilderWithSettings()).build(),
                applicant.id,
                programDefinition.id(),
                application.id,
                redirectLocation)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Application confirmation");
    assertThat(contentAsString(result)).contains("Create account");
  }

  @Test
  public void
      considerRegister_redirectsToUpsellViewForCommonIntakeWithNoRecommendedProgramsFound() {
    Question predicateQuestion = testQuestionBank().applicantFavoriteColor();
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            predicateQuestion.id,
                            Scalar.TEXT,
                            Operator.EQUAL_TO,
                            PredicateValue.of("yellow"))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("color-program")
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withEligibilityDefinition(eligibility)
            .buildDefinition();

    Applicant applicant = createApplicantWithMockedProfile();

    // Answer the color question with an ineligible response
    Path colorPath = ApplicantData.APPLICANT_PATH.join("applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(applicant.getApplicantData(), colorPath, "green");
    QuestionAnswerer.addMetadata(applicant.getApplicantData(), colorPath, 456L, 12345L);
    applicant.save();

    ProgramDefinition commonIntakeForm =
        ProgramBuilder.newActiveCommonIntakeForm("commonintake")
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .buildDefinition();
    Application application =
        resourceCreator.insertActiveApplication(applicant, commonIntakeForm.toProgram());

    String redirectLocation = "someUrl";

    Result result =
        instanceOf(UpsellController.class)
            .considerRegister(
                addCSRFToken(requestBuilderWithSettings()).build(),
                applicant.id,
                commonIntakeForm.id(),
                application.id,
                redirectLocation)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Benefits");
    assertThat(contentAsString(result)).contains("could not find");
    assertThat(contentAsString(result))
        .doesNotContain(programDefinition.localizedName().getDefault());
    assertThat(contentAsString(result)).contains("Create account");
  }

  @Test
  public void considerRegister_redirectsToUpsellViewForCommonIntakeWithRecommendedPrograms() {
    Question predicateQuestion = testQuestionBank().applicantFavoriteColor();
    EligibilityDefinition eligibility =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafOperationExpressionNode.create(
                            predicateQuestion.id,
                            Scalar.TEXT,
                            Operator.EQUAL_TO,
                            PredicateValue.of("yellow"))),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("color-program")
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .withEligibilityDefinition(eligibility)
            .buildDefinition();

    Applicant applicant = createApplicantWithMockedProfile();

    // Answer the color question with an eligible response
    Path colorPath = ApplicantData.APPLICANT_PATH.join("applicant_favorite_color");
    QuestionAnswerer.answerTextQuestion(applicant.getApplicantData(), colorPath, "yellow");
    QuestionAnswerer.addMetadata(applicant.getApplicantData(), colorPath, 456L, 12345L);
    applicant.save();

    ProgramDefinition commonIntakeForm =
        ProgramBuilder.newActiveCommonIntakeForm("commonintake")
            .withBlock()
            .withRequiredQuestion(predicateQuestion)
            .buildDefinition();
    Application application =
        resourceCreator.insertActiveApplication(applicant, commonIntakeForm.toProgram());

    String redirectLocation = "someUrl";

    Result result =
        instanceOf(UpsellController.class)
            .considerRegister(
                addCSRFToken(requestBuilderWithSettings()).build(),
                applicant.id,
                commonIntakeForm.id(),
                application.id,
                redirectLocation)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Benefits");
    assertThat(contentAsString(result)).contains(programDefinition.localizedName().getDefault());
    assertThat(contentAsString(result)).contains("Create account");
  }
}
