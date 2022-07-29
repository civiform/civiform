package controllers.ti;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;
import static play.mvc.Results.unauthorized;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.UpdateApplicantDob;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import models.TrustedIntermediaryGroup;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.SearchParameters;
import repository.UserRepository;
import services.PaginationInfo;
import services.applicant.exception.ApplicantNotFoundException;
import services.ti.*;
import views.applicant.TrustedIntermediaryDashboardView;

/**
 * Controller for handling methods for an trusted intermediary managing their clients and applying
 * to programs on behalf of them.
 */
public class TrustedIntermediaryController {

  private static final int PAGE_SIZE = 10;
  private final TrustedIntermediaryDashboardView tiDashboardView;
  private final ProfileUtils profileUtils;
  private final UserRepository userRepository;
  private final MessagesApi messagesApi;
  private final FormFactory formFactory;
  private final String baseUrl;
  private final TrustedIntermediaryService tiService;

  @Inject
  public TrustedIntermediaryController(
      ProfileUtils profileUtils,
      UserRepository userRepository,
      FormFactory formFactory,
      MessagesApi messagesApi,
      TrustedIntermediaryDashboardView trustedIntermediaryDashboardView,
      Config config,
      TrustedIntermediaryService tiService) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.tiDashboardView = Preconditions.checkNotNull(trustedIntermediaryDashboardView);
    this.userRepository = Preconditions.checkNotNull(userRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.messagesApi = Preconditions.checkNotNull(messagesApi);
    this.baseUrl = Preconditions.checkNotNull(config).getString("base_url");
    this.tiService = Preconditions.checkNotNull(tiService);
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result dashboard(
      Http.Request request,
      Optional<String> nameQuery,
      Optional<String> searchDate,
      Optional<Integer> page) {
    if (page.isEmpty()) {
      return redirect(
          routes.TrustedIntermediaryController.dashboard(nameQuery, searchDate, Optional.of(1)));
    }
    Optional<CiviFormProfile> civiformProfile = profileUtils.currentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
        userRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    SearchParameters searchParameters =
        SearchParameters.builder().setNameQuery(nameQuery).setSearchDate(searchDate).build();
    ImmutableList<Account> managedAccounts =
        tiService.getManagedAccounts(searchParameters, trustedIntermediaryGroup.get());
    PaginationInfo<Account> pageInfo =
        PaginationInfo.paginate(managedAccounts, PAGE_SIZE, page.get());
    return ok(
        tiDashboardView.render(
            trustedIntermediaryGroup.get(),
            civiformProfile.get().getApplicant().join().getApplicantData().getApplicantName(),
            pageInfo.getPageItems(),
            pageInfo.getPageCount(),
            pageInfo.getPage(),
            searchParameters,
            request,
            messagesApi.preferred(request)));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result updateDateOfBirth(Long accountId, Http.Request request) {
    Optional<CiviFormProfile> civiformProfile = profileUtils.currentUserProfile(request);

    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }

    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
        userRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }

    Form<UpdateApplicantDob> form =
        formFactory.form(UpdateApplicantDob.class).bindFromRequest(request);
    try {
      tiService.updateApplicantDateOfBirth(accountId, form);

    } catch (IncorrectDateFormatException
        | MissingDateOfBirthException
        | ApplicantNotFoundException
        | FormHasErrorException
        | DateOfBirthNotInPastException e) {
      redirectToDashboardWithUpdateDateOfBirthError(e.getLocalizedMessage(), form);
    }

    return redirect(
        routes.TrustedIntermediaryController.dashboard(
            /* paramName=  nameQuery */
            Optional.empty(),
            /* paramName=  searchDate */
            Optional.empty(),
            /* paramName=  page */
            Optional.empty()));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result addApplicant(Long id, Http.Request request) {
    Optional<CiviFormProfile> civiformProfile = profileUtils.currentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroup> trustedIntermediaryGroup =
        userRepository.getTrustedIntermediaryGroup(civiformProfile.get());
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
    if (Strings.isNullOrEmpty(form.get().getDob())) {
      return redirectToDashboardWithError("Date Of Birth required.", form);
    }
    try {
      userRepository.createNewApplicantForTrustedIntermediaryGroup(
          form.get(), trustedIntermediaryGroup.get());
      return redirect(
          routes.TrustedIntermediaryController.dashboard(
              /* paramName=  nameQuery */
              Optional.empty(),
              /* paramName=  searchDate */
              Optional.empty(),
              /* paramName=  page */
              Optional.empty()));
    } catch (EmailAddressExistsException e) {
      String trustedIntermediaryUrl = baseUrl + "/trustedIntermediaries";

      return redirectToDashboardWithError(
          "Email address already in use.  Cannot create applicant if an account already exists. "
              + " Direct applicant to sign in and go to"
              + " "
              + trustedIntermediaryUrl,
          form);
    }
  }

  private Result redirectToDashboardWithError(
      String errorMessage, Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    return redirect(
            routes.TrustedIntermediaryController.dashboard(
                /* paramName=  nameQuery */
                Optional.empty(),
                /* paramName=  searchDate */
                Optional.empty(),
                /* paramName=  page */
                Optional.empty()))
        .flashing("error", errorMessage)
        .flashing("providedFirstName", form.get().getFirstName())
        .flashing("providedMiddleName", form.get().getMiddleName())
        .flashing("providedLastName", form.get().getLastName())
        .flashing("providedEmail", form.get().getEmailAddress())
        .flashing("providedDateOfBirth", form.get().getDob());
  }

  private Result redirectToDashboardWithUpdateDateOfBirthError(
      String errorMessage, Form<UpdateApplicantDob> form) {
    return redirect(
            routes.TrustedIntermediaryController.dashboard(
                /* paramName=  nameQuery */
                Optional.empty(),
                /* paramName=  searchDate */
                Optional.empty(),
                /* paramName=  page */
                Optional.empty()))
        .flashing("error", errorMessage)
        .flashing("providedDateOfBirth", form.get().getDob());
  }
}
