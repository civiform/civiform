package mapping.ti;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import services.DateConverter;
import services.settings.SettingsManifest;
import views.trustedintermediary.TiEditClientPageViewModel;

public final class TiEditClientPageMapperTest {

  private TiEditClientPageMapper mapper;
  private DateConverter mockDateConverter;
  private SettingsManifest mockSettingsManifest;
  private Http.Request mockRequest;
  private TrustedIntermediaryGroupModel mockTiGroup;

  @Before
  public void setup() {
    mockDateConverter = mock(DateConverter.class);
    mockSettingsManifest = mock(SettingsManifest.class);
    mockRequest = mock(Http.Request.class);
    mapper = new TiEditClientPageMapper(mockDateConverter, mockSettingsManifest);

    mockTiGroup = mock(TrustedIntermediaryGroupModel.class);
    when(mockTiGroup.getName()).thenReturn("TI Org");
    when(mockSettingsManifest.getNameSuffixDropdownEnabled(mockRequest)).thenReturn(false);
  }

  @Test
  public void map_setsBasicFields() {
    TiEditClientPageViewModel result =
        mapper.map(
            mockTiGroup, false, "/submit/action", Optional.empty(), Optional.empty(), mockRequest);

    assertThat(result.getTiGroupName()).isEqualTo("TI Org");
    assertThat(result.isEdit()).isFalse();
    assertThat(result.getFormActionUrl()).isEqualTo("/submit/action");
  }

  @Test
  public void map_editMode_setsEditTrue() {
    TiEditClientPageViewModel result =
        mapper.map(
            mockTiGroup, true, "/submit/edit", Optional.empty(), Optional.empty(), mockRequest);

    assertThat(result.isEdit()).isTrue();
  }

  @Test
  public void map_setsUrls() {
    TiEditClientPageViewModel result =
        mapper.map(mockTiGroup, false, "/action", Optional.empty(), Optional.empty(), mockRequest);

    assertThat(result.getBackToClientListUrl()).isNotEmpty();
    assertThat(result.getCancelUrl()).isNotEmpty();
    assertThat(result.getAccountSettingsUrl()).isNotEmpty();
  }

  @Test
  public void map_noApplicant_setsEmptyPersonalFields() {
    TiEditClientPageViewModel result =
        mapper.map(mockTiGroup, false, "/action", Optional.empty(), Optional.empty(), mockRequest);

    assertThat(result.getFirstName()).isEmpty();
    assertThat(result.getMiddleName()).isEmpty();
    assertThat(result.getLastName()).isEmpty();
    assertThat(result.getNameSuffix()).isEmpty();
    assertThat(result.getPhoneNumber()).isEmpty();
    assertThat(result.getEmailAddress()).isEmpty();
    assertThat(result.getDateOfBirth()).isEmpty();
  }

  @Test
  public void map_noAccount_setsEmptyTiNote() {
    TiEditClientPageViewModel result =
        mapper.map(mockTiGroup, false, "/action", Optional.empty(), Optional.empty(), mockRequest);

    assertThat(result.getTiNote()).isEmpty();
  }

  @Test
  public void map_withApplicant_setsPersonalFields() {
    ApplicantModel applicant = mock(ApplicantModel.class);
    when(applicant.getFirstName()).thenReturn(Optional.of("John"));
    when(applicant.getMiddleName()).thenReturn(Optional.of("M"));
    when(applicant.getLastName()).thenReturn(Optional.of("Doe"));
    when(applicant.getSuffix()).thenReturn(Optional.of("Jr."));
    when(applicant.getPhoneNumber()).thenReturn(Optional.of("5551234567"));
    when(applicant.getEmailAddress()).thenReturn(Optional.of("john@example.com"));
    LocalDate dob = LocalDate.of(1990, 6, 15);
    when(applicant.getDateOfBirth()).thenReturn(Optional.of(dob));
    when(mockDateConverter.formatIso8601Date(dob)).thenReturn("1990-06-15");

    TiEditClientPageViewModel result =
        mapper.map(
            mockTiGroup, true, "/action", Optional.of(applicant), Optional.empty(), mockRequest);

    assertThat(result.getFirstName()).isEqualTo("John");
    assertThat(result.getMiddleName()).isEqualTo("M");
    assertThat(result.getLastName()).isEqualTo("Doe");
    assertThat(result.getNameSuffix()).isEqualTo("Jr.");
    assertThat(result.getPhoneNumber()).isEqualTo("5551234567");
    assertThat(result.getEmailAddress()).isEqualTo("john@example.com");
    assertThat(result.getDateOfBirth()).isEqualTo("1990-06-15");
  }

  @Test
  public void map_withAccount_setsTiNote() {
    AccountModel account = mock(AccountModel.class);
    when(account.getTiNote()).thenReturn("Important note");

    TiEditClientPageViewModel result =
        mapper.map(
            mockTiGroup, false, "/action", Optional.empty(), Optional.of(account), mockRequest);

    assertThat(result.getTiNote()).isEqualTo("Important note");
  }

  @Test
  public void map_nameSuffixEnabled_setsFlag() {
    when(mockSettingsManifest.getNameSuffixDropdownEnabled(mockRequest)).thenReturn(true);

    TiEditClientPageViewModel result =
        mapper.map(mockTiGroup, false, "/action", Optional.empty(), Optional.empty(), mockRequest);

    assertThat(result.isNameSuffixEnabled()).isTrue();
  }

  @Test
  public void map_buildsSuffixOptions() {
    TiEditClientPageViewModel result =
        mapper.map(mockTiGroup, false, "/action", Optional.empty(), Optional.empty(), mockRequest);

    assertThat(result.getSuffixOptions()).isNotEmpty();
    assertThat(result.getSuffixOptions().get(0).getLabel()).isNotEmpty();
    assertThat(result.getSuffixOptions().get(0).getValue()).isNotEmpty();
  }
}
