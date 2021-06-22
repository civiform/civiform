package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.Test;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class PredicateValueTest {

  private final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  public void oldValue_parsesCorrectly() throws Exception {
    String valueWithoutType = "{\"value\":\"\\\"hello\\\"\"}";
    ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());

    assertThat(mapper.readValue(valueWithoutType, PredicateValue.class))
        .isEqualTo(PredicateValue.of("hello"));
  }

  @Test
  public void stringValue_escapedProperly() {
    PredicateValue value = PredicateValue.of("hello");
    assertThat(value.value()).isEqualTo("\"hello\"");
  }

  @Test
  public void listOfStringsValue_escapedProperly() {
    PredicateValue value = PredicateValue.of(ImmutableList.of("hello", "world"));
    assertThat(value.value()).isEqualTo("[\"hello\", \"world\"]");
  }

  @Test
  public void toDisplayString_date() {
    PredicateValue value = PredicateValue.of(LocalDate.ofYearDay(2021, 1));

    assertThat(value.value()).isEqualTo("1609459200000");
    assertThat(value.toDisplayString(Optional.empty())).isEqualTo("2021-01-01");
  }

  @Test
  public void toDisplayString_simpleList() {
    PredicateValue value = PredicateValue.of(ImmutableList.of("kangaroo", "turtle"));

    assertThat(value.toDisplayString(Optional.empty())).isEqualTo("[\"kangaroo\", \"turtle\"]");
  }

  @Test
  public void toDisplayString_multiOptionList() {
    QuestionDefinition multiOption = testQuestionBank.applicantIceCream().getQuestionDefinition();

    PredicateValue value = PredicateValue.of(ImmutableList.of("1", "2"));

    assertThat(value.toDisplayString(Optional.of(multiOption)))
        .isEqualTo("[chocolate, strawberry]");
  }

  @Test
  public void toDisplayString_multiOptionList_missingIdDefaultsToObsolete() {
    QuestionDefinition multiOption = testQuestionBank.applicantIceCream().getQuestionDefinition();

    PredicateValue value = PredicateValue.of(ImmutableList.of("1", "100")); // 100 is not a valid ID

    assertThat(value.toDisplayString(Optional.of(multiOption)))
        .isEqualTo("[chocolate, <obsolete>]");
  }
}
