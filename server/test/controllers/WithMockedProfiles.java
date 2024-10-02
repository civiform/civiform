package controllers;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static play.inject.Bindings.bind;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.ProfileUtils;
import java.util.Collections;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.LifecycleStage;
import models.ProgramModel;
import models.TrustedIntermediaryGroupModel;
import models.VersionModel;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import play.Application;
import play.inject.Injector;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.test.Helpers;
import support.ProgramBuilder;
import support.ResourceCreator;
import support.TestQuestionBank;

public class WithMockedProfiles {

  private static final ProfileUtils MOCK_UTILS = Mockito.mock(ProfileUtils.class);

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(true);

  protected static final String skipUserProfile = "skipUserProfile";

  private static Injector injector;
  private static ProfileFactory profileFactory;
  protected static ResourceCreator resourceCreator;
  protected static Application app;

  @BeforeClass
  public static void setupInjector() {
    app =
        new GuiceApplicationBuilder()
            .overrides(bind(ProfileUtils.class).toInstance(MOCK_UTILS))
            .build();
    setupInjectorForApp(app);
  }

  @AfterClass
  public static void stopApp() {
    if (app != null) {
      Helpers.stop(app);
      app = null;
    }
  }

  public static void setupInjectorWithExtraBinding(play.api.inject.Binding<?> additionalBinding) {
    stopApp();
    app =
        new GuiceApplicationBuilder()
            .overrides(bind(ProfileUtils.class).toInstance(MOCK_UTILS), additionalBinding)
            .build();
    setupInjectorForApp(app);
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
    VersionModel newActiveVersion = new VersionModel(LifecycleStage.ACTIVE);
    newActiveVersion.save();
  }

  protected ApplicantModel createApplicant() {
    ApplicantModel applicant = resourceCreator.insertApplicant();
    AccountModel account = resourceCreator.insertAccount();

    applicant.setAccount(account);
    applicant.save();
    return applicant;
  }

  protected ApplicantModel createApplicantWithMockedProfile() {
    ApplicantModel applicant = createApplicant();
    CiviFormProfile profile = profileFactory.wrap(applicant);
    mockProfile(profile);

    when(MOCK_UTILS.getApplicantId(not(argThat(skipUserProfile()))))
        .thenReturn(Optional.of(applicant.id));

    return applicant;
  }

  protected void resetMocks() {
    Mockito.reset(MOCK_UTILS);
  }

  protected AccountModel createTIWithMockedProfile(ApplicantModel managedApplicant) {
    AccountModel ti = resourceCreator.insertAccount();

    TrustedIntermediaryGroupModel group = resourceCreator.insertTrustedIntermediaryGroup();
    AccountModel managedAccount = managedApplicant.getAccount();
    managedAccount.setManagedByGroup(group);
    ApplicantModel tiApplicant = resourceCreator.insertApplicant();
    ti.setApplicants(Collections.singletonList(tiApplicant));
    tiApplicant.setAccount(ti);
    tiApplicant.save();
    managedAccount.save();
    ti.setMemberOfGroup(group);
    ti.save();
    CiviFormProfile profile = profileFactory.wrap(ti);
    mockProfile(profile);
    return ti;
  }

  protected AccountModel createProgramAdminWithMockedProfile(ProgramModel program) {
    AccountModel programAdmin = resourceCreator.insertAccount();

    programAdmin.addAdministeredProgram(program.getProgramDefinition());
    programAdmin.save();

    CiviFormProfile profile = profileFactory.wrap(programAdmin);
    mockProfile(profile);
    return programAdmin;
  }

  protected AccountModel createGlobalAdminWithMockedProfile() {
    CiviFormProfile profile = profileFactory.wrapProfileData(profileFactory.createNewAdmin());
    mockProfile(profile);

    AccountModel adminAccount = profile.getAccount().join();

    ApplicantModel applicant = resourceCreator.insertApplicant();
    applicant.setAccount(adminAccount);
    applicant.save();

    return adminAccount;
  }

  private static void setupInjectorForApp(Application app) {
    injector = app.injector();
    resourceCreator = new ResourceCreator(injector);
    Helpers.start(app);
    profileFactory = injector.instanceOf(ProfileFactory.class);
    ProgramBuilder.setInjector(injector);
  }

  private ArgumentMatcher<Http.Request> skipUserProfile() {
    return new HasBooleanHeaderArgumentMatcher(skipUserProfile);
  }

  private void mockProfile(CiviFormProfile profile) {
    when(MOCK_UTILS.optionalCurrentUserProfile(not(argThat(skipUserProfile()))))
        .thenReturn(Optional.of(profile));
    when(MOCK_UTILS.currentUserProfile(not(argThat(skipUserProfile())))).thenReturn(profile);
  }
}
