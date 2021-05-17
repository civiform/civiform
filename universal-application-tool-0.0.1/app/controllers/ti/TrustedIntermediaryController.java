package controllers.ti;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;
import static play.mvc.Results.unauthorized;

import auth.Authorizers;
import auth.ProfileUtils;
import auth.UatProfile;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import java.util.Optional;
import javax.inject.Inject;
import models.TrustedIntermediaryGroup;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.UserRepository;
import services.ti.EmailAddressExistsException;
import views.applicant.TrustedIntermediaryDashboardView;

public class TrustedIntermediaryController {

  private final TrustedIntermediaryDashboardView tiDashboardView;
  private final ProfileUtils profileUtils;
  private final UserRepository userRepository;
  private final MessagesApi messagesApi;
  private final FormFactory formFactory;

  @Inject
  public TrustedIntermediaryController(
      ProfileUtils profileUtils,
      UserRepository userRepository,
      FormFactory formFactory,
      MessagesApi messagesApi,
      TrustedIntermediaryDashboardView trustedIntermediaryDashboardView) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.tiDashboardView = Preconditions.checkNotNull(trustedIntermediaryDashboardView);
    this.userRepository = Preconditions.checkNotNull(userRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.messagesApi = Preconditions.checkNotNull(messagesApi);
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result dashboard(Http.Request request) {
    Optional<UatProfile> uatProfile = profileUtils.currentUserProfile(request);
    if (uatProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
        userRepository.getTrustedIntermediaryGroup(uatProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    return ok(
        tiDashboardView.render(
            trustedIntermediaryGroup.get(),
            uatProfile.get().getApplicant().join().getApplicantData().getApplicantName(),
            request,
            messagesApi.preferred(request)));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result addApplicant(Long id, Http.Request request) {
    Optional<UatProfile> uatProfile = profileUtils.currentUserProfile(request);
    if (uatProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
        userRepository.getTrustedIntermediaryGroup(uatProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    if (!trustedIntermediaryGroup.get().id.equals(id)) {
      return unauthorized();
    }
    Form<AddApplicantToTrustedIntermediaryGroupForm> form =
        formFactory.form(AddApplicantToTrustedIntermediaryGroupForm.class).bindFromRequest(request);
    if (form.hasErrors()) {
      return redirectToDashboardWithError(form.errors().get(0).message(), form);
    }
    if (Strings.isNullOrEmpty(form.get().getFirstName())) {
      return redirectToDashboardWithError("First name required.", form);
    }
    if (Strings.isNullOrEmpty(form.get().getLastName())) {
      return redirectToDashboardWithError("Last name required.", form);
    }
    try {
      userRepository.createNewApplicantForTrustedIntermediaryGroup(
          form.get(), trustedIntermediaryGroup.get());
      return redirect(routes.TrustedIntermediaryController.dashboard());
    } catch (EmailAddressExistsException e) {
      return redirectToDashboardWithError(
          "Email address already in use.  Cannot create applicant if an account already exists. "
              + " Direct applicant to sign in and go to"
              + " https://civiform.seattle.gov/trustedIntermediaries.",
          form);
    }
  }

  private Result redirectToDashboardWithError(
      String errorMessage, Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    return redirect(routes.TrustedIntermediaryController.dashboard())
        .flashing("error", errorMessage)
        .flashing("providedFirstName", form.get().getFirstName())
        .flashing("providedMiddleName", form.get().getMiddleName())
        .flashing("providedLastName", form.get().getLastName())
        .flashing("providedEmail", form.get().getEmailAddress())
        .flashing("providedDob", form.get().getDob());
  }
}
