package services;

import org.junit.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalizedStringsTest {

  @Test
  public void equalsWorks() {
    LocalizedStrings first = LocalizedStrings.of(Locale.US, "hello", Locale.FRENCH, "bonjour");
    LocalizedStrings second = LocalizedStrings.of(Locale.US, "hello", Locale.FRENCH, "bonjour");

    assertThat(first).isEqualTo(second);
  }
}
