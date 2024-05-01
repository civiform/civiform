package services.settings;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.regex.Pattern;

/** A server setting. Settings are defined in server/conf/env_var_docs.json */
@AutoValue
public abstract class SettingDescription {

  public static SettingDescription create(
      String variableName,
      String variableDescription,
      boolean isRequired,
      SettingType settingType,
      SettingMode settingMode) {
    return new AutoValue_SettingDescription(
        variableName,
        variableDescription,
        isRequired,
        settingType,
        settingMode,
        /* allowableValues= */ Optional.empty(),
        /* validationRegex= */ Optional.empty());
  }

  public static SettingDescription create(
      String variableName,
      String variableDescription,
      boolean isRequired,
      SettingType settingType,
      SettingMode settingMode,
      ImmutableList<String> allowableValues) {
    return new AutoValue_SettingDescription(
        variableName,
        variableDescription,
        isRequired,
        settingType,
        settingMode,
        Optional.of(allowableValues),
        /* validationRegex= */ Optional.empty());
  }

  public static SettingDescription create(
      String variableName,
      String variableDescription,
      boolean isRequired,
      SettingType settingType,
      SettingMode settingMode,
      Pattern validationRegex) {
    return new AutoValue_SettingDescription(
        variableName,
        variableDescription,
        isRequired,
        settingType,
        settingMode,
        /* allowableValues= */ Optional.empty(),
        /* validationRegex= */ Optional.of(validationRegex));
  }

  /** Variable name of the setting. Will always be in SCREAMING_SNAKE_CASE. */
  public abstract String variableName();

  /** A sentence or two describing the setting. */
  public abstract String settingDescription();

  // True if the setting must be present.
  public abstract boolean isRequired();

  /** The type of this setting. */
  public abstract SettingType settingType();

  /** The display mode of this setting. */
  public abstract SettingMode settingMode();

  // Present if variable is an ENUM. Defines the list of values this setting
  // may have.
  public abstract Optional<ImmutableList<String>> allowableValues();

  // Present if variable is a STRING and has a validation regex. If present
  // the value of this setting must match the regex.
  public abstract Optional<Pattern> validationRegex();

  public boolean isReadOnly() {
    return !settingMode().equals(SettingMode.ADMIN_WRITEABLE);
  }

  private static final ImmutableSet<SettingMode> DISPLAYABLE_SETTING_MODES =
      ImmutableSet.of(SettingMode.ADMIN_WRITEABLE, SettingMode.ADMIN_READABLE);

  /** True if the setting should be displayed in the UI. */
  public boolean shouldDisplay() {
    return DISPLAYABLE_SETTING_MODES.contains(settingMode());
  }
}
