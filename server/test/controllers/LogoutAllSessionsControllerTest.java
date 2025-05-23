package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.ClientIpResolver;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import models.AccountModel;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import repository.AccountRepository;
import services.settings.SettingsManifest;

public class LogoutAllSessionsControllerTest extends WithMockedProfiles {
  private LogoutAllSessionsController controller;
  private CiviFormProfile testProfile;
  private ProfileUtils profileUtils;
  private AccountRepository accountRepository;
  private ClientIpResolver clientIpResolver;
  private SettingsManifest settingsManifest;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);

    profileUtils = mock(ProfileUtils.class);
    testProfile = mock(CiviFormProfile.class);
    clientIpResolver = mock(ClientIpResolver.class);
    settingsManifest = mock(SettingsManifest.class);

    when(profileUtils.optionalCurrentUserProfile(any(Http.RequestHeader.class)))
        .thenReturn(Optional.of(testProfile));

    controller =
        new LogoutAllSessionsController(
            profileUtils, accountRepository, settingsManifest, clientIpResolver);
  }

  @Test
  public void testIndexWithNoProfile() {
    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "true").build();
    Result result = controller.index(request).toCompletableFuture().join();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testIndexWithProfile_withSession() throws InterruptedException, ExecutionException {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    ApplicantModel applicant = createApplicantWithMockedProfile();
    AccountModel account = applicant.getAccount();
    account.addActiveSession("fake session", clock);
    account.save();

    when(testProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(account));

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();

    CompletionStage<Result> resultStage = controller.index(request);
    Result result = resultStage.toCompletableFuture().get();

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testIndexWithProfile_withMultipleSessions()
      throws InterruptedException, ExecutionException {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    ApplicantModel applicant = createApplicantWithMockedProfile();
    AccountModel account = applicant.getAccount();
    account.addActiveSession("session1", clock);
    account.addActiveSession("session2", clock);
    account.save();

    when(testProfile.getAccount()).thenReturn(CompletableFuture.completedFuture(account));

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();

    CompletionStage<Result> resultStage = controller.index(request);
    Result result = resultStage.toCompletableFuture().get();

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testLogoutWithAuthorityId_withSession()
      throws InterruptedException, ExecutionException {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    AccountModel account = new AccountModel();
    account.addActiveSession("fake session", clock);
    account.save();
    String authorityId = setAndGetEncodedAuthorityId(account);

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();

    CompletionStage<Result> resultStage = controller.logoutFromAuthorityId(request, authorityId);
    Result result = resultStage.toCompletableFuture().get();

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testLogoutWithAuthorityId_withMultipleSessions()
      throws InterruptedException, ExecutionException {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    AccountModel account = new AccountModel();
    account.addActiveSession("session1", clock);
    account.addActiveSession("session2", clock);
    account.save();
    String authorityId = setAndGetEncodedAuthorityId(account);

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();

    CompletionStage<Result> resultStage = controller.logoutFromAuthorityId(request, authorityId);
    Result result = resultStage.toCompletableFuture().get();

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testLogoutWithAuthorityId_withAllowedIps_allowed()
      throws InterruptedException, ExecutionException {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    AccountModel account = new AccountModel();
    account.addActiveSession("session1", clock);
    account.addActiveSession("session2", clock);
    account.save();
    String authorityId = setAndGetEncodedAuthorityId(account);

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();
    when(settingsManifest.getAllowedIpAddressesForLogout())
        .thenReturn(Optional.of(ImmutableList.of("allowed.ip")));
    when(clientIpResolver.resolveClientIp(request)).thenReturn("allowed.ip");

    CompletionStage<Result> resultStage = controller.logoutFromAuthorityId(request, authorityId);
    Result result = resultStage.toCompletableFuture().get();

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  @Test
  public void testLogoutWithAuthorityId_withAllowedIps_notAllowed()
      throws InterruptedException, ExecutionException {
    // Add active session to account
    Clock clock = Clock.fixed(Instant.ofEpochSecond(10), ZoneOffset.UTC);
    AccountModel account = new AccountModel();
    account.addActiveSession("session1", clock);
    account.addActiveSession("session2", clock);
    account.save();
    String authorityId = setAndGetEncodedAuthorityId(account);

    Http.Request request = fakeRequestBuilder().header(skipUserProfile, "false").build();
    when(settingsManifest.getAllowedIpAddressesForLogout())
        .thenReturn(Optional.of(ImmutableList.of("allowed.ip")));
    when(clientIpResolver.resolveClientIp(request)).thenReturn("not.allowed.ip");

    CompletionStage<Result> resultStage = controller.logoutFromAuthorityId(request, authorityId);
    Result result = resultStage.toCompletableFuture().get();

    AccountModel updatedAccount = accountRepository.lookupAccount(account.id).get();
    assertThat(updatedAccount.getActiveSessions()).isNotEmpty();
    assertThat(result.redirectLocation()).isEqualTo(Optional.of("/"));
  }

  private String setAndGetEncodedAuthorityId(AccountModel account) {
    // Set authority ID with account ID to ensure uniqueness
    String authorityId = "iss: auth0 sub:" + account.id.toString();
    account.setAuthorityId(authorityId);
    account.save();
    return authorityId;
  }
}
