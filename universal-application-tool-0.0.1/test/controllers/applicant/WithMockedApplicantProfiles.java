package controllers.applicant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static play.inject.Bindings.bind;

import auth.ProfileFactory;
import auth.ProfileUtils;
import auth.UatProfile;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.Account;
import models.Applicant;
import org.junit.BeforeClass;
import org.mockito.Mockito;
import play.inject.Injector;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import support.ResourceCreator;
import support.TestConstants;

public class WithMockedApplicantProfiles {

  private static final ProfileUtils MOCK_UTILS = Mockito.mock(ProfileUtils.class);

  private static Injector injector;
  private static ResourceCreator resourceCreator;
  private static ProfileFactory profileFactory;

  @BeforeClass
  public static void setupInjector() {
    injector =
        new GuiceApplicationBuilder()
            .configure(TestConstants.TEST_DATABASE_CONFIG)
            .overrides(bind(ProfileUtils.class).toInstance(MOCK_UTILS))
            .build()
            .injector();
    resourceCreator = new ResourceCreator(injector);
    profileFactory = injector.instanceOf(ProfileFactory.class);
  }

  protected <T> T instanceOf(Class<T> clazz) {
    return injector.instanceOf(clazz);
  }

  protected ResourceCreator resourceCreator() {
    return resourceCreator;
  }

  protected Applicant createApplicantWithMockedProfile() {
    Applicant applicant = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccount();

    account.setApplicants(ImmutableList.of(applicant));
    account.save();
    applicant.setAccount(account);
    applicant.save();

    UatProfile profile = profileFactory.wrap(applicant);
    mockProfile(profile);
    return applicant;
  }

  private void mockProfile(UatProfile profile) {
    when(MOCK_UTILS.currentUserProfile(any(Http.Request.class))).thenReturn(Optional.of(profile));
  }
}
