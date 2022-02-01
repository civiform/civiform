package controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static play.inject.Bindings.bind;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.ProfileUtils;
import java.util.Optional;
import models.Account;
import models.Applicant;
import models.LifecycleStage;
import models.Program;
import models.TrustedIntermediaryGroup;
import models.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import play.Application;
import play.inject.Injector;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.test.Helpers;
import support.ProgramBuilder;
import support.ResourceCreator;
import support.TestConstants;
import support.TestQuestionBank;

public class WithMockedProfiles {

  private static final ProfileUtils MOCK_UTILS = Mockito.mock(ProfileUtils.class);

  private static TestQuestionBank testQuestionBank = new TestQuestionBank(true);

  private static Injector injector;
  private static ResourceCreator resourceCreator;
  private static ProfileFactory profileFactory;
  protected static Application app;

  @BeforeClass
  public static void setupInjector() {
    app =
        new GuiceApplicationBuilder()
            .configure(TestConstants.TEST_DATABASE_CONFIG)
            .overrides(bind(ProfileUtils.class).toInstance(MOCK_UTILS))
            .build();
    injector = app.injector();
    resourceCreator = new ResourceCreator(injector);
    Helpers.start(app);
    profileFactory = injector.instanceOf(ProfileFactory.class);
    ProgramBuilder.setInjector(injector);
  }

  @AfterClass
  public static void stopApp() {
    if (app != null) {
      Helpers.stop(app);
      app = null;
    }
  }

  protected <T> T instanceOf(Class<T> clazz) {
    return injector.instanceOf(clazz);
  }

  protected ResourceCreator resourceCreator() {
    return resourceCreator;
  }

  protected TestQuestionBank testQuestionBank() {
    return testQuestionBank;
  }

  protected void resetDatabase() {
    testQuestionBank().reset();
    resourceCreator().truncateTables();
    Version newActiveVersion = new Version(LifecycleStage.ACTIVE);
    newActiveVersion.save();
  }

  protected Applicant createApplicant() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccount();

    applicant.setAccount(account);
    applicant.save();
    return applicant;
  }

  protected Applicant createApplicantWithMockedProfile() {
    Applicant applicant = createApplicant();
    CiviFormProfile profile = profileFactory.wrap(applicant);
    mockProfile(profile);
    return applicant;
  }

  protected Account createTIWithMockedProfile(Applicant managedApplicant) {
    Account ti = resourceCreator.insertAccount();

    TrustedIntermediaryGroup group = resourceCreator.insertTrustedIntermediaryGroup();
    Account managedAccount = managedApplicant.getAccount();
    managedAccount.setManagedByGroup(group);
    managedAccount.save();
    ti.setMemberOfGroup(group);
    ti.save();

    CiviFormProfile profile = profileFactory.wrap(ti);
    mockProfile(profile);
    return ti;
  }

  protected Account createProgramAdminWithMockedProfile(Program program) {
    Account programAdmin = resourceCreator.insertAccount();

    programAdmin.addAdministeredProgram(program.getProgramDefinition());
    programAdmin.save();

    CiviFormProfile profile = profileFactory.wrap(programAdmin);
    mockProfile(profile);
    return programAdmin;
  }

  protected Account createGlobalAdminWithMockedProfile() {
    Account globalAdmin = resourceCreator.insertAccount();

    globalAdmin.setGlobalAdmin(true);
    globalAdmin.save();

    CiviFormProfile profile = profileFactory.wrap(globalAdmin);
    mockProfile(profile);
    return globalAdmin;
  }

  private void mockProfile(CiviFormProfile profile) {
    when(MOCK_UTILS.currentUserProfile(any(Http.Request.class))).thenReturn(Optional.of(profile));
  }
}
