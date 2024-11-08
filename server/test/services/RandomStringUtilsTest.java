package services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.Test;

public class RandomStringUtilsTest {
  @Test
  public void randomAlphabetic_pass() {
    String actual = RandomStringUtils.randomAlphabetic(50);
    assertThat(actual.length()).isEqualTo(50);
    assertThat(actual.matches("[a-zA-Z]+")).isTrue();
  }

  @Test
  public void randomAlphabetic_failsArgumentCheck() {
    assertThatThrownBy(() -> RandomStringUtils.randomAlphabetic(-1));
  }

  @Test
  public void randomAlphanumeric_pass() {
    String actual = RandomStringUtils.randomAlphanumeric(50);
    assertThat(actual.length()).isEqualTo(50);
    assertThat(actual.matches("[a-zA-Z0-9]+")).isTrue();
  }

  @Test
  public void randomAlphanumeric_failsArgumentCheck() {
    assertThatThrownBy(() -> RandomStringUtils.randomAlphanumeric(-1));
  }
}
