package services;

import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import com.google.common.collect.ImmutableSet;

public class ErrorAndTest {
  @Test
  public void canBeCreatedWithResultAndErrors() {
    ErrorAnd<String, String> errorAndResult =
        new ErrorAnd<>(ImmutableSet.of("error 1", "error 2"), "result");

    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult()).isEqualTo("result");
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors()).containsAll(ImmutableSet.of("error 1", "error 2"));
  }

  @Test
  public void canBeCreatedWithOnlyErrors() {
    ErrorAnd<String, String> errorAndResult = new ErrorAnd<>(ImmutableSet.of("error 1", "error 2"));

    Throwable thrown = catchThrowable(() -> errorAndResult.getResult());

    assertThat(thrown).isInstanceOf(RuntimeException.class);
    assertThat(thrown).hasMessage("There is no result");
    assertThat(errorAndResult.hasResult()).isFalse();
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
