package actions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.settings.SettingsManifest;

/**
 * Action that ensures the demo mode is enabled before allowing certain actions.
 *
 * <p>The action will redirect the request to the home page if the the action is disabled.
 *
 * <p>
 */
public class DemoModeDisabledAction extends Action.Simple {
  private static final Logger logger = LoggerFactory.getLogger(DemoModeDisabledAction.class);
  private final SettingsManifest settingsManifest;

  @Inject
  public DemoModeDisabledAction(SettingsManifest settingsManifest) {
    this.settingsManifest = settingsManifest;
  }

  @Override
  public CompletionStage<Result> call(Request req) {

    if (settingsManifest.getStagingDisableDemoModeLogins(req)) {
      logger.warn(
          "There was an attempt to navigate to a dev tools action when demo mode was disabled");
      return CompletableFuture.completedFuture(
          redirect(controllers.routes.HomeController.index().url()));
    }

    return delegate.call(req); // continute processing next step
  }
}
