package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
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
  public void value_string() {
    PredicateValue value = PredicateValue.of("hello");
    assertThat(value.value()).isEqualTo("\"hello\"");
  }

  @Test
  public void value_stringWithQuotes_stripsQuotesBeforeEncoding() {
    PredicateValue value = PredicateValue.of("\"\"h\"el\"\"lo\"\"\"\"\"");
    assertThat(value.value()).isEqualTo("\"hello\"");
  }

  @Test
  public void value_listOfStrings() {
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
  public void valueWithoutSurroundingQuotes_string() {
    PredicateValue value = PredicateValue.of("hello");
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo("hello");
  }

  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private PredicateValue[] valueWithoutSurroundingQuotes_doesNotAffectNonStrings_parameters() {
    return new PredicateValue[] {
      // All possible PredicateValue types besides string
      PredicateValue.of(10001),
      PredicateValue.of(1.04),
      PredicateValue.of(LocalDate.ofYearDay(2021, 1)),
      PredicateValue.listOfStrings(ImmutableList.of("hello", "world")),
      PredicateValue.listOfLongs(ImmutableList.of(1L, 2L, 3L)),
      PredicateValue.serviceArea("seattle"),
    };
  }

  @Test
  @Parameters(method = "valueWithoutSurroundingQuotes_doesNotAffectNonStrings_parameters")
  public void valueWithoutSurroundingQuotes_doesNotAffectNonStrings(PredicateValue value) {
    // valueWithoutSurrroundingQuotes should only affect plain strings
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo(value.value());
  }
}
