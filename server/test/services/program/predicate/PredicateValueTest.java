package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import org.junit.Test;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class PredicateValueTest {

  private final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  public void oldValue_withoutTypeField_parsesCorrectly() throws Exception {
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
    PredicateValue value = PredicateValue.listOfStrings(ImmutableList.of("hello", "world"));
    assertThat(value.value()).isEqualTo("[\"hello\", \"world\"]");
  }

  @Test
  public void toDisplayString_currency() {
    QuestionDefinition currencyDef =
        testQuestionBank.applicantMonthlyIncome().getQuestionDefinition();
    PredicateValue value = PredicateValue.of(10001);

    assertThat(value.value()).isEqualTo("10001");
    assertThat(value.toDisplayString(currencyDef)).isEqualTo("$100.01");
  }

  @Test
  public void toDisplayString_date() {
    QuestionDefinition dateDef = testQuestionBank.applicantDate().getQuestionDefinition();
    PredicateValue value = PredicateValue.of(LocalDate.ofYearDay(2021, 1));

    assertThat(value.value()).isEqualTo("1609459200000");
    assertThat(value.toDisplayString(dateDef)).isEqualTo("2021-01-01");
  }

  @Test
  public void toDisplayString_listOfLongs() {
    QuestionDefinition longDef = testQuestionBank.applicantJugglingNumber().getQuestionDefinition();
    PredicateValue value = PredicateValue.listOfLongs(ImmutableList.of(1L, 2L, 3L));

    assertThat(value.value()).isEqualTo("[1, 2, 3]");
    assertThat(value.toDisplayString(longDef)).isEqualTo("[1, 2, 3]");
  }

  @Test
  public void toDisplayString_simpleList() {
    QuestionDefinition stringDef =
        testQuestionBank.applicantFavoriteColor().getQuestionDefinition();
    PredicateValue value = PredicateValue.listOfStrings(ImmutableList.of("kangaroo", "turtle"));

    assertThat(value.toDisplayString(stringDef)).isEqualTo("[\"kangaroo\", \"turtle\"]");
  }

  @Test
  public void toDisplayString_multiOptionList() {
    QuestionDefinition multiOption = testQuestionBank.applicantIceCream().getQuestionDefinition();

    PredicateValue value = PredicateValue.listOfStrings(ImmutableList.of("1", "2"));

    assertThat(value.toDisplayString(multiOption)).isEqualTo("[chocolate, strawberry]");
  }

  @Test
  public void toDisplayString_multiOptionSingleValue_convertsIdToString() {
    QuestionDefinition multiOption =
        testQuestionBank.applicantKitchenTools().getQuestionDefinition();

    PredicateValue value = PredicateValue.of("1");

    assertThat(value.toDisplayString(multiOption)).isEqualTo("toaster");
  }

  @Test
  public void toDisplayString_multiOptionList_missingIdDefaultsToObsolete() {
    QuestionDefinition multiOption = testQuestionBank.applicantIceCream().getQuestionDefinition();

    PredicateValue value =
        PredicateValue.listOfStrings(ImmutableList.of("1", "100")); // 100 is not a valid ID

    assertThat(value.toDisplayString(multiOption)).isEqualTo("[chocolate, <obsolete>]");
  }

  @Test
  public void valueWithoutSurroundingQuotes_parsesCorrectly() {
    PredicateValue value = PredicateValue.of("hello");
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo("hello");

    // Should only strip plain strings. Everything else should remain the same.
    value = PredicateValue.listOfStrings(ImmutableList.of("hello", "world"));
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo("[\"hello\", \"world\"]");

    value = PredicateValue.of(10001);
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo("10001");

    value = PredicateValue.of(LocalDate.ofYearDay(2021, 1));
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo("1609459200000");

    value = PredicateValue.listOfLongs(ImmutableList.of(1L, 2L, 3L));
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo("[1, 2, 3]");
  }

  @Test
  public void surroundWithQuotes_stripsQuotesThenAppendsCorrectly() {
    PredicateValue value = PredicateValue.of("\"\"h\"el\"\"lo\"\"\"\"\"");
    assertThat(value.value()).isEqualTo("\"hello\"");
  }
}
