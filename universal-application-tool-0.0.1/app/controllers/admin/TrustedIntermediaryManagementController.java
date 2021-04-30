package controllers.admin;

import auth.Authorizers;
import com.google.common.base.Strings;
import forms.CreateTrustedIntermediaryGroupForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;
import views.admin.ti.TrustedIntermediaryGroupListView;

public class TrustedIntermediaryManagementController extends Controller {
  private final TrustedIntermediaryGroupListView listView;
  private final UserRepository userRepository;
  private final FormFactory formFactory;

  @Inject
  public TrustedIntermediaryManagementController(
      TrustedIntermediaryGroupListView listView,
      UserRepository userRepository,
      FormFactory formFactory) {
    this.listView = listView;
    this.userRepository = userRepository;
    this.formFactory = formFactory;
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result index(Http.Request request) {
    LoggerFactory.getLogger(TrustedIntermediaryManagementController.class)
        .info(request.flash().data().toString());
    return ok(listView.render(userRepository.listTrustedIntermediaryGroups(), request));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result create(Http.Request request) {
    Form<CreateTrustedIntermediaryGroupForm> form =
        formFactory.form(CreateTrustedIntermediaryGroupForm.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return flashFieldValuesWithError(form.errors().get(0).toString(), form);
    }
    if (Strings.isNullOrEmpty(form.get().getName())) {
      return flashFieldValuesWithError("Must provide group name.", form);
    }
    if (Strings.isNullOrEmpty(form.get().getDescription())) {
      return flashFieldValuesWithError("Must provide group description.", form);
    }
    userRepository.createNewTrustedIntermediaryGroup(
        form.get().getName(), form.get().getDescription());

    return redirect(routes.TrustedIntermediaryManagementController.index());
  }

  private Result flashFieldValuesWithError(
      String error, Form<CreateTrustedIntermediaryGroupForm> form) {
    Result result =
        redirect(routes.TrustedIntermediaryManagementController.index()).flashing("error", error);
    if (form.value().isPresent()) {
      result = result.flashing("providedName", form.value().get().getName());
      result = result.flashing("providedDescription", form.value().get().getDescription());
    }
    return result;
  }
}
