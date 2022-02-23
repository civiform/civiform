package controllers.admin;

import auth.Authorizers;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import views.admin.versions.VersionListView;

/** Controller for handling methods for admins managing versions. */
public class AdminVersionController extends Controller {
  private final VersionRepository versionRepository;
  private final VersionListView versionListView;

  @Inject
  public AdminVersionController(
      VersionRepository versionRepository, VersionListView versionListView) {
    this.versionRepository = versionRepository;
    this.versionListView = versionListView;
  }

  /** Return a HTML page displaying all current and past verions. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    return ok(versionListView.render(versionRepository.listAllVersions(), request));
  }

  /** POST endpoint for setting a certain version to live. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result setVersionLive(long versionId, Http.Request request) {
    versionRepository.setLive(versionId);
    return redirect(routes.AdminVersionController.index());
  }
}
