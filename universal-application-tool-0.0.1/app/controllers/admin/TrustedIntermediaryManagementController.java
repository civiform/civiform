package controllers.admin;

import auth.Authorizers;
import com.google.common.base.Preconditions;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;
import views.admin.ti.TrustedIntermediaryGroupListView;

public class TrustedIntermediaryManagementController extends Controller {
  private final TrustedIntermediaryGroupListView listView;
  private final UserRepository userRepository;

  @Inject
  public TrustedIntermediaryManagementController(
      TrustedIntermediaryGroupListView listView, UserRepository userRepository) {
    this.listView = listView;
    this.userRepository = userRepository;
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result index(Http.Request request) {
    return ok(listView.render(userRepository.listTrustedIntermediaryGroups(), request));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result newGroup(Http.Request request) {
    String[] name = request.body().asFormUrlEncoded().get("name");
    String[] description = request.body().asFormUrlEncoded().get("description");
    Preconditions.checkState(name.length == 1, "Need exactly one `name` form entry.");
    Preconditions.checkState(description.length == 1, "Need exactly one `name` form entry.");
    userRepository.createNewTrustedIntermediaryGroup(name[0], description[0]);

    return redirect(routes.TrustedIntermediaryManagementController.index());
  }
}
