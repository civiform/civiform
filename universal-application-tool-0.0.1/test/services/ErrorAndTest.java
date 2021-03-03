package services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class ErrorAndTest {
  @Test
  public void canBeCreatedWithResultAndErrors() {
    ErrorAnd<String, String> errorAndResult =
        ErrorAnd.errorAnd(ImmutableSet.of("error 1", "error 2"), "result");

    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult()).isEqualTo("result");
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors()).containsAll(ImmutableSet.of("error 1", "error 2"));
  }

  @Test
  public void canBeCreatedWithOnlyErrors() {
    ErrorAnd<String, String> errorAndResult = ErrorAnd.error(ImmutableSet.of("error 1", "error 2"));
    Throwable thrown = catchThrowable(() -> errorAndResult.getResult());

    assertThat(thrown).isInstanceOf(RuntimeException.class);
    assertThat(thrown).hasMessage("There is no result");
    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors()).containsAll(ImmutableSet.of("error 1", "error 2"));
  }

  @Test
  public void canBeCreatedWithOnlyResult() {
    ErrorAnd<String, String> errorAndResult = ErrorAnd.of("result");

    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult()).isEqualTo("result");
    assertThat(errorAndResult.isError()).isFalse();
    assertThat(errorAndResult.getErrors()).isEmpty();
  }
}
