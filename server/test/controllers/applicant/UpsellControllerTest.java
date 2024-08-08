package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;

import auth.ProfileFactory;
import controllers.WithMockedProfiles;
import models.ApplicantModel;
import models.ApplicationModel;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import services.question.QuestionAnswerer;
import support.ProgramBuilder;

public class UpsellControllerTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void considerRegister_redirectsToUpsellViewForDefaultProgramType() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());
    String redirectLocation = "someUrl";

    Result result =
        instanceOf(UpsellController.class)
            .considerRegister(
                fakeRequest(),
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
    QuestionModel predicateQuestion = testQuestionBank().textApplicantFavoriteColor();
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

    ApplicantModel applicant = createApplicantWithMockedProfile();

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
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, commonIntakeForm.toProgram());

    String redirectLocation = "someUrl";

    Result result =
        instanceOf(UpsellController.class)
            .considerRegister(
                fakeRequest(),
                applicant.id,
                commonIntakeForm.id(),
                application.id,
                redirectLocation)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Programs");
    assertThat(contentAsString(result)).contains("could not find");
    assertThat(contentAsString(result))
        .doesNotContain(programDefinition.localizedName().getDefault());
    assertThat(contentAsString(result)).contains("Create account");
  }

  @Test
  public void considerRegister_redirectsToUpsellViewForCommonIntakeWithRecommendedPrograms() {
    QuestionModel predicateQuestion = testQuestionBank().textApplicantFavoriteColor();
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

    ApplicantModel applicant = createApplicantWithMockedProfile();

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
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, commonIntakeForm.toProgram());

    String redirectLocation = "someUrl";

    Result result =
        instanceOf(UpsellController.class)
            .considerRegister(
                fakeRequest(),
                applicant.id,
                commonIntakeForm.id(),
                application.id,
                redirectLocation)
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Programs");
    assertThat(contentAsString(result)).contains(programDefinition.localizedName().getDefault());
    assertThat(contentAsString(result)).contains("Create account");
  }

  @Test
  public void download_authenticatedApplicant() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          instanceOf(UpsellController.class)
              .download(fakeRequest(), application.id, applicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void download_authenticatedTI() {
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    profileFactory.createFakeTrustedIntermediary();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(managedApplicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          instanceOf(UpsellController.class)
              .download(fakeRequest(), application.id, managedApplicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void download_unauthorizedTI() {
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel unmanagedApplicant = createApplicant();
    ApplicantModel managedApplicant = createApplicant();
    createTIWithMockedProfile(managedApplicant);
    profileFactory.createFakeTrustedIntermediary();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(managedApplicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          instanceOf(UpsellController.class)
              .download(fakeRequest(), application.id, unmanagedApplicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void download_invalidApplicantID() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel application =
        resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          instanceOf(UpsellController.class)
              .download(fakeRequest(), application.id, 0)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void download_invalidApplicationID() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    resourceCreator.insertActiveApplication(applicant, programDefinition.toProgram());

    Result result;
    try {
      result =
          instanceOf(UpsellController.class)
              .download(fakeRequest(), 0, applicant.id)
              .toCompletableFuture()
              .join();
    } catch (ProgramNotFoundException e) {
      throw new RuntimeException(e);
    }
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }
}
