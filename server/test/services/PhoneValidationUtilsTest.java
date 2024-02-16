package services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class PhoneValidationUtilsTest {
  private static final Optional<String> PHONE_NUMBER_GOOD_US = Optional.of("206-684-2489");
  private static final Optional<String> PHONE_NUMBER_GOOD_CA = Optional.of("250-555-0199");
  private static final Optional<String> PHONE_NUMBER_BAD_AREA_CODE = Optional.of("000-555-0199");
  private static final Optional<String> PHONE_NUMBER_BAD_CHARACTERS = Optional.of("555-555-555a");
  private static final Optional<String> PHONE_NUMBER_BAD_TOO_LONG = Optional.of("555-555-55551");
  private static final Optional<String> PHONE_NUMBER_BAD_TOO_SHORT = Optional.of("555-555-555");
  private static final Optional<String> PHONE_NUMBER_BAD_NO_VALUE = Optional.of("");
  private static final Optional<String> PHONE_NUMBER_BAD_EMPTY = Optional.empty();

  private static final Optional<String> COUNTRY_CODE_US = Optional.of("US");
  private static final Optional<String> COUNTRY_CODE_CA = Optional.of("CA");
  private static final Optional<String> COUNTRY_CODE_NO_VALUE = Optional.of("");
  private static final Optional<String> COUNTRY_CODE_EMPTY = Optional.empty();

  private static final Optional<MessageKey> MESSAGE_KEY_EMPTY = Optional.empty();

  private Object[] validatePhoneNumber_validValues() {
    return new Object[] {
      // Valid US phone number
      new Object[] {PHONE_NUMBER_GOOD_US, COUNTRY_CODE_US, MESSAGE_KEY_EMPTY},
      // Valid Canadian phone number
      new Object[] {PHONE_NUMBER_GOOD_CA, COUNTRY_CODE_CA, MESSAGE_KEY_EMPTY}
    };
  }

  private Object[] validatePhoneNumber_invalidValues() {
    return new Object[] {
      // Phone number is not set
      new Object[] {
        PHONE_NUMBER_BAD_EMPTY,
        COUNTRY_CODE_US,
        Optional.of(MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED)
      },
      // Phone number is not set
      new Object[] {
        PHONE_NUMBER_BAD_NO_VALUE,
        COUNTRY_CODE_CA,
        Optional.of(MessageKey.PHONE_VALIDATION_NUMBER_REQUIRED)
      },
      // Country code is not set
      new Object[] {
        PHONE_NUMBER_GOOD_US,
        COUNTRY_CODE_EMPTY,
        Optional.of(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER)
      },
      // Country code is not set
      new Object[] {
        PHONE_NUMBER_GOOD_US,
        COUNTRY_CODE_NO_VALUE,
        Optional.of(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER)
      },
      // Phone number contains invalid character(s)
      new Object[] {
        PHONE_NUMBER_BAD_CHARACTERS,
        COUNTRY_CODE_US,
        Optional.of(MessageKey.PHONE_VALIDATION_NON_NUMBER_VALUE)
      },
      // Phone number length is too long
      new Object[] {
        PHONE_NUMBER_BAD_TOO_LONG,
        COUNTRY_CODE_US,
        Optional.of(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER)
      },
      // Phone number length is too short
      new Object[] {
        PHONE_NUMBER_BAD_TOO_SHORT,
        COUNTRY_CODE_US,
        Optional.of(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER)
      },
      // Phone number with bad area code
      new Object[] {
        PHONE_NUMBER_BAD_AREA_CODE,
        COUNTRY_CODE_US,
        Optional.of(MessageKey.PHONE_VALIDATION_INVALID_PHONE_NUMBER)
      },
      // Mismatch phone number and country code
      new Object[] {
        PHONE_NUMBER_GOOD_US,
        COUNTRY_CODE_CA,
        Optional.of(MessageKey.PHONE_VALIDATION_NUMBER_NOT_IN_COUNTRY)
      }
    };
  }

  @Test
  @Parameters(method = "validatePhoneNumber_validValues,validatePhoneNumber_invalidValues")
  public void validatePhoneNumber_hasValidPhoneNumber(
      Optional<String> phoneNumber,
      Optional<String> countryCode,
      Optional<MessageKey> expectedResult) {
    var validationResults = PhoneValidationUtils.validatePhoneNumber(phoneNumber, countryCode);

    assertThat(validationResults).isEqualTo(expectedResult);
  }

  private Object[] calculateCountryCode_findsExpectedCountryCode() {
    return new Object[] {
      // Correct country code for US phone number
      new Object[] {PHONE_NUMBER_GOOD_US, COUNTRY_CODE_US},
      // Correct country code for Canadian phone number
      new Object[] {PHONE_NUMBER_GOOD_CA, COUNTRY_CODE_CA},
      // No country code with empty phone number
      new Object[] {PHONE_NUMBER_BAD_EMPTY, COUNTRY_CODE_EMPTY},
      // No country code with empty phone number
      new Object[] {PHONE_NUMBER_BAD_NO_VALUE, COUNTRY_CODE_EMPTY},
      // No country code with bad phone number
      new Object[] {PHONE_NUMBER_BAD_AREA_CODE, COUNTRY_CODE_EMPTY}
    };
  }

  @Test
  @Parameters(method = "calculateCountryCode_findsExpectedCountryCode")
  public void calculateCountryCode_phoneNumberHasExpectedCountryCode(
      Optional<String> phoneNumber, Optional<String> countryCode) {
    Optional<String> actualCountryCode = PhoneValidationUtils.determineCountryCode(phoneNumber);
    assertThat(actualCountryCode).isEqualTo(countryCode);
  }
}
