package controllers.admin;

import auth.Authorizers;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import forms.AddTrustedIntermediaryForm;
import forms.CreateTrustedIntermediaryGroupForm;
import forms.RemoveTrustedIntermediaryForm;
import java.util.Optional;
import javax.inject.Inject;
import models.TrustedIntermediaryGroup;
import org.pac4j.play.java.Secure;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;
import services.ti.NoSuchTrustedIntermediaryError;
import services.ti.NoSuchTrustedIntermediaryGroupError;
import views.admin.ti.EditTrustedIntermediaryGroupView;
import views.admin.ti.TrustedIntermediaryGroupListView;

/** Controller for admins to manage trusted intermediaries of programs. */
public class TrustedIntermediaryManagementController extends Controller {
  private final TrustedIntermediaryGroupListView listView;
  private final UserRepository userRepository;
  private final FormFactory formFactory;
  private final EditTrustedIntermediaryGroupView editView;

  @Inject
  public TrustedIntermediaryManagementController(
      TrustedIntermediaryGroupListView listView,
      EditTrustedIntermediaryGroupView editView,
      UserRepository userRepository,
      FormFactory formFactory) {
    this.listView = Preconditions.checkNotNull(listView);
    this.userRepository = Preconditions.checkNotNull(userRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.editView = Preconditions.checkNotNull(editView);
  }

  /** Return a HTML page displaying all trusted intermediary groups. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    LoggerFactory.getLogger(TrustedIntermediaryManagementController.class)
        .info(request.flash().data().toString());
    return ok(listView.render(userRepository.listTrustedIntermediaryGroups(), request));
  }

  /** POST endpoint for creating a new trusted intermediary group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Http.Request request) {
    Form<CreateTrustedIntermediaryGroupForm> form =
        formFactory.form(CreateTrustedIntermediaryGroupForm.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return flashCreateTIFieldValuesWithError(form.errors().get(0).toString(), form);
    }
    if (Strings.isNullOrEmpty(form.get().getName())) {
      return flashCreateTIFieldValuesWithError("Must provide group name.", form);
    }
    if (Strings.isNullOrEmpty(form.get().getDescription())) {
      return flashCreateTIFieldValuesWithError("Must provide group description.", form);
    }
    userRepository.createNewTrustedIntermediaryGroup(
        form.get().getName(), form.get().getDescription());

    return redirect(routes.TrustedIntermediaryManagementController.index());
  }

  private Result flashCreateTIFieldValuesWithError(
      String error, Form<CreateTrustedIntermediaryGroupForm> form) {
    Result result =
        redirect(routes.TrustedIntermediaryManagementController.index()).flashing("error", error);
    if (form.value().isPresent()) {
      result = result.flashing("providedName", form.value().get().getName());
      result = result.flashing("providedDescription", form.value().get().getDescription());
    }
    return result;
  }

  private Result flashAddTIFieldValuesWithError(
      String error, Form<AddTrustedIntermediaryForm> form, long id) {
    Result result =
        redirect(routes.TrustedIntermediaryManagementController.edit(id)).flashing("error", error);
    if (form.value().isPresent()) {
      result = result.flashing("providedEmailAddress", form.value().get().getEmailAddress());
    }
    return result;
  }

  /** Return a HTML page displaying all trusted intermediaries in the specified group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(long id, Http.Request request) {
    Optional<TrustedIntermediaryGroup> tiGroup = userRepository.getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      return notFound("no such group.");
    }
    return ok(editView.render(tiGroup.get(), request));
  }

  /** POST endpoint for deleting a trusted intermediary group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result delete(long id, Http.Request request) {
    try {
      userRepository.deleteTrustedIntermediaryGroup(id);
    } catch (NoSuchTrustedIntermediaryGroupError e) {
      return notFound("no such group");
    }
    return redirect(routes.TrustedIntermediaryManagementController.index());
  }

  /** POST endpoint for adding an email to a trusted intermediary group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result addIntermediary(long id, Http.Request request) {
    Form<AddTrustedIntermediaryForm> form =
        formFactory.form(AddTrustedIntermediaryForm.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return flashAddTIFieldValuesWithError(form.errors().get(0).toString(), form, id);
    }
    try {
      userRepository.addTrustedIntermediaryToGroup(id, form.get().getEmailAddress());
    } catch (NoSuchTrustedIntermediaryGroupError e) {
      return flashAddTIFieldValuesWithError("No such TI group.", form, id);
    }

    return redirect(routes.TrustedIntermediaryManagementController.edit(id));
  }

  /** POST endpoint for removing an account from a trusted intermediary group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result removeIntermediary(long id, Http.Request request) {
    try {
      Form<RemoveTrustedIntermediaryForm> form =
          formFactory.form(RemoveTrustedIntermediaryForm.class).bindFromRequest(request);
      userRepository.removeTrustedIntermediaryFromGroup(id, form.get().getAccountId());
    } catch (NoSuchTrustedIntermediaryGroupError e) {
      return redirect(routes.TrustedIntermediaryManagementController.edit(id))
          .flashing("error", "No such TI group.");
    } catch (NoSuchTrustedIntermediaryError e) {
      return redirect(routes.TrustedIntermediaryManagementController.edit(id))
          .flashing("error", "No such TI.");
    }

    return redirect(routes.TrustedIntermediaryManagementController.edit(id));
  }
}
