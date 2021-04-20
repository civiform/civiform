package services;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;

public class LocalizationUtils {

  /**
   * By design, {@link ImmutableMap}s do not have a {@code remove} method in their builders. If we
   * want to update an existing translation, we must copy the map except the existing entry for that
   * locale, and then create a new map with the new entry.
   */
  public static ImmutableMap<Locale, String> overwriteExistingTranslation(
      ImmutableMap<Locale, String> existing, Locale locale, String value) {
    ImmutableMap.Builder<Locale, String> builder = ImmutableMap.builder();
    for (Map.Entry<Locale, String> entry : existing.entrySet()) {
      if (!entry.getKey().equals(locale)) {
        builder.put(entry.getKey(), entry.getValue());
      }
    }
    builder.put(locale, value);
    return builder.build();
  }
}
