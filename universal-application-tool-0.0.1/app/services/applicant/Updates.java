package services.applicant;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.ImmutableSetMultimap.toImmutableSetMultimap;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.AbstractMap;
import java.util.Map;
import services.Path;
import services.question.MultiOptionQuestionDefinition;

@AutoValue
public abstract class Updates {

  public static Updates create(ImmutableMap<String, String> rawUpdates) {
    return new AutoValue_Updates(generateUpdates(rawUpdates));
  }

  public abstract ImmutableSet<Update> updates();

  private static ImmutableSet<Update> generateUpdates(ImmutableMap<String, String> rawUpdates) {
    ImmutableSetMultimap<Path, String> updates =
        rawUpdates.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(Path.create(e.getKey()), e.getValue()))
            .map(Updates::convertIfCheckboxField)
            .collect(toImmutableSetMultimap(Map.Entry::getKey, Map.Entry::getValue));

    System.out.println(updates);

    return updates.asMap().entrySet().stream()
        .map(
            e -> {
              String valueAsString =
                  e.getValue().size() == 1
                      ? e.getValue().iterator().next()
                      : e.getValue().toString();
              return Update.create(e.getKey(), valueAsString);
            })
        .collect(toImmutableSet());
  }

  /**
   * If this is a checkbox field, the form data will be passed as {@code
   * path.selection.option_label: on}. Instead, we want {@code path.selection: option_label}. We
   * convert them here.
   */
  private static Map.Entry<Path, String> convertIfCheckboxField(Map.Entry<Path, String> entry) {
    if (entry.getKey().parentPath().keyName().equals(MultiOptionQuestionDefinition.SCALAR_NAME)) {
      return new AbstractMap.SimpleImmutableEntry<>(
          entry.getKey().parentPath(), entry.getKey().keyName());
    }
    return entry;
  }
}
