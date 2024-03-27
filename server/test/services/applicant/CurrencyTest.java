package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class CurrencyTest {

  private static ImmutableList<Object[]> getTestData() {
    return ImmutableList.of(
        // Zero dollars
        new Object[] {TestData.create("0", "0.00", 0, "0.00", 0)}, // Zero
        new Object[] {TestData.create("0.00", "0.00", 0, "0.00", 0)}, // Zero with cents

        // Non zero Single dollars.
        new Object[] {TestData.create("1", "1.00", 1, "1.00", 100)}, // Single dollars
        new Object[] {TestData.create("0.40", "0.40", 0.40, "0.40", 40)}, // Only cents
        new Object[] {TestData.create("1.23", "1.23", 1.23, "1.23", 123)},

        // Large values
        new Object[] {TestData.create("12345", "12,345.00", 12345, "12345.00", 12345 * 100)},

        // With comma
        new Object[] {TestData.create("12,345", "12,345.00", 12345, "12345.00", 12345 * 100)},

        // With cents.
        new Object[] {TestData.create("12345.67", "12,345.67", 12345.67, "12345.67", 1234567)},

        // With cents to demonstrate fixing a floating point issue
        new Object[] {TestData.create("18500.01", "18,500.01", 18500.01, "18500.01", 1850001)},

        // With comma and cents.
        new Object[] {TestData.create("12,345.67", "12,345.67", 12345.67, "12345.67", 1234567)});
  }

  @Test
  @Parameters(method = "getTestData")
  public void constructor(TestData testData) {
    Currency currency = new Currency(testData.cents());
    assertThat(currency.getCents()).isEqualTo(testData.cents());
  }

  @Test
  @Parameters(method = "getTestData")
  public void parse_validValuesParse(TestData testData) {
    Currency currency = Currency.parse(testData.userInput());
    assertThat(currency.getCents()).isEqualTo(testData.cents());
  }

  @Test
  @Parameters(method = "getTestData")
  public void getDollarsString(TestData testData) {
    Currency currency = Currency.parse(testData.userInput());
    assertThat(currency.getDollarsString()).isEqualTo(testData.dollarsString());
  }

  @Test
  @Parameters(method = "getTestData")
  public void prettyPrint(TestData testData) {
    Currency currency = Currency.parse(testData.userInput());
    assertThat(currency.prettyPrint()).isEqualTo(testData.prettyString());
  }

  @Test
  @Parameters({
    "$1", // Dollar sign.
    "1.2", // too few decimals.
    "1.234", // too many decimals
    "\\,1.23", // too many decimals
    "1\\,00.23", // too many decimals
    "01" // Leading 0
  })
  public void parse_invalidValuesFail(String currency) {
    assertThatThrownBy(() -> Currency.parse(currency)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Parameters(method = "getTestData")
  public void getDollars(TestData testData) {
    Currency currency = Currency.parse(testData.userInput());
    assertThat(currency.getDollars()).isEqualTo(testData.dollars());
  }

  @Test
  @Parameters(method = "getTestData")
  public void getCents(TestData testData) {
    Currency currency = Currency.parse(testData.userInput());
    assertThat(currency.getCents()).isEqualTo(testData.cents());
  }

  @AutoValue
  abstract static class TestData {

    static TestData create(
        String userInput, String prettyString, double dollars, String dollarsString, long cents) {
      return new AutoValue_CurrencyTest_TestData(
          userInput, prettyString, dollars, dollarsString, Long.valueOf(cents));
    }

    abstract String userInput();

    abstract String prettyString();

    abstract double dollars();

    abstract String dollarsString();

    abstract Long cents();
  }
}
