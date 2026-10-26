package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Optional;
import models.ApplicantModel;
import models.ApplicationModel;
import models.ApplicationStep;
import models.DisplayMode;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Messages;
import repository.SubmittedApplicationFilter;
import services.DateConverter;
import services.LocalizedStrings;
import services.applicant.ApplicantService;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.PaginationResult;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramApplicationListPageViewModel;
import views.admin.programs.ProgramApplicationListPageViewModel.ApplicationRow;
import views.admin.programs.ProgramApplicationTableView.RenderFilterParams;

public final class ProgramApplicationListPageMapperTest {

  private static final long PROGRAM_ID = 1L;
  private static final long APPLICATION_ID = 7L;
  private static final Instant SUBMIT_TIME = Instant.parse("2024-01-01T17:00:00Z");

  private static final StatusDefinitions.Status APPROVED =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .setDefaultStatus(Optional.of(true))
          .build();
  private static final StatusDefinitions.Status REJECTED =
      StatusDefinitions.Status.builder()
          .setStatusText("Rejected")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Rejected"))
          .build();
  private static final StatusDefinitions STATUSES =
      new StatusDefinitions(ImmutableList.of(APPROVED, REJECTED));

  private static final RenderFilterParams NO_FILTERS =
      RenderFilterParams.builder()
          .setSearch(Optional.empty())
          .setFromDate(Optional.empty())
          .setUntilDate(Optional.empty())
          .setSelectedApplicationStatus(Optional.empty())
          .build();
  private static final RenderFilterParams ALL_FILTERS =
      RenderFilterParams.builder()
          .setSearch(Optional.of("SARA"))
          .setFromDate(Optional.of("2024-01-01"))
          .setUntilDate(Optional.of("2024-12-31"))
          .setSelectedApplicationStatus(Optional.of("Approved"))
          .build();

  private ProgramApplicationListPageMapper mapper;
  private ProgramDefinition program;
  private ApplicationModel application;
  private ApplicantService applicantService;
  private DateConverter dateConverter;
  private Messages enUsMessages;

  @Before
  public void setup() {
    mapper = new ProgramApplicationListPageMapper();

    program = programBuilder().build();

    ProgramModel programModel = mock(ProgramModel.class);
    programModel.id = PROGRAM_ID;
    ApplicantModel applicant = mock(ApplicantModel.class);
    when(applicant.getApplicantDisplayName()).thenReturn(Optional.of("Smith, Sarah"));

    application = mock(ApplicationModel.class);
    application.id = APPLICATION_ID;
    when(application.getProgram()).thenReturn(programModel);
    when(application.getApplicant()).thenReturn(applicant);
    when(application.getLatestStatus()).thenReturn(Optional.of("Rejected"));
    when(application.getSubmitTime()).thenReturn(SUBMIT_TIME);
    when(application.getSubmitterEmail()).thenReturn(Optional.of("ti@example.com"));

    applicantService = mock(ApplicantService.class);
    when(applicantService.getApplicationEligibilityStatus(application, program))
        .thenReturn(Optional.of(true));

    dateConverter = mock(DateConverter.class);
    when(dateConverter.renderDateTimeHumanReadable(SUBMIT_TIME))
        .thenReturn("2024/01/01 at 9:00 AM PST");

    enUsMessages = mock(Messages.class);
    when(enUsMessages.at("placeholder.noDropdownSelection")).thenReturn("Choose an option:");
    when(enUsMessages.at("guest")).thenReturn("Guest");
  }

  private static ProgramDefinition.Builder programBuilder() {
    return ProgramDefinition.builder()
        .setId(PROGRAM_ID)
        .setAdminName("utility-discount")
        .setAdminDescription("")
        .setLocalizedName(LocalizedStrings.withDefaultValue("Utility Discount"))
        .setLocalizedDescription(LocalizedStrings.withDefaultValue("description"))
        .setLocalizedShortDescription(LocalizedStrings.withDefaultValue("Help with utilities"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setEligibilityIsGating(true)
        .setLoginOnly(false)
        .setAcls(new ProgramAcls())
        .setCategories(ImmutableList.of())
        .setApplicationSteps(ImmutableList.of(new ApplicationStep("title", "description")))
        .setBridgeDefinitions(ImmutableMap.of());
  }

  private ProgramApplicationListPageViewModel map(
      RenderFilterParams filterParams, int currentPage, int numPages) {
    return mapper.map(
        program,
        STATUSES,
        ImmutableList.of("Approved", "Rejected"),
        new PageNumberPaginationSpec(
            /* pageSize= */ 10, currentPage, PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
        new PaginationResult<>(
            /* hasNext= */ currentPage < numPages, numPages, ImmutableList.of(application)),
        filterParams,
        /* showDownloadButton= */ true,
        /* showDownloadModal= */ false,
        /* message= */ Optional.empty(),
        applicantService,
        dateConverter,
        enUsMessages);
  }

  private ProgramApplicationListPageViewModel map() {
    return map(NO_FILTERS, /* currentPage= */ 1, /* numPages= */ 1);
  }

  private ApplicationRow onlyRow(ProgramApplicationListPageViewModel model) {
    assertThat(model.getApplicationRows()).hasSize(1);
    return model.getApplicationRows().get(0);
  }

  @Test
  public void map_setsProgramName() {
    assertThat(map().getProgramName()).isEqualTo("utility-discount");
  }

  @Test
  public void map_setsUrls() {
    ProgramApplicationListPageViewModel model = map();

    assertThat(model.getIndexUrl()).isEqualTo("/admin/programs/1/applications");
    assertThat(model.getDownloadAllCsvUrl()).isEqualTo("/admin/programs/1/applications/all");
    assertThat(model.getDownloadAllJsonUrl()).isEqualTo("/admin/programs/1/applications/allJson");
    assertThat(model.getUpdateStatusesUrl())
        .isEqualTo("/admin/programs/1/applications/updateStatuses");
  }

  @Test
  public void map_defaultsFilterValuesToEmptyStrings() {
    ProgramApplicationListPageViewModel model = map();

    assertThat(model.getFromDate()).isEmpty();
    assertThat(model.getUntilDate()).isEmpty();
    assertThat(model.getSearch()).isEmpty();
    assertThat(model.getSelectedApplicationStatus()).isEmpty();
  }

  @Test
  public void map_setsFilterValues() {
    ProgramApplicationListPageViewModel model =
        map(ALL_FILTERS, /* currentPage= */ 1, /* numPages= */ 1);

    assertThat(model.getFromDate()).isEqualTo("2024-01-01");
    assertThat(model.getUntilDate()).isEqualTo("2024-12-31");
    assertThat(model.getSearch()).isEqualTo("SARA");
    assertThat(model.getSelectedApplicationStatus()).isEqualTo("Approved");
  }

  @Test
  public void map_setsStatusFilterOptions() {
    ProgramApplicationListPageViewModel model = map();

    assertThat(model.getApplicationStatusOptions()).containsExactly("Approved", "Rejected");
    assertThat(model.hasApplicationStatuses()).isTrue();
    assertThat(model.getNoStatusFilterOptionValue())
        .isEqualTo(SubmittedApplicationFilter.NO_STATUS_FILTERS_OPTION_UUID);
  }

  @Test
  public void map_noStatusesEver_hidesStatusControls() {
    ProgramApplicationListPageViewModel model =
        mapper.map(
            program,
            new StatusDefinitions(),
            ImmutableList.of(),
            new PageNumberPaginationSpec(
                /* pageSize= */ 10,
                /* currentPage= */ 1,
                PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            new PaginationResult<>(
                /* hasNext= */ false, /* numPages= */ 1, ImmutableList.of(application)),
            NO_FILTERS,
            /* showDownloadButton= */ true,
            /* showDownloadModal= */ false,
            /* message= */ Optional.empty(),
            applicantService,
            dateConverter,
            enUsMessages);

    assertThat(model.hasApplicationStatuses()).isFalse();
    assertThat(model.getBulkStatusOptions()).isEmpty();
    assertThat(onlyRow(model).getStatusText()).isEqualTo("Rejected");
  }

  @Test
  public void map_setsBulkStatusOptionsAndPlaceholder() {
    ProgramApplicationListPageViewModel model = map();

    assertThat(model.getBulkStatusPlaceholder()).isEqualTo("Choose an option:");
    assertThat(model.getBulkStatusOptions()).containsExactly("Approved", "Rejected");
  }

  @Test
  public void map_setsDownloadFlags() {
    ProgramApplicationListPageViewModel model =
        mapper.map(
            program,
            STATUSES,
            ImmutableList.of("Approved"),
            new PageNumberPaginationSpec(
                /* pageSize= */ 10,
                /* currentPage= */ 1,
                PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            new PaginationResult<>(/* hasNext= */ false, /* numPages= */ 1, ImmutableList.of()),
            NO_FILTERS,
            /* showDownloadButton= */ false,
            /* showDownloadModal= */ true,
            /* message= */ Optional.empty(),
            applicantService,
            dateConverter,
            enUsMessages);

    assertThat(model.isShowDownloadButton()).isFalse();
    assertThat(model.isShowDownloadModal()).isTrue();
    assertThat(model.getApplicationRows()).isEmpty();
  }

  @Test
  public void map_setsStatusUpdateMessage() {
    ProgramApplicationListPageViewModel model =
        mapper.map(
            program,
            STATUSES,
            ImmutableList.of("Approved"),
            new PageNumberPaginationSpec(
                /* pageSize= */ 10,
                /* currentPage= */ 1,
                PageNumberPaginationSpec.OrderByEnum.SUBMIT_TIME),
            new PaginationResult<>(/* hasNext= */ false, /* numPages= */ 1, ImmutableList.of()),
            NO_FILTERS,
            /* showDownloadButton= */ true,
            /* showDownloadModal= */ false,
            Optional.of("Status updated for 2 applications"),
            applicantService,
            dateConverter,
            enUsMessages);

    assertThat(model.getStatusUpdateMessage()).isEqualTo("Status updated for 2 applications");
  }

  @Test
  public void map_noMessage_leavesStatusUpdateMessageNull() {
    assertThat(map().getStatusUpdateMessage()).isNull();
  }

  @Test
  public void map_setsEligibilityEnabled() {
    BlockDefinition eligibilityBlock =
        BlockDefinition.builder()
            .setId(1L)
            .setName("Screen 1")
            .setDescription("Screen 1 description")
            .setLocalizedName(LocalizedStrings.withDefaultValue("Screen 1"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("Screen 1 description"))
            .setEligibilityDefinition(
                EligibilityDefinition.builder()
                    .setPredicate(
                        PredicateDefinition.create(
                            PredicateExpressionNode.create(
                                LeafAddressServiceAreaExpressionNode.create(
                                    /* questionId= */ 1L, "seattle", Operator.IN_SERVICE_AREA)),
                            PredicateAction.ELIGIBLE_BLOCK))
                    .build())
            .build();
    program = programBuilder().addBlockDefinition(eligibilityBlock).build();
    when(applicantService.getApplicationEligibilityStatus(application, program))
        .thenReturn(Optional.of(true));

    assertThat(map().isEligibilityEnabled()).isTrue();
  }

  @Test
  public void map_buildsApplicationRow() {
    ApplicationRow row = onlyRow(map());

    assertThat(row.getId()).isEqualTo(APPLICATION_ID);
    assertThat(row.getApplicantNameWithApplicationId()).isEqualTo("Smith, Sarah (7)");
    assertThat(row.isMeetsEligibility()).isTrue();
    assertThat(row.getStatusText()).isEqualTo("Rejected");
    assertThat(row.getSubmitTime()).isEqualTo("2024/01/01 at 9:00 AM PST");
    assertThat(row.getSubmitterEmail()).isEqualTo("ti@example.com");
  }

  @Test
  public void map_rowViewUrl_carriesFiltersAndPage() {
    ApplicationRow row = onlyRow(map(ALL_FILTERS, /* currentPage= */ 2, /* numPages= */ 3));

    assertThat(row.getViewUrl())
        .startsWith("/admin/programs/1/applications/7?")
        .contains("search=SARA")
        .contains("fromDate=2024-01-01")
        .contains("toDate=2024-12-31")
        .contains("page=2")
        .contains("selectedApplicationStatus=Approved");
  }

  @Test
  public void map_guestApplicant_usesEnUsGuestName() {
    ApplicantModel guest = mock(ApplicantModel.class);
    when(guest.getApplicantDisplayName()).thenReturn(Optional.empty());
    when(application.getApplicant()).thenReturn(guest);

    assertThat(onlyRow(map()).getApplicantNameWithApplicationId()).isEqualTo("Guest (7)");
  }

  @Test
  public void map_defaultStatus_isMarked() {
    when(application.getLatestStatus()).thenReturn(Optional.of("Approved"));

    assertThat(onlyRow(map()).getStatusText()).isEqualTo("Approved (default)");
  }

  @Test
  public void map_noLatestStatus_rendersNone() {
    when(application.getLatestStatus()).thenReturn(Optional.empty());

    assertThat(onlyRow(map()).getStatusText()).isEqualTo("None");
  }

  @Test
  public void map_unknownEligibility_doesNotMeetEligibility() {
    when(applicantService.getApplicationEligibilityStatus(application, program))
        .thenReturn(Optional.empty());

    assertThat(onlyRow(map()).isMeetsEligibility()).isFalse();
  }

  @Test
  public void map_missingSubmitTime_rendersEmptyString() {
    when(application.getSubmitTime()).thenReturn(null);

    assertThat(onlyRow(map()).getSubmitTime()).isEmpty();
  }

  @Test
  public void map_missingSubmitterEmail_rendersEmptyString() {
    when(application.getSubmitterEmail()).thenReturn(Optional.empty());

    assertThat(onlyRow(map()).getSubmitterEmail()).isEmpty();
  }

  @Test
  public void map_singlePage_hasNoPagination() {
    assertThat(map().getPagination()).isNull();
  }

  @Test
  public void map_multiplePages_buildsPaginationWithFilters() {
    ProgramApplicationListPageViewModel model =
        map(ALL_FILTERS, /* currentPage= */ 2, /* numPages= */ 3);

    assertThat(model.getPagination()).isNotNull();
    assertThat(model.getPagination().getItems()).hasSize(3);
    assertThat(model.getPagination().getItems().get(1).isCurrent()).isTrue();
    assertThat(model.getPagination().getPreviousPageUrl())
        .startsWith("/admin/programs/1/applications?")
        .contains("search=SARA")
        .contains("page=1")
        .contains("applicationStatus=Approved");
    assertThat(model.getPagination().getNextPageUrl()).contains("page=3");
  }
}
