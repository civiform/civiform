package services.settings;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.regex.Pattern;

/** A server setting. Settings are defined in server/conf/env_var_docs.json */
@AutoValue
public abstract class SettingDescription {

  public static SettingDescription create(
      String variableName, String variableDescription, SettingType settingType) {
    return new AutoValue_SettingDescription(
        variableName,
        variableDescription,
        settingType,
        /* allowableValues= */ Optional.empty(),
        /* validationRegex= */ Optional.empty());
  }

  public static SettingDescription create(
      String variableName,
      String variableDescription,
      SettingType settingType,
      ImmutableList<String> allowableValues) {
    return new AutoValue_SettingDescription(
        variableName,
        variableDescription,
        settingType,
        Optional.of(allowableValues),
        /* validationRegex= */ Optional.empty());
  }

  public static SettingDescription create(
      String variableName,
      String variableDescription,
      SettingType settingType,
      Pattern validationRegex) {
    return new AutoValue_SettingDescription(
        variableName,
        variableDescription,
        settingType,
        /* allowableValues= */ Optional.empty(),
        /* validationRegex= */ Optional.of(validationRegex));
  }

  /** Variable name of the setting. Will always be in SCREAMING_SNAKE_CASE. */
  public abstract String variableName();

  /** A sentence or two describing the setting. */
  public abstract String settingDescription();

  /** The type of this setting. */
  public abstract SettingType settingType();

  // Present if variable is an ENUM. Defines the list of values this setting
  // may have.
  public abstract Optional<ImmutableList<String>> allowableValues();

  // Present if variable is a STRING and has a validation regex. If present
  // the value of this setting must match the regex.
  public abstract Optional<Pattern> validationRegex();
}
