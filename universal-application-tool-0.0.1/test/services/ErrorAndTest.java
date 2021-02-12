package services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

public class ErrorAndTest {
  @Test
  public void canBeCreatedWithResultAndErrors() {
    ErrorAnd<String, ImmutableSet<String>> errorAndResult =
        new ErrorAnd<String, ImmutableSet<String>>("result", ImmutableSet.of("error 1", "error 2"));

    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult()).isEqualTo("result");
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors()).containsAll(ImmutableSet.of("error 1", "error 2"));
  }

  @Test
  public void canBeCreatedWithOnlyErrors() {
    ErrorAnd<String, ImmutableSet<String>> errorAndResult =
        new ErrorAnd<String, ImmutableSet<String>>(ImmutableSet.of("error 1", "error 2"));

    assertThat(errorAndResult.hasResult()).isFalse();
    // TODO: expect errorAndResult.getResult() to throw
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors()).containsAll(ImmutableSet.of("error 1", "error 2"));
  }

  @Test
  public void canBeCreatedWithOnlyResult() {
    ErrorAnd<String, ImmutableSet<String>> errorAndResult = new ErrorAnd<>("result");

    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult()).isEqualTo("result");
    assertThat(errorAndResult.isError()).isFalse();
    assertThat(errorAndResult.getErrors()).isEmpty();
  }
}
