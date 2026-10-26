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
 * Returns a 404 unless the images in questions feature is enabled. See {@link
 * FileUploadImprovementsEnabledAction}.
 */
public class QuestionImagesEnabledAction extends Action.Simple {
  private final SettingsManifest settingsManifest;

  @Inject
  public QuestionImagesEnabledAction(SettingsManifest settingsManifest) {
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Override
  public CompletionStage<Result> call(Request req) {
    if (!settingsManifest.getImagesInQuestionFeatureEnabled(req)) {
      return CompletableFuture.completedFuture(notFound());
    }
    return delegate.call(req);
  }
}
