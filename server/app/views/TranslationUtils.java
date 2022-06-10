package views;

import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.UUID;
import play.i18n.Messages;
import services.MessageKey;

/** Utility class for managing translated content. */
public final class TranslationUtils {
  public static TranslatedStringSplitResult splitTranslatedSingleString(
      Messages messages, MessageKey messageKey) {
    return splitTranslatedSingleString(messages, messageKey.getKeyName());
  }

  static TranslatedStringSplitResult splitTranslatedSingleString(Messages messages, String key) {
    String placeholder = UUID.randomUUID().toString();
    String translatedWithPlaceholder = messages.at(key, placeholder);
    // Now split into components.
    List<String> components =
        Splitter.onPattern(placeholder).splitToList(translatedWithPlaceholder);
    if (components.size() != 2) {
      throw new RuntimeException(
          String.format(
              "Expected exactly one occurrence of %s in translated string: %s",
              placeholder, translatedWithPlaceholder));
    }
    return TranslatedStringSplitResult.builder()
        .setBeforeInterpretedContent(components.get(0))
        .setAfterInterpretedContent(components.get(1))
        .build();
  }

  @AutoValue
  public abstract static class TranslatedStringSplitResult {
    public abstract String beforeInterpretedContent();

    public abstract String afterInterpretedContent();

    static Builder builder() {
      return new AutoValue_TranslationUtils_TranslatedStringSplitResult.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setBeforeInterpretedContent(String value);

      abstract Builder setAfterInterpretedContent(String value);

      abstract TranslatedStringSplitResult build();
    }
  }
}
