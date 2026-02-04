package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ClientIpResolver;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.AccountModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.AccountRepository;
import services.settings.SettingsManifest;

/**
 * This controller handles back channel logout requests from the identity provider. For example, if
 * a user resets their password, the government can choose to send a back channel logout request to
 * CiviForm.
 */
public class LogoutAllSessionsController extends Controller {

  private static final Logger logger = LoggerFactory.getLogger(LogoutAllSessionsController.class);
  private final ProfileUtils profileUtils;
  private final AccountRepository accountRepository;
  private final ClientIpResolver clientIpResolver;
  private final SettingsManifest settingsManifest;

  @Inject
  public LogoutAllSessionsController(
      ProfileUtils profileUtils,
      AccountRepository accountRepository,
      SettingsManifest settingsManifest,
      ClientIpResolver clientIpResolver) {

    this.profileUtils = checkNotNull(profileUtils);
    this.accountRepository = checkNotNull(accountRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.clientIpResolver = checkNotNull(clientIpResolver);
  }

  public CompletionStage<Result> index(Http.Request request) {
    Optional<CiviFormProfile> optionalProfile = profileUtils.optionalCurrentUserProfile(request);
    try {
      if (optionalProfile.isPresent()) {
        CiviFormProfile profile = optionalProfile.get();
        profile
            .getAccount()
            .thenAccept(
                account -> {
                  logger.debug("Found account for back channel logout: {}", account.id);
                  account.clearActiveSessions();
                  account.save();
                })
            .exceptionally(
                e -> {
                  logger.error(e.getMessage(), e);
                  return null;
                });
      } else {
        logger.warn("No account found for back channel logout");
      }
    } catch (RuntimeException e) {
      logger.error("Error clearing session from account", e);
    }

    // Redirect to the landing page
    return CompletableFuture.completedFuture(redirect(routes.HomeController.index().url()));
  }

  public CompletionStage<Result> logoutFromAuthorityId(Http.Request request, String authorityId) {
    Optional<ImmutableList<String>> allowedIpAddresses =
        settingsManifest.getAllowedIpAddressesForLogout();
    String clientIp = clientIpResolver.resolveClientIp(request);
    if (allowedIpAddresses.isPresent()
        && !allowedIpAddresses.get().isEmpty()
        && !allowedIpAddresses.get().contains(clientIp)) {
      logger.warn("Unauthorized logout attempt from IP address: {}", clientIp);
      return CompletableFuture.completedFuture(redirect(routes.HomeController.index().url()));
    }
    try {
      Optional<AccountModel> maybeAccount =
          accountRepository.lookupAccountByAuthorityId(authorityId);

      if (maybeAccount.isPresent()) {
        AccountModel account = maybeAccount.get();
        logger.debug("Found account for back channel logout: {}", account.id);
        account.clearActiveSessions();
        account.save();
      } else {
        logger.warn("No account found for back channel logout with authority ID");
      }

    } catch (RuntimeException e) {
      logger.error("Error clearing session from account", e);
    }

    // Redirect to the landing page
    return CompletableFuture.completedFuture(redirect(routes.HomeController.index().url()));
  }
}
