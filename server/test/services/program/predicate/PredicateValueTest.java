package services.program.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.test.WithApplication;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class PredicateValueTest extends WithApplication {

  private final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  public void oldValue_withoutTypeField_parsesCorrectly() throws Exception {
    String valueWithoutType = "{\"value\":\"\\\"hello\\\"\"}";
    ObjectMapper mapper = instanceOf(ObjectMapper.class);

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
  @Parameters({"false", "true"})
  public void toDisplayString_currency(boolean useFormatted) {
    QuestionDefinition currencyDef =
        testQuestionBank.currencyApplicantMonthlyIncome().getQuestionDefinition();
    PredicateValue value = PredicateValue.of(10001);

    assertThat(value.value()).isEqualTo("10001");
    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(currencyDef).render())
          .isEqualTo("<strong>$100.01</strong>");
    } else {
      assertThat(value.toDisplayString(currencyDef)).isEqualTo("$100.01");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_currencyPair(boolean useFormatted) {
    QuestionDefinition currencyDef =
        testQuestionBank.currencyApplicantMonthlyIncome().getQuestionDefinition();
    PredicateValue value = PredicateValue.pairOfLongs(10001, 20002);

    assertThat(value.value()).isEqualTo("[10001, 20002]");
    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(currencyDef).render())
          .isEqualTo("<strong>$100.01</strong> and <strong>$200.02</strong>");
    } else {
      assertThat(value.toDisplayString(currencyDef)).isEqualTo("$100.01 and $200.02");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_date(boolean useFormatted) {
    QuestionDefinition dateDef = testQuestionBank.dateApplicantBirthdate().getQuestionDefinition();
    PredicateValue value = PredicateValue.of(LocalDate.ofYearDay(2021, 1));

    assertThat(value.value()).isEqualTo("1609459200000");
    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(dateDef).render())
          .isEqualTo("<strong>2021-01-01</strong>");
    } else {
      assertThat(value.toDisplayString(dateDef)).isEqualTo("2021-01-01");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_listOfLongs(boolean useFormatted) {
    QuestionDefinition longDef =
        testQuestionBank.numberApplicantJugglingNumber().getQuestionDefinition();
    PredicateValue value = PredicateValue.listOfLongs(ImmutableList.of(1L, 2L, 3L));

    assertThat(value.value()).isEqualTo("[1, 2, 3]");
    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(longDef).render())
          .isEqualTo("<strong>[1, 2, 3]</strong>");
    } else {
      assertThat(value.toDisplayString(longDef)).isEqualTo("[1, 2, 3]");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_simpleList(boolean useFormatted) {
    QuestionDefinition stringDef =
        testQuestionBank.textApplicantFavoriteColor().getQuestionDefinition();
    PredicateValue value = PredicateValue.listOfStrings(ImmutableList.of("kangaroo", "turtle"));

    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(stringDef).render())
          .isEqualTo("<strong>[&quot;kangaroo&quot;, &quot;turtle&quot;]</strong>");
    } else {
      assertThat(value.toDisplayString(stringDef)).isEqualTo("[\"kangaroo\", \"turtle\"]");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_multiOptionList(boolean useFormatted) {
    QuestionDefinition multiOption =
        testQuestionBank.dropdownApplicantIceCream().getQuestionDefinition();

    PredicateValue value = PredicateValue.listOfStrings(ImmutableList.of("1", "2"));

    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(multiOption).render())
          .isEqualTo("<strong>[chocolate, strawberry]</strong>");
    } else {
      assertThat(value.toDisplayString(multiOption)).isEqualTo("[chocolate, strawberry]");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_multiOptionSingleValue_convertsIdToString(boolean useFormatted) {
    QuestionDefinition multiOption =
        testQuestionBank.checkboxApplicantKitchenTools().getQuestionDefinition();

    PredicateValue value = PredicateValue.of("1");

    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(multiOption).render())
          .isEqualTo("<strong>toaster</strong>");
    } else {
      assertThat(value.toDisplayString(multiOption)).isEqualTo("toaster");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_multiOptionList_missingIdDefaultsToObsolete(boolean useFormatted) {
    QuestionDefinition multiOption =
        testQuestionBank.dropdownApplicantIceCream().getQuestionDefinition();

    PredicateValue value =
        PredicateValue.listOfStrings(ImmutableList.of("1", "100")); // 100 is not a valid ID

    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(multiOption).render())
          .isEqualTo("<strong>[chocolate, &lt;obsolete&gt;]</strong>");
    } else {
      assertThat(value.toDisplayString(multiOption)).isEqualTo("[chocolate, <obsolete>]");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_pairOfDates(boolean useFormatted) {
    QuestionDefinition dateDef = testQuestionBank.dateApplicantBirthdate().getQuestionDefinition();

    LocalDate date1 = LocalDate.of(2024, 5, 1);
    LocalDate date2 = LocalDate.of(2024, 5, 2);
    PredicateValue value = PredicateValue.pairOfDates(date1, date2);

    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(dateDef).render())
          .isEqualTo("<strong>2024-05-01</strong> and <strong>2024-05-02</strong>");
    } else {
      assertThat(value.toDisplayString(dateDef)).isEqualTo("2024-05-01 and 2024-05-02");
    }
  }

  @Test
  @Parameters({"false", "true"})
  public void toDisplayString_pairOfLongs(boolean useFormatted) {
    QuestionDefinition dateDef = testQuestionBank.dateApplicantBirthdate().getQuestionDefinition();

    PredicateValue value = PredicateValue.pairOfLongs(18, 30);

    if (useFormatted) {
      assertThat(value.toDisplayFormattedHtml(dateDef).render())
          .isEqualTo("<strong>18</strong> and <strong>30</strong>");
    } else {
      assertThat(value.toDisplayString(dateDef)).isEqualTo("18 and 30");
    }
  }

  @Test
  public void valueWithoutSurroundingQuotes_string() {
    PredicateValue value = PredicateValue.of("hello");
    assertThat(value.valueWithoutSurroundingQuotes()).isEqualTo("hello");
  }

  @Test
  public void toSelectedValue_singleLong_formatsCurrencyWithoutDollarSign() {
    PredicateValue predicateValue = PredicateValue.of(10000L);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.CURRENCY);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.SINGLE);
    assertThat(selectedValue.single()).isEqualTo("100.00");
  }

  @Test
  public void toSelectedValue_singleLong_formatsNumberAsString() {
    PredicateValue predicateValue = PredicateValue.of(10000L);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.NUMBER);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.SINGLE);
    assertThat(selectedValue.single()).isEqualTo("10000");
  }

  @Test
  public void toSelectedValue_singleDate_formatsDateString() {
    LocalDate date = LocalDate.of(2025, 1, 1);
    PredicateValue predicateValue = PredicateValue.of(date);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.DATE);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.SINGLE);
    assertThat(selectedValue.single()).isEqualTo("2025-01-01");
  }

  @Test
  public void toSelectedValue_incorrectQuestionType_correctlyFormatsDateString() {
    LocalDate date = LocalDate.of(2025, 1, 1);
    PredicateValue predicateValue = PredicateValue.of(date);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.CURRENCY);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.SINGLE);
    assertThat(selectedValue.single()).isEqualTo("2025-01-01");
  }

  @Test
  public void toSelectedValue_pairOfDates_formatsBothDateStrings() {
    LocalDate date1 = LocalDate.of(2025, 1, 1);
    LocalDate date2 = LocalDate.of(1900, 1, 1);
    PredicateValue predicateValue = PredicateValue.pairOfDates(date1, date2);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.DATE);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.PAIR);
    assertThat(selectedValue.pair())
        .isEqualTo(new SelectedValue.ValuePair("2025-01-01", "1900-01-01"));
  }

  @Test
  public void toSelectedValue_pairOfLongs_formatsBothCurrencyStrings() {
    PredicateValue predicateValue = PredicateValue.pairOfLongs(100, 200);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.CURRENCY);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.PAIR);
    assertThat(selectedValue.pair()).isEqualTo(new SelectedValue.ValuePair("1.00", "2.00"));
  }

  @Test
  public void toSelectedValue_listOfStrings_createsMultipleValues() {
    ImmutableList<String> values = ImmutableList.of("A", "B", "C");
    PredicateValue predicateValue = PredicateValue.listOfStrings(values);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.CHECKBOX);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.MULTIPLE);
    assertThat(selectedValue.multiple()).isEqualTo(ImmutableSet.copyOf(values));
  }

  @Test
  public void toSelectedValue_listOfLongs_createsMultipleValues() {
    ImmutableList<Long> values = ImmutableList.of(100L, 200L, 300L);
    PredicateValue predicateValue = PredicateValue.listOfLongs(values);

    SelectedValue selectedValue = predicateValue.toSelectedValue(QuestionType.CHECKBOX);

    assertThat(selectedValue.getKind()).isEqualTo(SelectedValue.Kind.MULTIPLE);
    assertThat(selectedValue.multiple()).isEqualTo(ImmutableSet.of("100", "200", "300"));
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
