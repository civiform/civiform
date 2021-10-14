package services.applicant;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents a US currency amount.
 *
 * Accepts various string formats with a required dollars value with no leading 0, unless the value is 0.
 * Optionally:
 *   May contain commas in the dollars: 12,345
 *   May contain exactly 2 decimal points for cents.
 *
 * and returns
 */
public class Currency {
  // Currency containing only numbers, without leading 0s and optional 2 digit cents.
  private static Pattern CURRENCY_NO_COMMAS = Pattern.compile("^[1-9]\\d*(?:\\.\\d\\d)?$");
  // Same as CURRENCY_NO_COMMAS but commas followed by 3 digits are allowed.
  private static Pattern CURRENCY_WITH_COMMAS = Pattern
      .compile("^[1-9]\\d{0,2}(?:,\\d\\d\\d)*(?:\\.\\d\\d)?$");
  // Currency of 0 dollars with optional 2 digit cents.
  private static Pattern CURRENCY_ZERO_DOLLARS = Pattern.compile("^0(?:\\.\\d\\d)?$");

  // The database storage is a long, so use a long here too.
  private Long cents = 0L;

  private Currency(Long cents) {
    this.cents = cents;
  }

  public static Currency parse(String currency) {
    if(!validate(currency)) {
      throw new IllegalArgumentException(
          String.format("Currency is misformatted: %s", currency));
    }
    try {
      double dollars = NumberFormat.getNumberInstance(Locale.US).parse(currency).doubleValue();
      Double cents = dollars * 100;
      return new Currency(cents.longValue());
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
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

  /**
   * Returns the currency number as a US formatted human readable string.
   *
   * The string will always have 2 decimals, and commas, but not a dollar sign.
   */
  public String getPrettyPrint() {
    // Format with commas and 2 decimal cents always.
    NumberFormat formatter = NumberFormat.getInstance(Locale.US);
    formatter.setMinimumFractionDigits(2);
    formatter.setMaximumFractionDigits(2);
    Double dollars = cents.doubleValue() / 100.0;
    return formatter.format(dollars);
  }



}
