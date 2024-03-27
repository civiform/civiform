package services.applicant;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents a US currency amount.
 *
 * <p>Accepts various string formats with a required dollars value with no leading 0, unless the
 * value is 0.
 *
 * <p>Optionally:
 *
 * <ul>
 *   <li>May contain commas in the dollars: 12,345
 *   <li>May contain exactly 2 decimal points for cents: 34.56
 */
public final class Currency {

  // Currency validation regexs.
  // Currency containing only numbers, without leading 0s and optional 2 digit cents.
  private static final Pattern CURRENCY_NO_COMMAS = Pattern.compile("^[1-9]\\d*(?:\\.\\d\\d)?$");
  // Same as CURRENCY_NO_COMMAS but commas followed by 3 digits are allowed.
  private static final Pattern CURRENCY_WITH_COMMAS =
      Pattern.compile("^[1-9]\\d{0,2}(?:,\\d\\d\\d)*(?:\\.\\d\\d)?$");
  // Currency of 0 dollars with optional 2 digit cents.
  private static final Pattern CURRENCY_ZERO_DOLLARS = Pattern.compile("^0(?:\\.\\d\\d)?$");

  private final Long cents;

  /** Constructs a new Currency of the specified cents. */
  public Currency(long cents) {
    this.cents = cents;
  }

  /**
   * Validates and creates a new Currency based on a dollars and optional cents string. See class
   * comment for more on the valid formats.
   */
  public static Currency parse(String currency) {
    if (!validate(currency)) {
      throw new IllegalArgumentException(String.format("Currency is misformatted: %s", currency));
    }
    try {
      BigDecimal bigDollars =
          BigDecimal.valueOf(
              NumberFormat.getNumberInstance(Locale.US).parse(currency).doubleValue());
      long cents = bigDollars.multiply(new BigDecimal(100)).longValue();
      return new Currency(cents);
    } catch (ParseException e) {
      throw new IllegalArgumentException(
          String.format("Currency is misformatted: %s", currency), e);
    }
  }

  // Returns if the currency string is a valid format.
  public static boolean validate(String currency) {
    return CURRENCY_NO_COMMAS.matcher(currency).matches()
        || CURRENCY_WITH_COMMAS.matcher(currency).matches()
        || CURRENCY_ZERO_DOLLARS.matcher(currency).matches();
  }

  public Long getCents() {
    return cents;
  }

  public Double getDollars() {
    return cents.doubleValue() / 100.0;
  }

  /**
   * Returns the currency number as a string of numbers. There will always be at least one dollar
   * and exactly 2 cents digits.
   */
  public String getDollarsString() {
    Double dollars = cents.doubleValue() / 100.0;
    // 0 is the format for minimal required digits.
    DecimalFormat myFormatter = new DecimalFormat("0.00");
    return myFormatter.format(dollars);
  }

  /**
   * Returns the currency number as a US formatted human readable string.
   *
   * <p>The string will always have 2 decimals, and commas, but not a dollar sign.
   */
  public String prettyPrint() {
    // Format with commas and 2 decimal cents always.
    NumberFormat formatter = NumberFormat.getInstance(Locale.US);
    formatter.setMinimumFractionDigits(2);
    formatter.setMaximumFractionDigits(2);
    Double dollars = cents.doubleValue() / 100.0;
    return formatter.format(dollars);
  }
}
