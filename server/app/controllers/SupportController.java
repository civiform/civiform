package controllers;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.DeploymentType;
import views.support.UnsupportedBrowserPageView;

public final class SupportController extends Controller {
  private final UnsupportedBrowserPageView unsupportedBrowserPageView;
  private final DeploymentType deploymentType;

  @Inject
  public SupportController(
      UnsupportedBrowserPageView unsupportedBrowserPageView, DeploymentType deploymentType) {
    this.unsupportedBrowserPageView = checkNotNull(unsupportedBrowserPageView);
    this.deploymentType = checkNotNull(deploymentType);
  }

  public Result handleUnsupportedBrowser(Http.Request request) {
    return ok(unsupportedBrowserPageView.render(
            request, new UnsupportedBrowserPageView.Model(deploymentType.isDevOrStaging())))
        .as(Http.MimeTypes.HTML);
  }
}
