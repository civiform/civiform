package services;

import com.google.common.collect.ImmutableMap;
import net.jcip.annotations.Immutable;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalizedStringsTest {

  @Test
  public void equalsWorks() {
    LocalizedStrings first = LocalizedStrings.of(Locale.US, "hello", Locale.FRENCH, "bonjour");
    LocalizedStrings second = LocalizedStrings.of(Locale.US, "hello", Locale.FRENCH, "bonjour");

    assertThat(first).isEqualTo(second);
  }

  @Test
  public void toBuilder() {
    LocalizedStrings original = LocalizedStrings.of(Locale.US, "hello");

    LocalizedStrings subject = original.toBuilder().setIsRequired(false).build();

    assertThat(subject.translations()).containsExactlyEntriesOf(original.translations());
    assertThat(subject.isRequired()).isFalse();
  }

  @Test
  public void builder_isRequiredByDefault() {
    LocalizedStrings subject = LocalizedStrings.builder().build();

    assertThat(subject.isRequired()).isTrue();
    assertThat(subject.isEmpty()).isTrue();
  }

  @Test
  public void builder_clearTranslations() {
    LocalizedStrings subject = LocalizedStrings.builder().put(Locale.US, "first").clearTranslations().put(Locale.FRENCH, "one").build();

    assertThat(subject.translations()).containsExactlyEntriesOf(ImmutableMap.of(Locale.FRENCH, "one"));
  }
}
