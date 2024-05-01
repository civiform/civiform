package services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;

public class LocalizedStringsTest {

  @Test
  public void equalsWorks() {
    LocalizedStrings first = LocalizedStrings.of(Locale.US, "hello", Locale.FRENCH, "bonjour");
    LocalizedStrings second = LocalizedStrings.of(Locale.FRENCH, "bonjour", Locale.US, "hello");

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
  public void isRequiredByDefault() {
    assertThat(LocalizedStrings.create(ImmutableMap.of()).isRequired()).isTrue();
    assertThat(LocalizedStrings.builder().build().isRequired()).isTrue();
  }

  @Test
  public void empty_isNotRequired() {
    assertThat(LocalizedStrings.empty().isRequired()).isFalse();
  }

  @Test
  public void withEmptyDefault_hasDefault() throws TranslationNotFoundException {
    assertThat(LocalizedStrings.withEmptyDefault().get(LocalizedStrings.DEFAULT_LOCALE))
        .isEqualTo("");
  }

  @Test
  public void withEmptyDefault_isRequired() {
    assertThat(LocalizedStrings.withEmptyDefault().isRequired()).isTrue();
  }

  @Test
  public void of_isRequired() {
    assertThat(LocalizedStrings.of().isRequired()).isTrue();
  }

  @Test
  public void updateDefaultTranslation_addsANewOne() {
    LocalizedStrings strings = LocalizedStrings.of();

    LocalizedStrings subject = strings.updateDefaultTranslation("hello");

    assertThat(subject.translations())
        .containsExactlyEntriesOf(ImmutableMap.of(LocalizedStrings.DEFAULT_LOCALE, "hello"));
  }

  @Test
  public void updateDefaultTranslation_updatesExistingOne() {
    LocalizedStrings strings = LocalizedStrings.of(LocalizedStrings.DEFAULT_LOCALE, "old");

    LocalizedStrings subject = strings.updateDefaultTranslation("new");

    assertThat(subject.translations())
        .containsExactlyEntriesOf(ImmutableMap.of(LocalizedStrings.DEFAULT_LOCALE, "new"));
  }

  @Test
  public void updateTranslation_addsANewOne() {
    LocalizedStrings strings = LocalizedStrings.of();

    LocalizedStrings subject = strings.updateTranslation(Locale.US, "hello");

    assertThat(subject.translations())
        .containsExactlyEntriesOf(ImmutableMap.of(Locale.US, "hello"));
  }

  @Test
  public void updateTranslation_updatesExistingOne() {
    LocalizedStrings strings = LocalizedStrings.of(Locale.US, "old");

    LocalizedStrings subject = strings.updateTranslation(Locale.US, "new");

    assertThat(subject.translations()).containsExactlyEntriesOf(ImmutableMap.of(Locale.US, "new"));
  }

  @Test
  public void updateTranslation_setsToEmptyString() {
    LocalizedStrings strings = LocalizedStrings.of(Locale.US, "old");

    LocalizedStrings subject = strings.updateTranslation(Locale.US, "");

    assertThat(subject.translations()).containsExactlyEntriesOf(ImmutableMap.of(Locale.US, ""));
  }

  @Test
  public void updateTranslation_emptyOptionalClearsExistingTranslation() {
    LocalizedStrings strings =
        LocalizedStrings.of(Locale.US, "old", Locale.FRENCH, "old french text");

    LocalizedStrings subject = strings.updateTranslation(Locale.FRENCH, Optional.empty());

    assertThat(subject.translations()).containsExactlyEntriesOf(ImmutableMap.of(Locale.US, "old"));
  }

  @Test
  public void updateTranslation_emptyOptionalNoOpForMissingTranslation() {
    LocalizedStrings strings = LocalizedStrings.of(Locale.US, "old");

    LocalizedStrings subject = strings.updateTranslation(Locale.FRENCH, Optional.empty());

    assertThat(subject.translations()).containsExactlyEntriesOf(ImmutableMap.of(Locale.US, "old"));
  }

  @Test
  public void cannotAddSameLocale() {
    LocalizedStrings strings = LocalizedStrings.of(Locale.US, "existing");

    assertThatThrownBy(() -> strings.toBuilder().put(Locale.US, "new").build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Multiple entries with same key");

    assertThatThrownBy(() -> LocalizedStrings.of(Locale.US, "first", Locale.US, "second"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Multiple entries with same key");
  }
}
