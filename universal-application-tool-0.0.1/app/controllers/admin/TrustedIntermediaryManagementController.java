package controllers.admin;

import auth.Authorizers;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Controller;
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
  public Result index() {
    return ok(listView.render(userRepository.listTrustedIntermediaryGroups()));
  }
}
