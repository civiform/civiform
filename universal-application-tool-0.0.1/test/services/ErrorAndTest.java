package services;

import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorAndTest {
  @Test
  public void canBeCreatedWithResultAndErrors() {
    ErrorAnd<String, ImmutableSet<String>> errorAndResult =
            new ErrorAnd<String, ImmutableSet<String>>("result", ImmutableSet.of("hello", "world"));

    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult()).isEqualTo("result");
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors()).containsAll(ImmutableSet.of("error 1", "error 2"));
  }

  @Test
  public void canBeCreatedWithOnlyErrors() {
    ErrorAnd<String, ImmutableSet<String>> errorAndResult =
            new ErrorAnd<String, ImmutableSet<String>>(ImmutableSet.of("hello", "world"));

    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors()).containsAll(ImmutableSet.of("error 1", "error 2"));
  }

  @Test
  public void canBeCreatedWithOnlyResult() {
    ErrorAnd<String, ImmutableSet<String>> errorAndResult =
            new ErrorAnd<>("result");

    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult()).isEqualTo("result");
    assertThat(errorAndResult.isError()).isFalse();
    assertThat(errorAndResult.getErrors()).isEmpty();
  }
}
