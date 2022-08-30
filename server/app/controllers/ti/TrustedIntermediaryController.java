package controllers.ti;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;
import static play.mvc.Results.unauthorized;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import controllers.BadRequestException;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.UpdateApplicantDobForm;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import models.TrustedIntermediaryGroup;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.SearchParameters;
import repository.UserRepository;
import services.PaginationInfo;
import services.ti.TrustedIntermediarySearchResult;
import services.ti.TrustedIntermediaryService;
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
  private final TrustedIntermediaryService tiService;

  @Inject
  public TrustedIntermediaryController(
      ProfileUtils profileUtils,
      UserRepository userRepository,
      FormFactory formFactory,
      MessagesApi messagesApi,
      TrustedIntermediaryDashboardView trustedIntermediaryDashboardView,
      TrustedIntermediaryService tiService) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.tiDashboardView = Preconditions.checkNotNull(trustedIntermediaryDashboardView);
    this.userRepository = Preconditions.checkNotNull(userRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.messagesApi = Preconditions.checkNotNull(messagesApi);
    this.tiService = Preconditions.checkNotNull(tiService);
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result dashboard(
      Http.Request request,
      Optional<String> nameQuery,
      Optional<String> dateQuery,
      Optional<Integer> page) {
    if (page.isEmpty()) {
      return redirect(
          routes.TrustedIntermediaryController.dashboard(nameQuery, dateQuery, Optional.of(1)));
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
        SearchParameters.builder().setNameQuery(nameQuery).setDateQuery(dateQuery).build();
    TrustedIntermediarySearchResult trustedIntermediarySearchResult =
        tiService.getManagedAccounts(searchParameters, trustedIntermediaryGroup.get());
    if (!trustedIntermediarySearchResult.isSuccessful()) {
      throw new BadRequestException(trustedIntermediarySearchResult.getErrorMessage().get());
    }
    PaginationInfo<Account> pageInfo =
        PaginationInfo.paginate(
            trustedIntermediarySearchResult.getAccounts().get(), PAGE_SIZE, page.get());

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
    final Form<UpdateApplicantDobForm> form;
    form =
        tiService.updateApplicantDateOfBirth(
            trustedIntermediaryGroup.get(),
            accountId,
            formFactory.form(UpdateApplicantDobForm.class).bindFromRequest(request));

    if (!form.hasErrors()) {
      return redirect(
              routes.TrustedIntermediaryController.dashboard(
                      /* nameQuery= */ Optional.empty(),
                      /* dateQuery= */ Optional.empty(),
                      /* page= */ Optional.empty())
                  .url())
          .flashing("success", "Date of Birth is updated");
    }

    return redirectToDashboardWithUpdateDateOfBirthError(getValidationErros(form.errors()));
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
        tiService.addNewClient(
            formFactory
                .form(AddApplicantToTrustedIntermediaryGroupForm.class)
                .bindFromRequest(request),
            trustedIntermediaryGroup.get());
    if (!form.hasErrors()) {
      return redirect(
          routes.TrustedIntermediaryController.dashboard(
              /* nameQuery= */ Optional.empty(),
              /* dateQuery= */ Optional.empty(),
              /* page= */ Optional.empty()));
    }
    return redirectToDashboardWithError(getValidationErros(form.errors()), form);
  }

  private String getValidationErros(List<ValidationError> errors) {
    StringBuilder errorMessage = new StringBuilder();
    errors.stream()
        .forEach(validationError -> errorMessage.append(validationError.message() + "\n"));
    return errorMessage.toString();
  }

  private Result redirectToDashboardWithError(
      String errorMessage, Form<AddApplicantToTrustedIntermediaryGroupForm> form) {
    return redirect(
            routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dateQuery= */ Optional.empty(),
                    /* page= */ Optional.of(1))
                .url())
        .flashing("error", errorMessage)
        .flashing("providedFirstName", form.value().get().getFirstName())
        .flashing("providedMiddleName", form.value().get().getMiddleName())
        .flashing("providedLastName", form.value().get().getLastName())
        .flashing("providedEmail", form.value().get().getEmailAddress())
        .flashing("providedDateOfBirth", form.value().get().getDob());
  }

  private Result redirectToDashboardWithUpdateDateOfBirthError(String errorMessage) {
    return redirect(
            routes.TrustedIntermediaryController.dashboard(
                    /* paramName=  nameQuery */
                    Optional.empty(),
                    /* paramName=  searchDate */
                    Optional.empty(),
                    /* paramName=  page */
                    Optional.of(1))
                .url())
        .flashing("error", errorMessage);
  }
}
