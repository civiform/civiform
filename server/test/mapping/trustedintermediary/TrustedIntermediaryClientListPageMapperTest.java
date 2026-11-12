package mapping.trustedintermediary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.ti.routes;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.ApplicationStep;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import repository.SearchParameters;
import services.DateConverter;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import views.admin.shared.LegacyPagination;
import views.trustedintermediary.TrustedIntermediaryClientListPageViewModel;
import views.trustedintermediary.TrustedIntermediaryClientListPageViewModel.Client;
import views.trustedintermediary.TrustedIntermediaryClientListPageViewModel.ClientCountHeading;

public final class TrustedIntermediaryClientListPageMapperTest {

  private static final String BASE_URL = "https://civiform.example.com";
  private static final long TI_GROUP_ID = 5L;
  private static final SearchParameters NO_SEARCH = search("", "", "", "");

  private TrustedIntermediaryClientListPageMapper mapper;
  private ProgramService programService;
  private DateConverter dateConverter;
  private TrustedIntermediaryGroupModel tiGroup;

  @Before
  public void setup() {
    mapper = new TrustedIntermediaryClientListPageMapper();
    programService = mock(ProgramService.class);
    dateConverter = new DateConverter(Clock.systemUTC());

    tiGroup = mock(TrustedIntermediaryGroupModel.class);
    tiGroup.id = TI_GROUP_ID;
    when(tiGroup.getName()).thenReturn("Food Bank");
  }

  private static SearchParameters search(String name, String day, String month, String year) {
    return SearchParameters.builder()
        .setNameQuery(Optional.of(name))
        .setDayQuery(Optional.of(day))
        .setMonthQuery(Optional.of(month))
        .setYearQuery(Optional.of(year))
        .build();
  }

  private static AccountModel account(long id, String displayName) {
    AccountModel account = mock(AccountModel.class);
    account.id = id;
    when(account.getApplicantDisplayName()).thenReturn(displayName);
    when(account.getTiNote()).thenReturn("");
    when(account.representativeApplicant()).thenReturn(Optional.empty());

    return account;
  }

  private static ApplicantModel applicantFor(AccountModel account, long applicantId) {
    ApplicantModel applicant = mock(ApplicantModel.class);
    applicant.id = applicantId;
    when(applicant.getDateOfBirth()).thenReturn(Optional.empty());
    when(applicant.getPhoneNumber()).thenReturn(Optional.empty());
    when(applicant.getEmailAddress()).thenReturn(Optional.empty());
    when(applicant.getApplications()).thenReturn(ImmutableList.of());
    when(account.representativeApplicant()).thenReturn(Optional.of(applicant));

    return applicant;
  }

  private ApplicationModel application(long programId, String programName, LifecycleStage stage)
      throws ProgramNotFoundException {
    ProgramModel program = mock(ProgramModel.class);
    program.id = programId;

    when(programService.getFullProgramDefinition(programId))
        .thenReturn(programDefinition(programId, programName));

    ApplicationModel application = mock(ApplicationModel.class);
    when(application.getProgram()).thenReturn(program);
    when(application.getLifecycleStage()).thenReturn(stage);

    return application;
  }

  private static ProgramDefinition programDefinition(long programId, String programName) {
    return ProgramDefinition.builder()
        .setId(programId)
        .setAdminName("program-" + programId)
        .setAdminDescription("")
        .setLocalizedName(LocalizedStrings.withDefaultValue(programName))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
        .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("short description"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setLastModifiedTime(Instant.parse("2024-01-01T17:00:00Z"))
        .setApplicationSteps(ImmutableList.of(new ApplicationStep("title", "description")))
        .setBridgeDefinitions(ImmutableMap.of())
        .build();
  }

  private TrustedIntermediaryClientListPageViewModel map(
      ImmutableList<AccountModel> managedAccounts, SearchParameters searchParameters) {
    return mapper.map(
        tiGroup,
        managedAccounts,
        /* totalPageCount= */ 1,
        /* page= */ 1,
        searchParameters,
        BASE_URL,
        programService,
        dateConverter,
        /* errorMessage= */ Optional.empty());
  }

  private Client mapClient(AccountModel account) {
    return map(ImmutableList.of(account), NO_SEARCH).getClients().get(0);
  }

  @Test
  public void map_setsTiGroupName() {
    TrustedIntermediaryClientListPageViewModel result = map(ImmutableList.of(), NO_SEARCH);

    assertThat(result.getTiGroupName()).isEqualTo("Food Bank");
  }

  @Test
  public void map_prefixesTabUrlsWithBaseUrl() {
    TrustedIntermediaryClientListPageViewModel result = map(ImmutableList.of(), NO_SEARCH);

    assertThat(result.getClientListUrl())
        .isEqualTo(
            BASE_URL
                + routes.TrustedIntermediaryController.dashboard(
                        /* nameQuery= */ Optional.empty(),
                        /* dayQuery= */ Optional.empty(),
                        /* monthQuery= */ Optional.empty(),
                        /* yearQuery= */ Optional.empty(),
                        /* page= */ Optional.of(1))
                    .url());
    assertThat(result.getAccountSettingsUrl())
        .isEqualTo(BASE_URL + routes.TrustedIntermediaryController.accountSettings().url());
  }

  @Test
  public void map_setsAddClientUrl() {
    TrustedIntermediaryClientListPageViewModel result = map(ImmutableList.of(), NO_SEARCH);

    assertThat(result.getAddClientUrl())
        .isEqualTo(routes.TrustedIntermediaryController.showAddClientForm(TI_GROUP_ID).url());
  }

  @Test
  public void map_setsSearchUrlWithoutQueryOrPage() {
    TrustedIntermediaryClientListPageViewModel result =
        map(ImmutableList.of(), search("Gu", "", "", ""));

    assertThat(result.getSearchUrl())
        .isEqualTo(
            routes.TrustedIntermediaryController.dashboard(
                    /* nameQuery= */ Optional.empty(),
                    /* dayQuery= */ Optional.empty(),
                    /* monthQuery= */ Optional.empty(),
                    /* yearQuery= */ Optional.empty(),
                    /* page= */ Optional.empty())
                .url());
  }

  @Test
  public void map_absentSearchParameters_setsEmptyQueries() {
    SearchParameters absent =
        SearchParameters.builder()
            .setNameQuery(Optional.empty())
            .setDayQuery(Optional.empty())
            .setMonthQuery(Optional.empty())
            .setYearQuery(Optional.empty())
            .build();

    TrustedIntermediaryClientListPageViewModel result = map(ImmutableList.of(), absent);

    assertThat(result.getNameQuery()).isEmpty();
    assertThat(result.getDayQuery()).isEmpty();
    assertThat(result.getMonthQuery()).isEmpty();
    assertThat(result.getYearQuery()).isEmpty();
  }

  @Test
  public void map_keepsSearchQueries() {
    TrustedIntermediaryClientListPageViewModel result =
        map(ImmutableList.of(), search("Gu", "28", "1", "1986"));

    assertThat(result.getNameQuery()).isEqualTo("Gu");
    assertThat(result.getDayQuery()).isEqualTo("28");
    assertThat(result.getMonthQuery()).isEqualTo("1");
    assertThat(result.getYearQuery()).isEqualTo("1986");
  }

  @Test
  public void map_noSearch_isValidSearchWithAllClientsHeading() {
    TrustedIntermediaryClientListPageViewModel result =
        map(ImmutableList.of(account(1L, "Gu, Darren")), NO_SEARCH);

    assertThat(result.isValidSearch()).isTrue();
    assertThat(result.getClientCountHeading()).isEqualTo(ClientCountHeading.ALL_CLIENTS);
  }

  @Test
  public void map_searchWithOneClient_setsOneClientHeading() {
    TrustedIntermediaryClientListPageViewModel result =
        map(ImmutableList.of(account(1L, "Gu, Darren")), search("Gu", "", "", ""));

    assertThat(result.isValidSearch()).isTrue();
    assertThat(result.getClientCountHeading()).isEqualTo(ClientCountHeading.ONE_CLIENT);
  }

  @Test
  public void map_searchWithSeveralClients_setsMultipleClientsHeadingAndCount() {
    TrustedIntermediaryClientListPageViewModel result =
        map(
            ImmutableList.of(account(1L, "Gu, Darren"), account(2L, "Gu, Mei")),
            search("Gu", "", "", ""));

    assertThat(result.getClientCountHeading()).isEqualTo(ClientCountHeading.MULTIPLE_CLIENTS);
    assertThat(result.getClientCount()).isEqualTo(2);
  }

  @Test
  public void map_searchWithNoClients_setsMultipleClientsHeading() {
    TrustedIntermediaryClientListPageViewModel result =
        map(ImmutableList.of(), search("Gu", "", "", ""));

    assertThat(result.getClientCountHeading()).isEqualTo(ClientCountHeading.MULTIPLE_CLIENTS);
    assertThat(result.getClientCount()).isZero();
  }

  @Test
  public void map_partialDateOfBirthWithoutName_isInvalidSearch() {
    TrustedIntermediaryClientListPageViewModel result =
        map(ImmutableList.of(), search("", "28", "", "1986"));

    assertThat(result.isValidSearch()).isFalse();
  }

  @Test
  public void map_partialDateOfBirthWithName_isValidSearch() {
    TrustedIntermediaryClientListPageViewModel result =
        map(ImmutableList.of(), search("Gu", "28", "", "1986"));

    assertThat(result.isValidSearch()).isTrue();
  }

  @Test
  public void map_sortsClientsByDisplayName() {
    TrustedIntermediaryClientListPageViewModel result =
        map(
            ImmutableList.of(
                account(1L, "Smith, Zoe"), account(2L, "Gu, Darren"), account(3L, "Lee, Ann")),
            NO_SEARCH);

    assertThat(result.getClients())
        .extracting(Client::getDisplayName)
        .containsExactly("Gu, Darren", "Lee, Ann", "Smith, Zoe");
  }

  @Test
  public void map_setsClientEditUrlAndNotes() {
    AccountModel account = account(7L, "Gu, Darren");
    when(account.getTiNote()).thenReturn("Prefers phone calls");

    Client result = mapClient(account);

    assertThat(result.getEditUrl())
        .isEqualTo(routes.TrustedIntermediaryController.showEditClientForm(7L).url());
    assertThat(result.getNotes()).isEqualTo("Prefers phone calls");
  }

  @Test
  public void map_clientWithoutApplicant_leavesApplicantFieldsUnset() {
    Client result = mapClient(account(7L, "<Unnamed User>"));

    assertThat(result.isHasApplicant()).isFalse();
    assertThat(result.getApplicantId()).isNull();
    assertThat(result.getApplicationsUrl()).isNull();
    assertThat(result.getDateOfBirth()).isNull();
    assertThat(result.getPhoneNumber()).isNull();
    assertThat(result.getEmailAddress()).isNull();
    assertThat(result.getProgramNames()).isNull();
  }

  @Test
  public void map_clientWithApplicant_setsApplicantIdAndApplicationsUrl() {
    AccountModel account = account(7L, "Gu, Darren");
    applicantFor(account, 42L);

    Client result = mapClient(account);

    assertThat(result.isHasApplicant()).isTrue();
    assertThat(result.getApplicantId()).isEqualTo(42L);
    assertThat(result.getApplicationsUrl())
        .isEqualTo(
            controllers.applicant.routes.ApplicantProgramsController.indexWithApplicantId(
                    42L, /* categories= */ ImmutableList.of())
                .url());
  }

  @Test
  public void map_formatsDateOfBirthAsIsoDate() {
    AccountModel account = account(7L, "Gu, Darren");
    ApplicantModel applicant = applicantFor(account, 42L);
    when(applicant.getDateOfBirth()).thenReturn(Optional.of(LocalDate.of(1986, 1, 28)));

    Client result = mapClient(account);

    assertThat(result.getDateOfBirth()).isEqualTo("1986-01-28");
  }

  @Test
  public void map_noDateOfBirth_setsEmptyDateOfBirth() {
    AccountModel account = account(7L, "Gu, Darren");
    applicantFor(account, 42L);

    Client result = mapClient(account);

    assertThat(result.getDateOfBirth()).isEmpty();
  }

  @Test
  public void map_formatsPhoneNumberInNationalFormat() {
    AccountModel account = account(7L, "Gu, Darren");
    ApplicantModel applicant = applicantFor(account, 42L);
    when(applicant.getPhoneNumber()).thenReturn(Optional.of("4256373270"));

    Client result = mapClient(account);

    assertThat(result.getPhoneNumber()).isEqualTo("(425) 637-3270");
  }

  @Test
  public void map_unparseablePhoneNumber_setsDash() {
    AccountModel account = account(7L, "Gu, Darren");
    ApplicantModel applicant = applicantFor(account, 42L);
    when(applicant.getPhoneNumber()).thenReturn(Optional.of("not a phone number"));

    Client result = mapClient(account);

    assertThat(result.getPhoneNumber()).isEqualTo("-");
  }

  @Test
  public void map_noPhoneNumberOrEmail_setsNull() {
    AccountModel account = account(7L, "Gu, Darren");
    applicantFor(account, 42L);

    Client result = mapClient(account);

    assertThat(result.getPhoneNumber()).isNull();
    assertThat(result.getEmailAddress()).isNull();
  }

  @Test
  public void map_setsEmailAddress() {
    AccountModel account = account(7L, "Gu, Darren");
    ApplicantModel applicant = applicantFor(account, 42L);
    when(applicant.getEmailAddress()).thenReturn(Optional.of("darren@example.com"));

    Client result = mapClient(account);

    assertThat(result.getEmailAddress()).isEqualTo("darren@example.com");
  }

  @Test
  public void map_countsAndNamesOnlyActiveApplications() throws ProgramNotFoundException {
    AccountModel account = account(7L, "Gu, Darren");
    ApplicantModel applicant = applicantFor(account, 42L);
    ImmutableList<ApplicationModel> applications =
        ImmutableList.of(
            application(1L, "Food Assistance", LifecycleStage.ACTIVE),
            application(2L, "Housing Assistance", LifecycleStage.DRAFT),
            application(3L, "Utility Discount", LifecycleStage.ACTIVE));
    when(applicant.getApplications()).thenReturn(applications);

    Client result = mapClient(account);

    assertThat(result.getApplicationCount()).isEqualTo(2);
    assertThat(result.getProgramNames()).isEqualTo("Food Assistance, Utility Discount");
  }

  @Test
  public void map_noApplications_setsZeroCountAndEmptyProgramNames() {
    AccountModel account = account(7L, "Gu, Darren");
    applicantFor(account, 42L);

    Client result = mapClient(account);

    assertThat(result.getApplicationCount()).isZero();
    assertThat(result.getProgramNames()).isEmpty();
  }

  @Test
  public void map_programNotFound_usesPlaceholderName() throws ProgramNotFoundException {
    AccountModel account = account(7L, "Gu, Darren");
    ApplicantModel applicant = applicantFor(account, 42L);
    ImmutableList<ApplicationModel> applications =
        ImmutableList.of(application(1L, "Food Assistance", LifecycleStage.ACTIVE));
    when(applicant.getApplications()).thenReturn(applications);
    when(programService.getFullProgramDefinition(1L)).thenThrow(new ProgramNotFoundException(1L));

    Client result = mapClient(account);

    assertThat(result.getProgramNames()).isEqualTo("<unknown>");
  }

  @Test
  public void map_noClients_setsNullPagination() {
    TrustedIntermediaryClientListPageViewModel result = map(ImmutableList.of(), NO_SEARCH);

    assertThat(result.getClients()).isEmpty();
    assertThat(result.getPagination()).isNull();
  }

  @Test
  public void map_withClients_buildsPaginationThatKeepsTheSearch() {
    SearchParameters searchParameters = search("Gu", "", "", "");

    TrustedIntermediaryClientListPageViewModel result =
        mapper.map(
            tiGroup,
            ImmutableList.of(account(1L, "Gu, Darren")),
            /* totalPageCount= */ 3,
            /* page= */ 2,
            searchParameters,
            BASE_URL,
            programService,
            dateConverter,
            /* errorMessage= */ Optional.empty());
    LegacyPagination pagination = result.getPagination();

    assertThat(pagination.getPreviousPageUrl()).isEqualTo(pageUrl(searchParameters, 1));
    assertThat(pagination.getNextPageUrl()).isEqualTo(pageUrl(searchParameters, 3));
    assertThat(pagination.getItems())
        .extracting(LegacyPagination.Item::getPageNumber)
        .containsExactly(1, 2, 3);
    assertThat(pagination.getItems())
        .extracting(LegacyPagination.Item::isCurrent)
        .containsExactly(false, true, false);
  }

  private static String pageUrl(SearchParameters searchParameters, int page) {
    return routes.TrustedIntermediaryController.dashboard(
            searchParameters.nameQuery(),
            searchParameters.dayQuery(),
            searchParameters.monthQuery(),
            searchParameters.yearQuery(),
            Optional.of(page))
        .url();
  }

  @Test
  public void map_noError_setsNullErrorToast() {
    TrustedIntermediaryClientListPageViewModel result = map(ImmutableList.of(), NO_SEARCH);

    assertThat(result.getErrorToastId()).isNull();
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void map_error_prefixesErrorMessageAndSetsToastId() {
    TrustedIntermediaryClientListPageViewModel result =
        mapper.map(
            tiGroup,
            ImmutableList.of(),
            /* totalPageCount= */ 1,
            /* page= */ 1,
            NO_SEARCH,
            BASE_URL,
            programService,
            dateConverter,
            /* errorMessage= */ Optional.of("Please enter a valid birth date."));

    assertThat(result.getErrorToastId()).isNotBlank();
    assertThat(result.getErrorMessage()).isEqualTo("Error: Please enter a valid birth date.");
  }
}
