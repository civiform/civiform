package controllers.admin;

import auth.Authorizers;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import controllers.FlashKey;
import forms.AddTrustedIntermediaryForm;
import forms.CreateTrustedIntermediaryGroupForm;
import forms.RemoveTrustedIntermediaryForm;
import java.util.Optional;
import javax.inject.Inject;
import models.TrustedIntermediaryGroupModel;
import org.pac4j.play.java.Secure;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.AccountRepository;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.ti.NoSuchTrustedIntermediaryError;
import services.ti.NoSuchTrustedIntermediaryGroupError;
import services.ti.NotEligibleToBecomeTiError;
import views.admin.ti.EditTrustedIntermediaryGroupView;
import views.admin.ti.TrustedIntermediaryGroupListView;

/** Controller for admins to manage trusted intermediaries of programs. */
public class TrustedIntermediaryManagementController extends Controller {
  private final TrustedIntermediaryGroupListView listView;
  private final AccountRepository accountRepository;
  private final FormFactory formFactory;
  private final EditTrustedIntermediaryGroupView editView;
  private static final int PAGE_SIZE = 10;

  @Inject
  public TrustedIntermediaryManagementController(
      TrustedIntermediaryGroupListView listView,
      EditTrustedIntermediaryGroupView editView,
      AccountRepository accountRepository,
      FormFactory formFactory) {
    this.listView = Preconditions.checkNotNull(listView);
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.editView = Preconditions.checkNotNull(editView);
  }

  /** Return a HTML page displaying all trusted intermediary groups. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, Optional<Integer> page) {
    LoggerFactory.getLogger(TrustedIntermediaryManagementController.class)
        .info(request.flash().data().toString());
    var paginationSpec = new PageNumberBasedPaginationSpec(PAGE_SIZE, page.orElse(1));
    PaginationResult<TrustedIntermediaryGroupModel> tiGroups =
        accountRepository.getAllTiGroupsWithinPageSpec(F.Either.Right(paginationSpec));
    return ok(listView.render(tiGroups, paginationSpec, request));
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
    accountRepository.createNewTrustedIntermediaryGroup(
        form.get().getName(), form.get().getDescription());

    return redirect(routes.TrustedIntermediaryManagementController.index(Optional.of(1)));
  }

  private Result flashCreateTIFieldValuesWithError(
      String error, Form<CreateTrustedIntermediaryGroupForm> form) {
    Result result =
        redirect(routes.TrustedIntermediaryManagementController.index(Optional.of(1)))
            .flashing(FlashKey.ERROR, error);
    if (form.value().isPresent()) {
      result = result.flashing(FlashKey.PROVIDED_NAME, form.value().get().getName());
      result = result.flashing(FlashKey.PROVIDED_DESCRIPTION, form.value().get().getDescription());
    }
    return result;
  }

  private Result flashAddTIFieldValuesWithError(
      String error, Form<AddTrustedIntermediaryForm> form, long id) {
    Result result =
        redirect(routes.TrustedIntermediaryManagementController.edit(id))
            .flashing(FlashKey.ERROR, error);
    if (form.value().isPresent()) {
      result =
          result.flashing(FlashKey.PROVIDED_EMAIL_ADDRESS, form.value().get().getEmailAddress());
    }
    return result;
  }

  /** Return a HTML page displaying all trusted intermediaries in the specified group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(long id, Http.Request request) {
    Optional<TrustedIntermediaryGroupModel> tiGroup =
        accountRepository.getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      return notFound("no such group.");
    }
    return ok(editView.render(tiGroup.get(), request));
  }

  /** POST endpoint for deleting a trusted intermediary group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result delete(long id, Http.Request request) {
    try {
      accountRepository.deleteTrustedIntermediaryGroup(id);
    } catch (NoSuchTrustedIntermediaryGroupError e) {
      return notFound("no such group");
    }
    return redirect(routes.TrustedIntermediaryManagementController.index(Optional.of(1)));
  }

  /** POST endpoint for adding an email to a trusted intermediary group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result addIntermediary(long id, Http.Request request) {
    Form<AddTrustedIntermediaryForm> form =
        formFactory.form(AddTrustedIntermediaryForm.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return flashAddTIFieldValuesWithError(form.errors().get(0).toString(), form, id);
    }
    if (Strings.isNullOrEmpty(form.get().getEmailAddress())) {
      return flashAddTIFieldValuesWithError("Must provide email address.", form, id);
    }
    try {
      accountRepository.addTrustedIntermediaryToGroup(id, form.get().getEmailAddress());
    } catch (NoSuchTrustedIntermediaryGroupError e) {
      return flashAddTIFieldValuesWithError("No such TI group.", form, id);
    } catch (NotEligibleToBecomeTiError e) {
      return flashAddTIFieldValuesWithError(
          "Users that are CiviForm Admins or Program Admins may not become a TI.", form, id);
    }

    return redirect(routes.TrustedIntermediaryManagementController.edit(id));
  }

  /** POST endpoint for removing an account from a trusted intermediary group. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result removeIntermediary(long id, Http.Request request) {
    try {
      Form<RemoveTrustedIntermediaryForm> form =
          formFactory.form(RemoveTrustedIntermediaryForm.class).bindFromRequest(request);
      accountRepository.removeTrustedIntermediaryFromGroup(id, form.get().getAccountId());
    } catch (NoSuchTrustedIntermediaryGroupError e) {
      return redirect(routes.TrustedIntermediaryManagementController.edit(id))
          .flashing(FlashKey.ERROR, "No such TI group.");
    } catch (NoSuchTrustedIntermediaryError e) {
      return redirect(routes.TrustedIntermediaryManagementController.edit(id))
          .flashing(FlashKey.ERROR, "No such TI.");
    }

    return redirect(routes.TrustedIntermediaryManagementController.edit(id));
  }
}
