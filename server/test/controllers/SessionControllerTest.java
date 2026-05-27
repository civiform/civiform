package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileUtils;
import java.time.Clock;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;

public class SessionControllerTest {
  private SessionController controller;
  private ProfileUtils profileUtils;
  private CiviFormProfileData mockProfileData;
  private Clock clock;

  @Before
  public void setUp() {
    profileUtils = mock(ProfileUtils.class);
    clock = mock(Clock.class);
    CiviFormProfile mockProfile = mock(CiviFormProfile.class);
    mockProfileData = mock(CiviFormProfileData.class);

    when(mockProfile.getProfileData()).thenReturn(mockProfileData);
    when(profileUtils.optionalCurrentUserProfile(any(Http.Request.class)))
        .thenReturn(Optional.of(mockProfile));
    controller = new SessionController(profileUtils, clock);
  }

  @Test
  public void extendSession_withValidRequest_updatesLastActivityTime() {
    Http.Request request = fakeRequestBuilder().build();

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.OK);
    verify(mockProfileData).updateLastSessionActivityTime(clock);
  }

  @Test
  public void extendSession_withNoProfile_returnsUnauthorized() {
    Http.Request request = fakeRequestBuilder().build();

    when(profileUtils.optionalCurrentUserProfile(any(Http.Request.class)))
        .thenReturn(Optional.empty());

    Result result = controller.extendSession(request);

    assertThat(result.status()).isEqualTo(Http.Status.UNAUTHORIZED);
    verify(mockProfileData, never()).updateLastSessionActivityTime(any());
  }
}
