package forms;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import services.LocalizedStrings;
import services.question.QuestionSetting;

/**
 * Utility methods for working with QuestionSetting objects in question forms.
 */
public final class QuestionSettingUtils {
  
  /**
   * Creates an empty QuestionSetting with empty key and display name.
   *
   * @return empty QuestionSetting
   */
  public static QuestionSetting emptySetting() {
    return QuestionSetting.create("", LocalizedStrings.withDefaultValue(""));
  }
  
  /**
   * Creates a QuestionSetting with empty key but specified display name.
   *
   * @param displayName the display name for the setting
   * @return QuestionSetting with empty key and the given display name
   */
  public static QuestionSetting emptySettingWithDisplayName(String displayName) {
    return QuestionSetting.create("", LocalizedStrings.withDefaultValue(displayName));
  }
  
  
  /**
   * Validates that a QuestionSetting has non-empty key and display name.
   *
   * @param setting the QuestionSetting to validate
   * @return true if both key and display name are non-null and non-empty
   */
  public static boolean isValidSetting(QuestionSetting setting) {
    return setting.settingKey() != null
        && !setting.settingKey().isEmpty()
        && setting.settingDisplayName().getDefault() != null
        && !setting.settingDisplayName().getDefault().isEmpty();
  }
  
  /**
   * Finds a QuestionSetting by its display name from a set of settings.
   *
   * @param settings the set of settings to search
   * @param displayName the display name to match
   * @return the matching QuestionSetting, or empty setting if not found
   */
  public static QuestionSetting findByDisplayName(Set<QuestionSetting> settings, String displayName) {
    return settings.stream()
        .filter(setting -> setting.settingDisplayName().getDefault().equals(displayName))
        .findFirst()
        .orElse(emptySetting());
  }
}
