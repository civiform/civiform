package services.settings;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.regex.Pattern;

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

  public abstract String variableName();

  public abstract String variableDescription();

  public abstract SettingType settingType();

  // Present if variable is an ENUM
  public abstract Optional<ImmutableList<String>> allowableValues();

  // Present if variable is a STRING and has a validation regex
  public abstract Optional<Pattern> validationRegex();
}
