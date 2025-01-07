package modules;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import java.util.Optional;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends the default pac4j logout logic to include removing the active session from the
 * database after logout is successful.
 */
class CiviFormLogoutLogic extends DefaultLogoutLogic {
  private static final Logger logger = LoggerFactory.getLogger(CiviFormLogoutLogic.class);
  private final ProfileUtils profileUtils;

  public CiviFormLogoutLogic(ProfileUtils profileUtils) {
    this.profileUtils = profileUtils;
  }

  @Override
  public Object perform(
      final Config config,
      final String defaultUrl,
      final String inputLogoutUrlPattern,
      final Boolean inputLocalLogout,
      final Boolean inputDestroySession,
      final Boolean inputCentralLogout,
      final FrameworkParameters parameters) {

    try {
      Object result =
          super.perform(
              config,
              defaultUrl,
              inputLogoutUrlPattern,
              inputLocalLogout,
              inputDestroySession,
              inputCentralLogout,
              parameters);

      // Remove the session ID from the database
      try {
        CallContext callContext = buildContext(config, parameters);
        Optional<CiviFormProfile> maybeProfile =
            profileUtils.optionalCurrentUserProfile(callContext.webContext());
        if (maybeProfile.isPresent()) {
          CiviFormProfile profile = maybeProfile.get();
          var unused =
              profile
                  .getAccount()
                  .thenAccept(
                      account -> {
                        account.removeActiveSession(profile.getProfileData().getSessionId());
                        account.save();
                      });
        }
      } catch (RuntimeException e) {
        logger.error("Error clearing session from account", e);
      }

      return result;
    } catch (RuntimeException e) {
      // If the default logout logic throws an exception, log the error and re-throw the exception.
      logger.error("Error during logout", e);
      throw e;
    }
  }
}
