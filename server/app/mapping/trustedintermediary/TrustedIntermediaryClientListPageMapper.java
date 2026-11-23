package mapping.trustedintermediary;

import com.google.common.collect.ImmutableList;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import controllers.ti.routes;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import mapping.admin.shared.LegacyPaginationMapper;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.TrustedIntermediaryGroupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.SearchParameters;
import services.DateConverter;
import services.PhoneValidationResult;
import services.PhoneValidationUtils;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.ti.TrustedIntermediaryService;
import views.admin.shared.LegacyPagination;
import views.trustedintermediary.TrustedIntermediaryClientListPageViewModel;
import views.trustedintermediary.TrustedIntermediaryClientListPageViewModel.Client;
import views.trustedintermediary.TrustedIntermediaryClientListPageViewModel.ClientCountHeading;

/** Maps a trusted intermediary's clients to the TrustedIntermediaryClientListPageViewModel. */
public final class TrustedIntermediaryClientListPageMapper {
  private static final Logger logger =
      LoggerFactory.getLogger(TrustedIntermediaryClientListPageMapper.class);
  private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

  /**
   * Maps one page of the trusted intermediary group's clients to the view model.
   *
   * @param tiGroup the group the trusted intermediary belongs to
   * @param managedAccounts the clients on the current page
   * @param totalPageCount the total number of pages
   * @param page the current (1-based) page number
   * @param searchParameters the search the clients were filtered by, kept sticky in the form
   * @param baseUrl the URL of the CiviForm deployment, prefixed to the tab links
   * @param programService resolves the names of the programs the clients applied to
   * @param dateConverter formats the clients' dates of birth
   * @param errorMessage the flash error, shown as an error toast
   */
  public TrustedIntermediaryClientListPageViewModel map(
      TrustedIntermediaryGroupModel tiGroup,
      ImmutableList<AccountModel> managedAccounts,
      int totalPageCount,
      int page,
      SearchParameters searchParameters,
      String baseUrl,
      ProgramService programService,
      DateConverter dateConverter,
      Optional<String> errorMessage) {
    ImmutableList<Client> clients =
        managedAccounts.stream()
            .sorted(Comparator.comparing(AccountModel::getApplicantDisplayName))
            .map(account -> buildClient(account, programService, dateConverter))
            .collect(ImmutableList.toImmutableList());

    return TrustedIntermediaryClientListPageViewModel.builder()
        .tiGroupName(tiGroup.getName())
        .clientListUrl(
            baseUrl
                + dashboardUrl(
                    /* searchParameters= */ Optional.empty(), /* page= */ Optional.of(1)))
        .accountSettingsUrl(baseUrl + routes.TrustedIntermediaryController.accountSettings().url())
        .addClientUrl(routes.TrustedIntermediaryController.showAddClientForm(tiGroup.id).url())
        .searchUrl(
            dashboardUrl(/* searchParameters= */ Optional.empty(), /* page= */ Optional.empty()))
        .nameQuery(searchParameters.nameQuery().orElse(""))
        .dayQuery(searchParameters.dayQuery().orElse(""))
        .monthQuery(searchParameters.monthQuery().orElse(""))
        .yearQuery(searchParameters.yearQuery().orElse(""))
        .validSearch(TrustedIntermediaryService.validateSearch(searchParameters))
        .clientCountHeading(buildClientCountHeading(searchParameters, managedAccounts.size()))
        .clientCount(managedAccounts.size())
        .clients(clients)
        .pagination(buildPagination(managedAccounts, searchParameters, page, totalPageCount))
        // The legacy ToastMessage.errorNonLocalized prefixed the flash text and gave each toast a
        // random id.
        .errorToastId(errorMessage.map(error -> UUID.randomUUID().toString()).orElse(null))
        .errorMessage(errorMessage.map(error -> "Error: " + error).orElse(null))
        .build();
  }

  private static String dashboardUrl(
      Optional<SearchParameters> searchParameters, Optional<Integer> page) {
    return routes.TrustedIntermediaryController.dashboard(
            searchParameters.flatMap(SearchParameters::nameQuery),
            searchParameters.flatMap(SearchParameters::dayQuery),
            searchParameters.flatMap(SearchParameters::monthQuery),
            searchParameters.flatMap(SearchParameters::yearQuery),
            page)
        .url();
  }

  /** The count is of the clients on the current page, as it was in the legacy view. */
  private static ClientCountHeading buildClientCountHeading(
      SearchParameters searchParameters, int clientCount) {
    boolean noSearchTerms =
        TrustedIntermediaryService.findMissingSearchParams(searchParameters).size() == 4;

    if (noSearchTerms) {
      return ClientCountHeading.ALL_CLIENTS;
    }

    if (clientCount == 1) {
      return ClientCountHeading.ONE_CLIENT;
    }

    return ClientCountHeading.MULTIPLE_CLIENTS;
  }

  private static LegacyPagination buildPagination(
      ImmutableList<AccountModel> managedAccounts,
      SearchParameters searchParameters,
      int page,
      int totalPageCount) {
    if (managedAccounts.isEmpty()) {
      return null;
    }

    return new LegacyPaginationMapper()
        .map(
            page,
            totalPageCount,
            pageNumber -> dashboardUrl(Optional.of(searchParameters), Optional.of(pageNumber)));
  }

  private static Client buildClient(
      AccountModel account, ProgramService programService, DateConverter dateConverter) {
    Client.ClientBuilder client =
        Client.builder()
            .displayName(account.getApplicantDisplayName())
            .editUrl(routes.TrustedIntermediaryController.showEditClientForm(account.id).url())
            .notes(account.getTiNote());
    Optional<ApplicantModel> newestApplicant = account.representativeApplicant();

    if (newestApplicant.isEmpty()) {
      return client.hasApplicant(false).build();
    }

    ApplicantModel applicant = newestApplicant.get();
    ImmutableList<ApplicationModel> submittedApplications =
        applicant.getApplications().stream()
            .filter(application -> application.getLifecycleStage() == LifecycleStage.ACTIVE)
            .collect(ImmutableList.toImmutableList());

    return client
        .hasApplicant(true)
        .applicantId(applicant.id)
        .applicationsUrl(
            controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
                    applicant.id, /* categories= */ ImmutableList.of())
                .url())
        .dateOfBirth(applicant.getDateOfBirth().map(dateConverter::formatIso8601Date).orElse(""))
        .phoneNumber(
            applicant
                .getPhoneNumber()
                .map(TrustedIntermediaryClientListPageMapper::formatPhone)
                .orElse(null))
        .emailAddress(applicant.getEmailAddress().orElse(null))
        .applicationCount(submittedApplications.size())
        .programNames(buildProgramNames(submittedApplications, programService))
        .build();
  }

  private static String formatPhone(String phone) {
    try {
      PhoneValidationResult phoneValidationResults =
          PhoneValidationUtils.determineCountryCode(Optional.ofNullable(phone));
      Phonenumber.PhoneNumber phoneNumber =
          PHONE_NUMBER_UTIL.parse(phone, phoneValidationResults.getCountryCode().orElse(""));

      return PHONE_NUMBER_UTIL.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    } catch (NumberParseException e) {
      return "-";
    }
  }

  private static String buildProgramNames(
      ImmutableList<ApplicationModel> submittedApplications, ProgramService programService) {
    return submittedApplications.stream()
        .map(
            application -> {
              try {
                return programService
                    .getFullProgramDefinition(application.getProgram().id)
                    .localizedName()
                    .getDefault();
              } catch (ProgramNotFoundException e) {
                // Since this is just trying to get a csv representation of the programs
                // put a placeholder string if this exception occurs. Realistically at
                // this area of CiviForm it "shouldn't" occur, but why leave it up to
                // fate to raise an exception.
                logger.error(
                    "Unable to build complete string of programs. At least one program was not"
                        + " found",
                    e);
                return "<unknown>";
              }
            })
        .collect(Collectors.joining(", "));
  }
}
