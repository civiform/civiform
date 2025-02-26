package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileUtils;
import java.time.Clock;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Http;
import play.mvc.Result;
import services.settings.SettingsManifest;

public class SessionControllerTest {
  private SessionController controller;
  private ProfileUtils profileUtils;
  private CiviFormProfileData mockProfileData;
  private CiviFormProfile mockProfile;
  private SettingsManifest mockSettingsManifest;
  private Clock clock;

  @Before
  public void setUp() throws Exception {
    mockProfile = Mockito.mock(CiviFormProfile.class);
    mockProfileData = Mockito.mock(CiviFormProfileData.class);
    profileUtils = Mockito.mock(ProfileUtils.class);
    mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    clock = mock(Clock.class);
    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(profileUtils.optionalCurrentUserProfile(any(Http.Request.class)))
        .thenReturn(Optional.of(mockProfile));

    when(mockSettingsManifest.getSessionTimeoutEnabled()).thenReturn(true);

    controller = new SessionController(profileUtils, mockSettingsManifest, clock);
  }

  @Test
  public void testExtendSessionWithValidRequest() {
    Http.Request request =
        fakeRequestBuilder().header("Authorization", "Bearer valid_token").build();

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.OK);
    assertThat(contentAsString(result)).contains("Session extended");

    verify(mockProfileData).updateLastActivityTime(clock);
  }

  @Test
  public void testExtendSessionWhenTimeoutNotEnabled() {
    when(mockSettingsManifest.getSessionTimeoutEnabled()).thenReturn(false);

    Http.Request request =
        fakeRequestBuilder().header("Authorization", "Bearer valid_token").build();

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.BAD_REQUEST);
    assertThat(contentAsString(result)).contains("Session timeout is not enabled");
  }
}
