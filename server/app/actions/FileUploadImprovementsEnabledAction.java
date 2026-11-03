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
 * Returns a 404 unless the file upload question improvements feature is enabled. Apply it with
 * {@code deferBodyParsing} so the check runs before any of the upload is read.
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
