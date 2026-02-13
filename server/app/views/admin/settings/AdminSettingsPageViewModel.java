package views.admin.settings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import services.settings.SettingsService.SettingsGroupUpdateResult.UpdateError;
import views.admin.BaseViewModel;

@Data
@Builder
public final class AdminSettingsPageViewModel implements BaseViewModel {

  private final ImmutableList<SettingsSectionData> sections;
  private final Optional<ImmutableMap<String, UpdateError>> errorMessages;

  public String getFormActionUrl() {
    return routes.AdminSettingsController.update().url();
  }

  /** Represents a section of settings for display. */
  public record SettingsSectionData(
      String name,
      String slug,
      ImmutableList<SettingData> settings,
      ImmutableList<SettingsSectionData> subsections) {}

  /** Represents a single setting for display. */
  public record SettingData(
      String variableName,
      String descriptionHtml,
      String settingType,
      String value,
      boolean readOnly,
      Optional<ImmutableList<String>> allowableValues,
      Optional<String> errorMessage,
      Optional<String> errorValue) {

    public boolean isBoolean() {
      return "BOOLEAN".equals(settingType);
    }

    public boolean isString() {
      return "STRING".equals(settingType) || "LIST_OF_STRINGS".equals(settingType);
    }

    public boolean isInt() {
      return "INT".equals(settingType);
    }

    public boolean isEnum() {
      return "ENUM".equals(settingType);
    }

    public boolean isTrue() {
      return "TRUE".equals(value);
    }

    /** Returns the value to display, preferring the error value if present. */
    public String displayValue() {
      return errorValue.orElse(value);
    }
  }
}
