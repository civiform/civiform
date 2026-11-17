package controllers.ti;

import static play.mvc.Results.forbidden;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;
import static play.mvc.Results.unauthorized;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import controllers.BadRequestException;
import controllers.FlashKey;
import forms.TiClientInfoForm;
import java.util.Optional;
import javax.inject.Inject;
import mapping.trustedintermediary.TrustedIntermediaryAccountSettingsPageMapper;
import mapping.trustedintermediary.TrustedIntermediaryAddClientPageMapper;
import mapping.trustedintermediary.TrustedIntermediaryClientListPageMapper;
import mapping.trustedintermediary.TrustedIntermediaryEditClientPageMapper;
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
import services.DateConverter;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.applicant.exception.ApplicantNotFoundException;
import services.pagination.PaginationInfo;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import services.ti.AddNewApplicantReturnObject;
import services.ti.TrustedIntermediarySearchResult;
import services.ti.TrustedIntermediaryService;
import views.trustedintermediary.EditTiClientView;
import views.trustedintermediary.TrustedIntermediaryAccountSettingsPageView;
import views.trustedintermediary.TrustedIntermediaryAccountSettingsPageViewModel;
import views.trustedintermediary.TrustedIntermediaryAccountSettingsView;
import views.trustedintermediary.TrustedIntermediaryAddClientPageView;
import views.trustedintermediary.TrustedIntermediaryAddClientPageViewModel;
import views.trustedintermediary.TrustedIntermediaryClientListPageView;
import views.trustedintermediary.TrustedIntermediaryClientListPageViewModel;
import views.trustedintermediary.TrustedIntermediaryClientListView;
import views.trustedintermediary.TrustedIntermediaryEditClientPageView;
import views.trustedintermediary.TrustedIntermediaryEditClientPageViewModel;

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
  private final SettingsManifest settingsManifest;
  private final TrustedIntermediaryClientListPageView tiClientListPageView;
  private final TrustedIntermediaryAccountSettingsPageView tiAccountSettingsPageView;
  private final TrustedIntermediaryAddClientPageView tiAddClientPageView;
  private final TrustedIntermediaryEditClientPageView tiEditClientPageView;
  private final ProgramService programService;
  private final DateConverter dateConverter;

  @Inject
  public TrustedIntermediaryController(
      ProfileUtils profileUtils,
      AccountRepository accountRepository,
      FormFactory formFactory,
      MessagesApi messagesApi,
      TrustedIntermediaryClientListView trustedIntermediaryClientListView,
      TrustedIntermediaryService tiService,
      EditTiClientView editTiClientView,
      TrustedIntermediaryAccountSettingsView tiAccountSettingsView,
      SettingsManifest settingsManifest,
      TrustedIntermediaryClientListPageView tiClientListPageView,
      TrustedIntermediaryAccountSettingsPageView tiAccountSettingsPageView,
      TrustedIntermediaryAddClientPageView tiAddClientPageView,
      TrustedIntermediaryEditClientPageView tiEditClientPageView,
      ProgramService programService,
      DateConverter dateConverter) {
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
    this.tiClientListView = Preconditions.checkNotNull(trustedIntermediaryClientListView);
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.formFactory = Preconditions.checkNotNull(formFactory);
    this.messagesApi = Preconditions.checkNotNull(messagesApi);
    this.tiService = Preconditions.checkNotNull(tiService);
    this.editTiClientView = Preconditions.checkNotNull(editTiClientView);
    this.tiAccountSettingsView = Preconditions.checkNotNull(tiAccountSettingsView);
    this.settingsManifest = Preconditions.checkNotNull(settingsManifest);
    this.tiClientListPageView = Preconditions.checkNotNull(tiClientListPageView);
    this.tiAccountSettingsPageView = Preconditions.checkNotNull(tiAccountSettingsPageView);
    this.tiAddClientPageView = Preconditions.checkNotNull(tiAddClientPageView);
    this.tiEditClientPageView = Preconditions.checkNotNull(tiEditClientPageView);
    this.programService = Preconditions.checkNotNull(programService);
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
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
    CiviFormProfile civiformProfile = profileUtils.currentUserProfile(request);
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile);
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

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      TrustedIntermediaryClientListPageViewModel model =
          new TrustedIntermediaryClientListPageMapper()
              .map(
                  trustedIntermediaryGroup.get(),
                  pageInfo.getPageItems(),
                  pageInfo.getPageCount(),
                  pageInfo.getPage(),
                  searchParameters,
                  settingsManifest.getBaseUrl().orElse(""),
                  programService,
                  dateConverter,
                  request.flash().get(FlashKey.ERROR));
      return ok(tiClientListPageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    Optional<String> applicantName =
        civiformProfile.getApplicant().join().getApplicantDisplayName();

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
    CiviFormProfile civiformProfile = profileUtils.currentUserProfile(request);
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile);

    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      TrustedIntermediaryAccountSettingsPageViewModel model =
          new TrustedIntermediaryAccountSettingsPageMapper()
              .map(
                  trustedIntermediaryGroup.get(),
                  settingsManifest.getBaseUrl().orElse(""),
                  request.flash().get(FlashKey.ERROR));
      return ok(tiAccountSettingsPageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    Optional<String> applicantName =
        civiformProfile.getApplicant().join().getApplicantDisplayName();

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
    CiviFormProfile civiformProfile = profileUtils.currentUserProfile(request);
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile);
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    if (!trustedIntermediaryGroup.get().id.equals(id)) {
      return unauthorized();
    }

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      return renderAddClientPage(
          request,
          trustedIntermediaryGroup.get(),
          /* submittedForm= */ Optional.empty(),
          /* newClientApplicantId= */ Optional.empty());
    }

    Optional<String> applicantName = civiformProfile.getApplicant().join().getApplicantName();

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
    CiviFormProfile civiformProfile = profileUtils.currentUserProfile(request);
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile);
    if (trustedIntermediaryGroup.isEmpty()) {
      return notFound();
    }
    boolean isManagedAccount =
        trustedIntermediaryGroup.get().getManagedAccounts().stream()
            .anyMatch(account -> account.id.equals(accountId));
    if (!isManagedAccount) {
      return forbidden();
    }

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      return renderEditClientPage(
          request,
          trustedIntermediaryGroup.get(),
          accountId,
          /* submittedForm= */ Optional.empty());
    }

    String applicantName =
        accountRepository.lookupAccount(accountId).get().getApplicantDisplayName();
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
    CiviFormProfile civiformProfile = profileUtils.currentUserProfile(request);
    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile);
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

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      return renderAddClientPage(
          request,
          trustedIntermediaryGroup.get(),
          Optional.of(form),
          Optional.ofNullable(addNewApplicantReturnObject.getApplicantId()));
    }

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
    CiviFormProfile civiformProfile = profileUtils.currentUserProfile(request);

    Optional<TrustedIntermediaryGroupModel> trustedIntermediaryGroup =
        accountRepository.getTrustedIntermediaryGroup(civiformProfile);
    if (trustedIntermediaryGroup.isEmpty()) {
      return unauthorized();
    }
    Form<TiClientInfoForm> form = formFactory.form(TiClientInfoForm.class).bindFromRequest(request);
    form =
        tiService.updateClientInfo(
            form, trustedIntermediaryGroup.get(), id, messagesApi.preferred(request));

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      return renderEditClientPage(request, trustedIntermediaryGroup.get(), id, Optional.of(form));
    }

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

  /** Renders the Thymeleaf add client page, on first show and after a submission. */
  private Result renderAddClientPage(
      Http.Request request,
      TrustedIntermediaryGroupModel tiGroup,
      Optional<Form<TiClientInfoForm>> submittedForm,
      Optional<Long> newClientApplicantId) {
    TrustedIntermediaryAddClientPageViewModel model =
        new TrustedIntermediaryAddClientPageMapper()
            .map(
                tiGroup,
                submittedForm,
                newClientApplicantId,
                settingsManifest.getNameSuffixDropdownEnabled(request),
                settingsManifest.getBaseUrl().orElse(""),
                dateConverter,
                messagesApi.preferred(request));

    return ok(tiAddClientPageView.render(request, model)).as(Http.MimeTypes.HTML);
  }

  /** Renders the Thymeleaf edit client page, on first show and after a submission. */
  private Result renderEditClientPage(
      Http.Request request,
      TrustedIntermediaryGroupModel tiGroup,
      Long accountId,
      Optional<Form<TiClientInfoForm>> submittedForm) {
    // The legacy view looked the account up the same way.
    AccountModel account = accountRepository.lookupAccount(accountId).get();

    TrustedIntermediaryEditClientPageViewModel model =
        new TrustedIntermediaryEditClientPageMapper()
            .map(
                tiGroup,
                account,
                submittedForm,
                settingsManifest.getNameSuffixDropdownEnabled(request),
                settingsManifest.getBaseUrl().orElse(""),
                dateConverter,
                messagesApi.preferred(request));

    return ok(tiEditClientPageView.render(request, model)).as(Http.MimeTypes.HTML);
  }

  private Long getTiApplicantIdFromCiviformProfile(CiviFormProfile civiformProfile) {
    return civiformProfile.getApplicant().toCompletableFuture().join().id;
  }
}
