package mapping.admin.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import services.settings.SettingDescription;
import services.settings.SettingMode;
import services.settings.SettingType;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import views.CiviFormMarkdown;
import views.admin.settings.AdminSettingsPageViewModel;

public final class AdminSettingsPageMapperTest {

  private AdminSettingsPageMapper mapper;
  private SettingsManifest mockSettingsManifest;
  private CiviFormMarkdown mockMarkdown;
  private Http.Request mockRequest;

  @Before
  public void setup() {
    mapper = new AdminSettingsPageMapper();
    mockSettingsManifest = mock(SettingsManifest.class);
    mockMarkdown = mock(CiviFormMarkdown.class);
    mockRequest = mock(Http.Request.class);
    when(mockMarkdown.render(any())).thenReturn("<p>description</p>");
  }

  @Test
  public void map_setsFormActionUrl() {
    when(mockSettingsManifest.getSections()).thenReturn(ImmutableMap.of());

    AdminSettingsPageViewModel result =
        mapper.map(mockRequest, mockSettingsManifest, mockMarkdown, Optional.empty());

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_withNoSections_returnsEmptySections() {
    when(mockSettingsManifest.getSections()).thenReturn(ImmutableMap.of());

    AdminSettingsPageViewModel result =
        mapper.map(mockRequest, mockSettingsManifest, mockMarkdown, Optional.empty());

    assertThat(result.getSections()).isEmpty();
  }

  @Test
  public void map_withBrandingSection_includesBrandingSection() {
    SettingDescription settingDesc =
        SettingDescription.create(
            "TEST_SETTING",
            "A test setting",
            false,
            SettingType.STRING,
            SettingMode.ADMIN_WRITEABLE);
    SettingsSection brandingSection =
        SettingsSection.create(
            "Branding", "Branding settings", ImmutableList.of(), ImmutableList.of(settingDesc));

    when(mockSettingsManifest.getSections())
        .thenReturn(ImmutableMap.of("Branding", brandingSection));
    when(mockSettingsManifest.getSettingDisplayValue(any(), any()))
        .thenReturn(Optional.of("test value"));

    AdminSettingsPageViewModel result =
        mapper.map(mockRequest, mockSettingsManifest, mockMarkdown, Optional.empty());

    assertThat(result.getSections()).hasSize(1);
    assertThat(result.getSections().get(0).name()).isEqualTo("Branding");
  }

  @Test
  public void map_withErrorMessages_setsErrorMessages() {
    when(mockSettingsManifest.getSections()).thenReturn(ImmutableMap.of());

    ImmutableMap<String, services.settings.SettingsService.SettingsGroupUpdateResult.UpdateError>
        errors = ImmutableMap.of();
    AdminSettingsPageViewModel result =
        mapper.map(mockRequest, mockSettingsManifest, mockMarkdown, Optional.of(errors));

    assertThat(result.getErrorMessages()).isPresent();
  }

  @Test
  public void map_withNoErrorMessages_emptyErrorMessages() {
    when(mockSettingsManifest.getSections()).thenReturn(ImmutableMap.of());

    AdminSettingsPageViewModel result =
        mapper.map(mockRequest, mockSettingsManifest, mockMarkdown, Optional.empty());

    assertThat(result.getErrorMessages()).isEmpty();
  }
}
