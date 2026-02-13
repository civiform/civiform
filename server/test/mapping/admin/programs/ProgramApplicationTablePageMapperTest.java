package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import models.ApplicationModel;
import models.DisplayMode;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.LocalizedStrings;
import services.applicant.ApplicantService;
import services.pagination.PageNumberPaginationSpec;
import services.pagination.PaginationResult;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.statuses.StatusDefinitions;
import views.ApplicantUtils;
import views.admin.programs.ProgramApplicationTablePageViewModel;

public final class ProgramApplicationTablePageMapperTest {

  private ProgramApplicationTablePageMapper mapper;
  private DateConverter mockDateConverter;
  private ApplicantUtils mockApplicantUtils;
  private ApplicantService mockApplicantService;

  @Before
  public void setup() {
    mapper = new ProgramApplicationTablePageMapper();
    mockDateConverter = mock(DateConverter.class);
    mockApplicantUtils = mock(ApplicantUtils.class);
    mockApplicantService = mock(ApplicantService.class);
    when(mockDateConverter.renderDateTimeHumanReadable(any())).thenReturn("Jan 1, 2024");
  }

  private ProgramDefinition buildProgram() {
    return ProgramDefinition.builder()
        .setId(1L)
        .setAdminName("test-program")
        .setAdminDescription("desc")
        .setLocalizedName(LocalizedStrings.of(Locale.US, "Test Program"))
        .setLocalizedDescription(LocalizedStrings.of(Locale.US, "Desc"))
        .setLocalizedShortDescription(LocalizedStrings.of(Locale.US, "Short"))
        .setExternalLink("")
        .setDisplayMode(DisplayMode.PUBLIC)
        .setProgramType(ProgramType.DEFAULT)
        .setLocalizedConfirmationMessage(LocalizedStrings.empty())
        .setAcls(new ProgramAcls())
        .setBlockDefinitions(ImmutableList.of())
        .build();
  }

  @Test
  public void map_setsProgramAdminName() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 1, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of(),
            paginationSpec,
            paginationResult,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.getProgramAdminName()).isEqualTo("test-program");
  }

  @Test
  public void map_setsFormActionUrl() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 1, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of(),
            paginationSpec,
            paginationResult,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_noStatuses_displayStatusFalse() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 1, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of(),
            paginationSpec,
            paginationResult,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.isDisplayStatus()).isFalse();
  }

  @Test
  public void map_withStatuses_displayStatusTrue() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 1, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of("Approved", "Denied"),
            paginationSpec,
            paginationResult,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.isDisplayStatus()).isTrue();
  }

  @Test
  public void map_setsShowDownloadButton() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 1, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of(),
            paginationSpec,
            paginationResult,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.isShowDownloadButton()).isFalse();
  }

  @Test
  public void map_emptyApplications_setsEmptyApplicationRows() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 1, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of(),
            paginationSpec,
            paginationResult,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.getApplications()).isEmpty();
  }

  @Test
  public void map_setsFilterValues() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 1, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of(),
            paginationSpec,
            paginationResult,
            Optional.of("John"),
            Optional.of("2024-01-01"),
            Optional.of("2024-12-31"),
            Optional.of("Approved"),
            Optional.empty(),
            Optional.empty(),
            true,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.getSearchValue()).contains("John");
    assertThat(result.getFromDateValue()).contains("2024-01-01");
    assertThat(result.getUntilDateValue()).contains("2024-12-31");
    assertThat(result.getSelectedApplicationStatus()).contains("Approved");
  }

  @Test
  public void map_setsPaginationInfo() {
    ProgramDefinition program = buildProgram();
    StatusDefinitions statusDefinitions = new StatusDefinitions();
    PageNumberPaginationSpec paginationSpec =
        new PageNumberPaginationSpec(10, 2, PageNumberPaginationSpec.OrderByEnum.ID);
    PaginationResult<ApplicationModel> paginationResult =
        new PaginationResult<>(false, 3, ImmutableList.of());

    ProgramApplicationTablePageViewModel result =
        mapper.map(
            program,
            statusDefinitions,
            ImmutableList.of(),
            paginationSpec,
            paginationResult,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true,
            mockDateConverter,
            mockApplicantUtils,
            mockApplicantService);

    assertThat(result.getCurrentPage()).isEqualTo(2);
    assertThat(result.getTotalPages()).isEqualTo(3);
    assertThat(result.getPaginationLinks()).hasSize(3);
  }
}
