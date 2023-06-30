package controllers;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.CfTestHelpers.requestBuilderWithSettings;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.applicant.RedirectController;
import java.util.List;
import java.util.Locale;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultSecurityLogic;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import repository.UserRepository;
import repository.VersionRepository;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantService;
import services.applicant.question.Scalar;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.program.predicate.PredicateValue;
import support.ProgramBuilder;
import support.QuestionAnswerer;
import views.applicant.ApplicantCommonIntakeUpsellCreateAccountView;
import views.applicant.ApplicantUpsellCreateAccountView;

public class RedirectControllerTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
    // Get the config, and hack it so that all requests appear authorized.
    Config config = instanceOf(Config.class);
    AnonymousClient client = AnonymousClient.INSTANCE;
    config.setClients(new Clients(client));

    // The SecurityLogic wants to use some smarts to figure out which client to use, but
    // those smarts are never going to pick this client (since none of the endpoints are
    // configured to use it), so we implement an anonymous client finder which always returns
    // our client.
    DefaultSecurityLogic securityLogic = new DefaultSecurityLogic();
    securityLogic.setClientFinder(
        new ClientFinder() {
          @Override
          public List<Client> find(Clients clients, WebContext context, String clientNames) {
            return ImmutableList.of(client);
          }
        });
    config.setSecurityLogic(securityLogic);
  }

  @Test
  public void programBySlug_redirectsToPreviousProgramVersionForExistingApplications() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    Application app =
        new Application(applicant, programDefinition.toProgram(), LifecycleStage.DRAFT);
    app.save();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    Result result =
        instanceOf(RedirectController.class)
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
  }

  @Test
  public void programBySlug_clearsOutRedirectSessionKey_existingProgram() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    Result result =
        instanceOf(RedirectController.class)
            .programBySlug(
                addCSRFToken(
                        requestBuilderWithSettings()
                            .session(REDIRECT_TO_SESSION_KEY, "redirect-url"))
                    .build(),
                programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isNotPresent();
  }

  @Test
  public void programBySlug_clearsOutRedirectSessionKey_nonExistingProgram() {
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();

    Result result =
        instanceOf(RedirectController.class)
            .programBySlug(
                addCSRFToken(
                        requestBuilderWithSettings()
                            .session(REDIRECT_TO_SESSION_KEY, "redirect-url"))
                    .build(),
                "non-existing-program-slug")
            .toCompletableFuture()
            .join();

    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isNotPresent();
  }

  @Test
  public void programBySlug_testLanguageSelectorShown() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    Applicant applicant = createApplicantWithMockedProfile();
    RedirectController controller = instanceOf(RedirectController.class);
    Result result =
        controller
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantInformationController.setLangFromBrowser(
                    applicant.id)
                .url());
  }

  @Test
  public void programBySlug_testLanguageSelectorNotShownOneLanguage() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    RedirectController controller =
        new RedirectController(
            instanceOf(HttpExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            instanceOf(ApplicantUpsellCreateAccountView.class),
            instanceOf(ApplicantCommonIntakeUpsellCreateAccountView.class),
            instanceOf(MessagesApi.class),
            instanceOf(VersionRepository.class),
            languageUtils);
    Result result =
        controller
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
  }

  @Test
  public void programBySlug_testLanguageSelectorNotShownNoLanguage() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    RedirectController controller =
        new RedirectController(
            instanceOf(HttpExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            instanceOf(ApplicantUpsellCreateAccountView.class),
            instanceOf(ApplicantCommonIntakeUpsellCreateAccountView.class),
            instanceOf(MessagesApi.class),
            instanceOf(VersionRepository.class),
            languageUtils);
    Result result =
        controller
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
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
        instanceOf(RedirectController.class)
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
        instanceOf(RedirectController.class)
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
        instanceOf(RedirectController.class)
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
