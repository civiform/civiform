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
import forms.TiClientInfoForm;
import java.util.Optional;
import javax.inject.Inject;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.AccountRepository;
import repository.SearchParameters;
import services.PaginationInfo;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.applicant.exception.ApplicantNotFoundException;
import services.ti.AddNewApplicantReturnObject;
import services.ti.TrustedIntermediarySearchResult;
import services.ti.TrustedIntermediaryService;
import views.applicant.EditTiClientView;
import views.applicant.TrustedIntermediaryAccountSettingsView;
import views.applicant.TrustedIntermediaryClientListView;

/**
 * Controller for handling methods for an trusted intermediary managing their clients and applying
 * to programs on behalf of them.
 */
public final class TrustedIntermediaryController {

  private static final int PAGE_SIZE = 10;
  private final TrustedIntermediaryClientListView tiClientListView;
  private final ProfileUtils profileUtils;
  private final AccountRepository accountRepository;
  private final MessagesApi messagesApi;
  private final FormFactory formFactory;
  private final TrustedIntermediaryService tiService;
  private final EditTiClientView editTiClientView;
  private final TrustedIntermediaryAccountSettingsView tiAccountSettingsView;

  @Inject
  public TrustedIntermediaryController(
      ProfileUtils profileUtils,
      AccountRepository accountRepository,
      FormFactory formFactory,
      MessagesApi messagesApi,
      TrustedIntermediaryClientListView trustedIntermediaryClientListView,
      TrustedIntermediaryService tiService,
      EditTiClientView editTiClientView,
      TrustedIntermediaryAccountSettingsView tiAccountSettingsView) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.tiClientListView = Preconditions.checkNotNull(trustedIntermediaryClientListView);
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.messagesApi = Preconditions.checkNotNull(messagesApi);
    this.tiService = Preconditions.checkNotNull(tiService);
    this.editTiClientView = Preconditions.checkNotNull(editTiClientView);
    this.tiAccountSettingsView = Preconditions.checkNotNull(tiAccountSettingsView);
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result dashboard(
      Http.Request request,
      Optional<String> nameQuery,
      Optional<String> dayQuery,
      Optional<String> monthQuery,
      Optional<String> yearQuery,
      Optional<Integer> page) {
    if (page.isEmpty()) {
      return redirect(
          routes.TrustedIntermediaryController.dashboard(
              nameQuery, dayQuery, monthQuery, yearQuery, Optional.of(1)));
    }
    Optional<CiviFormProfile> civiformProfile = profileUtils.optionalCurrentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    SearchParameters searchParameters =
        SearchParameters.builder()
            .setNameQuery(nameQuery)
            .setDayQuery(dayQuery)
            .setMonthQuery(monthQuery)
            .setYearQuery(yearQuery)
            .build();
    TrustedIntermediarySearchResult trustedIntermediarySearchResult =
        tiService.getManagedAccounts(searchParameters, trustedIntermediaryGroup.get());
    if (!trustedIntermediarySearchResult.isSuccessful()) {
      throw new BadRequestException(trustedIntermediarySearchResult.errorMessage().get());
    }
    PaginationInfo<AccountModel> pageInfo =
        PaginationInfo.paginate(trustedIntermediarySearchResult.accounts(), PAGE_SIZE, page.get());

    Optional<String> applicantName =
        civiformProfile.get().getApplicant().join().getApplicantData().getApplicantName();

    return ok(
        tiClientListView.render(
            /* tiGroup= */ trustedIntermediaryGroup.get(),
            /* personalInfo= */ ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder().setName(applicantName).build()),
            /* managedAccounts= */ pageInfo.getPageItems(),
            /* totalPageCount= */ pageInfo.getPageCount(),
            /* page= */ pageInfo.getPage(),
            /* searchParameters= */ searchParameters,
            /* request= */ request,
            /* messages= */ messagesApi.preferred(request),
            /* currentTisApplicantId= */ getTiApplicantIdFromCiviformProfile(civiformProfile)));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result accountSettings(Http.Request request) {

    Optional<CiviFormProfile> civiformProfile = profileUtils.optionalCurrentUserProfile(request);

    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }

    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile.get());

    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }

    Optional<String> applicantName =
        civiformProfile.get().getApplicant().join().getApplicantData().getApplicantName();

    return ok(
        tiAccountSettingsView.render(
            /* tiGroup= */ trustedIntermediaryGroup.get(),
            /* personalInfo= */ ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder().setName(applicantName).build()),
            /* request= */ request,
            /* messages= */ messagesApi.preferred(request),
            /* currentTisApplicantId= */ getTiApplicantIdFromCiviformProfile(civiformProfile)));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result showAddClientForm(Long id, Http.Request request) {
    Optional<CiviFormProfile> civiformProfile = profileUtils.optionalCurrentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    if (!trustedIntermediaryGroup.get().id.equals(id)) {
      return unauthorized();
    }
    Optional<String> applicantName =
        civiformProfile.get().getApplicant().join().getApplicantData().getApplicantName();

    return ok(
        editTiClientView.render(
            /* tiGroup= */ trustedIntermediaryGroup.get(),
            /* personalInfo= */ ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder().setName(applicantName).build()),
            /* request= */ request,
            /* messages= */ messagesApi.preferred(request),
            /* accountIdToEdit= */ Optional.empty(),
            /* applicantIdOfTi= */ getTiApplicantIdFromCiviformProfile(civiformProfile),
            /* tiClientInfoForm= */ Optional.empty(),
            /* applicantIdOfNewlyAddedClient= */ null));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result showEditClientForm(Long accountId, Http.Request request) {
    Optional<CiviFormProfile> civiformProfile = profileUtils.optionalCurrentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    String applicantName = accountRepository.lookupAccount(accountId).get().getApplicantName();
    return ok(
        editTiClientView.render(
            /* tiGroup= */ trustedIntermediaryGroup.get(),
            /* personalInfo= */ ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder().setName(applicantName).build()),
            /* request= */ request,
            /* messages= */ messagesApi.preferred(request),
            /* accountIdToEdit= */ Optional.of(accountId),
            /* applicantIdOfTi= */ getTiApplicantIdFromCiviformProfile(civiformProfile),
            /* tiClientInfoForm= */ Optional.empty(),
            /* applicantIdOfNewlyAddedClient= */ null));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result addClient(Long id, Http.Request request) {
    Optional<CiviFormProfile> civiformProfile = profileUtils.optionalCurrentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    if (!trustedIntermediaryGroup.get().id.equals(id)) {
      return unauthorized();
    }
    AddNewApplicantReturnObject addNewApplicantReturnObject =
        tiService.addNewClient(
            formFactory.form(TiClientInfoForm.class).bindFromRequest(request),
            trustedIntermediaryGroup.get(),
            messagesApi.preferred(request));
    Form<TiClientInfoForm> form = addNewApplicantReturnObject.getForm();
    return ok(
        editTiClientView.render(
            /* tiGroup= */ trustedIntermediaryGroup.get(),
            /* personalInfo= */ ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder()
                    .setName(
                        form.value().get().getFirstName() + " " + form.value().get().getLastName())
                    .build()),
            /* request= */ request,
            /* messages= */ messagesApi.preferred(request),
            /* accountIdToEdit= */ Optional.empty(),
            /* applicantIdOfTi= */ getTiApplicantIdFromCiviformProfile(civiformProfile),
            /* tiClientInfoForm= */ Optional.of(form),
            /* applicantIdOfNewlyAddedClient= */ addNewApplicantReturnObject.getApplicantId()));
  }

  @Secure(authorizers = Authorizers.Labels.TI)
  public Result editClient(Long id, Http.Request request) throws ApplicantNotFoundException {
    Optional<CiviFormProfile> civiformProfile = profileUtils.optionalCurrentUserProfile(request);
    if (civiformProfile.isEmpty()) {
      return unauthorized();
    }

    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile.get());
    if (trustedIntermediaryGroup.isEmpty()) {
      return unauthorized();
    }
    Form<TiClientInfoForm> form = formFactory.form(TiClientInfoForm.class).bindFromRequest(request);
    form =
        tiService.updateClientInfo(
            form, trustedIntermediaryGroup.get(), id, messagesApi.preferred(request));
    return ok(
        editTiClientView.render(
            /* tiGroup= */ trustedIntermediaryGroup.get(),
            /* personalInfo= */ ApplicantPersonalInfo.ofLoggedInUser(
                Representation.builder()
                    .setName(
                        form.value().get().getFirstName() + " " + form.value().get().getLastName())
                    .build()),
            /* request= */ request,
            /* messages= */ messagesApi.preferred(request),
            /* accountIdToEdit= */ Optional.of(id),
            /* applicantIdOfTi= */ getTiApplicantIdFromCiviformProfile(civiformProfile),
            /* tiClientInfoForm= */ Optional.of(form),
            /* applicantIdOfNewlyAddedClient= */ null));
  }

  private Long getTiApplicantIdFromCiviformProfile(Optional<CiviFormProfile> civiformProfile) {
    return civiformProfile.get().getApplicant().toCompletableFuture().join().id;
  }
}
