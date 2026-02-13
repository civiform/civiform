package mapping.admin.programs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import services.DateConverter;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramApplicationShowPageViewModel;

public final class ProgramApplicationShowPageMapperTest {

  private ProgramApplicationShowPageMapper mapper;
  private DateConverter mockDateConverter;

  @Before
  public void setup() {
    mapper = new ProgramApplicationShowPageMapper();
    mockDateConverter = mock(DateConverter.class);
    when(mockDateConverter.renderDateTimeHumanReadable(any())).thenReturn("Jan 1, 2024");
    when(mockDateConverter.renderDate(any())).thenReturn("2024-01-01");
  }

  private ApplicationModel buildMockApplication() {
    ApplicationModel application = mock(ApplicationModel.class);
    ApplicantModel applicant = mock(ApplicantModel.class);
    AccountModel account = mock(AccountModel.class);
    ProgramModel programModel = mock(ProgramModel.class);

    when(application.getApplicant()).thenReturn(applicant);
    when(applicant.getAccount()).thenReturn(account);
    when(account.getEmailAddress()).thenReturn("test@example.com");
    when(applicant.getEmailAddress()).thenReturn(Optional.empty());
    when(application.getLatestStatus()).thenReturn(Optional.empty());
    when(application.getSubmitTime()).thenReturn(null);
    when(application.getSubmitterEmail()).thenReturn(Optional.empty());
    when(application.getProgram()).thenReturn(programModel);
    programModel.id = 1L;
    application.id = 100L;

    return application;
  }

  @Test
  public void map_setsProgramInfo() {
    ApplicationModel application = buildMockApplication();

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getProgramId()).isEqualTo(1L);
    assertThat(result.getProgramName()).isEqualTo("Test Program");
  }

  @Test
  public void map_setsApplicantInfo() {
    ApplicationModel application = buildMockApplication();

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getApplicantNameWithApplicationId()).isEqualTo("John Doe (100)");
    assertThat(result.getApplicationId()).isEqualTo(100L);
  }

  @Test
  public void map_withNullSubmitTime_setsDefaultMessage() {
    ApplicationModel application = buildMockApplication();
    when(application.getSubmitTime()).thenReturn(null);

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getSubmitTime())
        .isEqualTo("Application submitted without submission time marked.");
  }

  @Test
  public void map_noStatuses_hasStatusesFalse() {
    ApplicationModel application = buildMockApplication();

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.isHasStatuses()).isFalse();
    assertThat(result.getStatusOptions()).isEmpty();
  }

  @Test
  public void map_withStatuses_hasStatusesTrue() {
    ApplicationModel application = buildMockApplication();
    StatusDefinitions statusDefinitions =
        new StatusDefinitions(
            ImmutableList.of(
                StatusDefinitions.Status.builder()
                    .setStatusText("Approved")
                    .setLocalizedStatusText(services.LocalizedStrings.withDefaultValue("Approved"))
                    .setLocalizedEmailBodyText(Optional.empty())
                    .setDefaultStatus(Optional.of(false))
                    .build()));

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            statusDefinitions,
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.isHasStatuses()).isTrue();
    assertThat(result.getStatusOptions()).hasSize(1);
    assertThat(result.getStatusOptions().get(0).getStatusText()).isEqualTo("Approved");
  }

  @Test
  public void map_withShowDownloadButton_setsDownloadUrl() {
    ApplicationModel application = buildMockApplication();

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            true,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.isShowDownloadButton()).isTrue();
    assertThat(result.getDownloadUrl()).isPresent();
  }

  @Test
  public void map_withoutShowDownloadButton_noDownloadUrl() {
    ApplicationModel application = buildMockApplication();

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.isShowDownloadButton()).isFalse();
    assertThat(result.getDownloadUrl()).isEmpty();
  }

  @Test
  public void map_setsNote() {
    ApplicationModel application = buildMockApplication();

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.of("A note about this application"),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.getNote()).contains("A note about this application");
  }

  @Test
  public void map_setsSuccessMessage() {
    ApplicationModel application = buildMockApplication();

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("Status updated!"));

    assertThat(result.getSuccessMessage()).contains("Status updated!");
  }

  @Test
  public void map_noLatestStatus_setsNoCurrentStatusTrue() {
    ApplicationModel application = buildMockApplication();
    when(application.getLatestStatus()).thenReturn(Optional.empty());

    ProgramApplicationShowPageViewModel result =
        mapper.map(
            1L,
            "Test Program",
            application,
            "John Doe (100)",
            ImmutableList.of(),
            ImmutableList.of(),
            new StatusDefinitions(),
            Optional.empty(),
            false,
            false,
            mockDateConverter,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(result.isNoCurrentStatus()).isTrue();
    assertThat(result.getCurrentStatusDisplay()).isEmpty();
  }
}
