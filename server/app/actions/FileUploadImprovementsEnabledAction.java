package actions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import play.mvc.Action;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.settings.SettingsManifest;

/**
 * Action that returns a 404 unless the file upload question improvements feature is enabled.
 *
 * <p>Apply this to routes that use a streaming upload body parser, together with {@code
 * deferBodyParsing} in {@code conf/routes}. Play runs a route's body parser before action
 * composition unless the route defers body parsing, and the parser writes the file to cloud storage
 * as it reads it. With deferral, this check runs before any bytes are read.
 */
public class FileUploadImprovementsEnabledAction extends Action.Simple {
  private final SettingsManifest settingsManifest;

  @Inject
  public FileUploadImprovementsEnabledAction(SettingsManifest settingsManifest) {
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Override
  public CompletionStage<Result> call(Request req) {
    if (!settingsManifest.getFileUploadQuestionImprovementsEnabled(req)) {
      return CompletableFuture.completedFuture(notFound());
    }
    return delegate.call(req);
  }
}
