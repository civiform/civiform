package modules;

import org.pac4j.core.config.Config;
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

  public CiviFormLogoutLogic() {}

  @Override
  public Object perform(
      Config config,
      String inputLogoutUrlPattern,
      String otehrstring,
      Boolean inputLocalLogout,
      Boolean inputDestroySession,
      Boolean inputCentralLogout,
      FrameworkParameters frameworkParameters) {

    try {
      Object result =
          super.perform(
              config,
              inputLogoutUrlPattern,
              otehrstring,
              inputLocalLogout,
              inputDestroySession,
              inputCentralLogout,
              frameworkParameters);

      // TODO(#6975): Remove the session ID from the database at logout

      return result;
    } catch (RuntimeException e) {
      // If the default logout logic throws an exception, log the error and re-throw the exception.
      logger.error("Error during logout", e);
      throw e;
    }
  }
}
