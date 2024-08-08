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
  public void value_pairOfDates() {
    LocalDate date1 = LocalDate.of(2024, 5, 1);
    LocalDate date2 = LocalDate.of(2024, 5, 2);
    PredicateValue value = PredicateValue.pairOfDates(date1, date2);

    assertThat(value.value()).isEqualTo("[1714521600000, 1714608000000]");
  }

  @Test
  public void value_pairOfLongs() {
    PredicateValue value = PredicateValue.pairOfLongs(18, 30);

    assertThat(value.value()).isEqualTo("[18, 30]");
  }

  @Test
  public void toDisplayString_currency() {
    QuestionDefinition currencyDef =
        testQuestionBank.currencyApplicantMonthlyIncome().getQuestionDefinition();
    PredicateValue value = PredicateValue.of(10001);

    assertThat(value.value()).isEqualTo("10001");
    assertThat(value.toDisplayString(currencyDef)).isEqualTo("$100.01");
  }

  @Test
  public void toDisplayString_currencyPair() {
    QuestionDefinition currencyDef =
        testQuestionBank.currencyApplicantMonthlyIncome().getQuestionDefinition();
    PredicateValue value = PredicateValue.pairOfLongs(10001, 20002);

    assertThat(value.value()).isEqualTo("[10001, 20002]");
    assertThat(value.toDisplayString(currencyDef)).isEqualTo("$100.01 and $200.02");
  }

  @Test
  public void toDisplayString_date() {
    QuestionDefinition dateDef = testQuestionBank.dateApplicantBirthdate().getQuestionDefinition();
    PredicateValue value = PredicateValue.of(LocalDate.ofYearDay(2021, 1));

    assertThat(value.value()).isEqualTo("1609459200000");
    assertThat(value.toDisplayString(dateDef)).isEqualTo("2021-01-01");
  }

  @Test
  public void toDisplayString_listOfLongs() {
    QuestionDefinition longDef =
        testQuestionBank.numberApplicantJugglingNumber().getQuestionDefinition();
    PredicateValue value = PredicateValue.listOfLongs(ImmutableList.of(1L, 2L, 3L));

    assertThat(value.value()).isEqualTo("[1, 2, 3]");
    assertThat(value.toDisplayString(longDef)).isEqualTo("[1, 2, 3]");
  }

  @Test
  public void toDisplayString_simpleList() {
    QuestionDefinition stringDef =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();
    PredicateValue value = PredicateValue.listOfStrings(ImmutableList.of("kangaroo", "turtle"));

    assertThat(value.toDisplayString(stringDef)).isEqualTo("[\"kangaroo\", \"turtle\"]");
  }

  @Test
  public void toDisplayString_multiOptionList() {
    QuestionDefinition multiOption =
        testQuestionBank.dropdownApplicantIceCream().getQuestionDefinition();

    PredicateValue value = PredicateValue.listOfStrings(ImmutableList.of("1", "2"));

    assertThat(value.toDisplayString(multiOption)).isEqualTo("[chocolate, strawberry]");
  }

  @Test
  public void toDisplayString_multiOptionSingleValue_convertsIdToString() {
    QuestionDefinition multiOption =
        testQuestionBank.checkboxApplicantKitchenTools().getQuestionDefinition();

    PredicateValue value = PredicateValue.of("1");

    assertThat(value.toDisplayString(multiOption)).isEqualTo("toaster");
  }

  @Test
  public void toDisplayString_multiOptionList_missingIdDefaultsToObsolete() {
    QuestionDefinition multiOption =
        testQuestionBank.dropdownApplicantIceCream().getQuestionDefinition();

    PredicateValue value =
        PredicateValue.listOfStrings(ImmutableList.of("1", "100")); // 100 is not a valid ID

    assertThat(value.toDisplayString(multiOption)).isEqualTo("[chocolate, <obsolete>]");
  }

  @Test
  public void toDisplayString_pairOfDates() {
    QuestionDefinition dateDef = testQuestionBank.dateApplicantBirthdate().getQuestionDefinition();

    LocalDate date1 = LocalDate.of(2024, 5, 1);
    LocalDate date2 = LocalDate.of(2024, 5, 2);
    PredicateValue value = PredicateValue.pairOfDates(date1, date2);

    assertThat(value.toDisplayString(dateDef)).isEqualTo("2024-05-01 and 2024-05-02");
  }

  @Test
  public void toDisplayString_pairOfLongs() {
    QuestionDefinition dateDef = testQuestionBank.dateApplicantBirthdate().getQuestionDefinition();

    PredicateValue value = PredicateValue.pairOfLongs(18, 30);

    assertThat(value.toDisplayString(dateDef)).isEqualTo("18 and 30");
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
      PredicateValue.pairOfDates(LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 100)),
      PredicateValue.pairOfLongs(18, 30),
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
