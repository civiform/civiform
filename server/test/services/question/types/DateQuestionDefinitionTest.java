package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.CiviFormError;
import services.LocalizedStrings;
import services.question.types.DateQuestionDefinition.DateValidationOption;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.DateQuestionDefinition.DateValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class DateQuestionDefinitionTest {
  private static final DateValidationOption ANY_DATE =
      DateValidationOption.builder().setDateType(DateType.ANY).build();
  private static final DateValidationOption APPLICATION_DATE =
      DateValidationOption.builder().setDateType(DateType.APPLICATION_DATE).build();
  private static final DateValidationOption EMPTY_CUSTOM_DATE =
      DateValidationOption.builder().setDateType(DateType.CUSTOM).build();
  private static final DateValidationOption CUSTOM_DATE_INVALID_TYPE =
      DateValidationOption.builder()
          .setDateType(DateType.ANY)
          .setCustomDate(Optional.of(LocalDate.of(2025, 1, 1)))
          .build();
  private static final DateValidationOption CUSTOM_DATE_INVALID_YEAR =
      DateValidationOption.builder()
          .setDateType(DateType.CUSTOM)
          .setCustomDate(Optional.of(LocalDate.of(999999, 1, 1)))
          .build();

  @SuppressWarnings({"JavaTimeDefaultTimeZone", "TimeInStaticInitializer"})
  private static final DateValidationOption CUSTOM_PAST_DATE =
      DateValidationOption.builder()
          .setDateType(DateType.CUSTOM)
          .setCustomDate(Optional.of(LocalDate.now(Clock.systemDefaultZone()).minusDays(10)))
          .build();

  @SuppressWarnings({"JavaTimeDefaultTimeZone", "TimeInStaticInitializer"})
  private static final DateValidationOption CUSTOM_FUTURE_DATE =
      DateValidationOption.builder()
          .setDateType(DateType.CUSTOM)
          .setCustomDate(Optional.of(LocalDate.now(Clock.systemDefaultZone()).plusDays(10)))
          .build();

  @SuppressWarnings("unused") // Is used via reflection by the @Parameters annotation below
  private static ImmutableList<Object[]> getValidationTestData() {
    return ImmutableList.of(
        new Object[] {Optional.empty(), Optional.empty(), Optional.<String>empty()},
        new Object[] {
          Optional.empty(),
          Optional.of(ANY_DATE),
          Optional.of("Both start and end date are required")
        },
        new Object[] {
          Optional.of(ANY_DATE),
          Optional.empty(),
          Optional.of("Both start and end date are required")
        },
        new Object[] {Optional.of(ANY_DATE), Optional.of(ANY_DATE), Optional.<String>empty()},
        new Object[] {
          Optional.of(ANY_DATE), Optional.of(APPLICATION_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(ANY_DATE),
          Optional.of(EMPTY_CUSTOM_DATE),
          Optional.of("A valid date is required for custom start and end dates")
        },
        new Object[] {
          Optional.of(ANY_DATE),
          Optional.of(CUSTOM_DATE_INVALID_YEAR),
          Optional.of("A valid date is required for custom start and end dates")
        },
        new Object[] {
          Optional.of(ANY_DATE),
          Optional.of(CUSTOM_DATE_INVALID_TYPE),
          Optional.of("Specific date must be empty if start and end date are not \"Custom date\"")
        },
        new Object[] {
          Optional.of(ANY_DATE), Optional.of(CUSTOM_PAST_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(ANY_DATE), Optional.of(CUSTOM_FUTURE_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(APPLICATION_DATE), Optional.of(ANY_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(APPLICATION_DATE), Optional.of(APPLICATION_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(APPLICATION_DATE),
          Optional.of(CUSTOM_PAST_DATE),
          Optional.of("End date cannot be before today's date")
        },
        new Object[] {
          Optional.of(APPLICATION_DATE), Optional.of(CUSTOM_FUTURE_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(EMPTY_CUSTOM_DATE),
          Optional.of(ANY_DATE),
          Optional.of("A valid date is required for custom start and end dates")
        },
        new Object[] {
          Optional.of(CUSTOM_DATE_INVALID_YEAR),
          Optional.of(ANY_DATE),
          Optional.of("A valid date is required for custom start and end dates")
        },
        new Object[] {
          Optional.of(CUSTOM_DATE_INVALID_TYPE),
          Optional.of(ANY_DATE),
          Optional.of("Specific date must be empty if start and end date are not \"Custom date\"")
        },
        new Object[] {
          Optional.of(CUSTOM_PAST_DATE), Optional.of(ANY_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(CUSTOM_PAST_DATE), Optional.of(APPLICATION_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(CUSTOM_PAST_DATE), Optional.of(CUSTOM_FUTURE_DATE), Optional.<String>empty()
        },
        new Object[] {
          Optional.of(CUSTOM_FUTURE_DATE),
          Optional.of(APPLICATION_DATE),
          Optional.of("Start date cannot be after today's date")
        },
        new Object[] {
          Optional.of(CUSTOM_FUTURE_DATE),
          Optional.of(CUSTOM_PAST_DATE),
          Optional.of("Start date cannot be after end date")
        });
  }

  @Test
  @Parameters(method = "getValidationTestData")
  public void validate_settingConstraints(
      Optional<DateValidationOption> minDate,
      Optional<DateValidationOption> maxDate,
      Optional<String> expectedErrorMessage) {
    QuestionDefinitionConfig config =
        makeConfigBuilder()
            .setValidationPredicates(
                DateValidationPredicates.builder().setMinDate(minDate).setMaxDate(maxDate).build())
            .build();
    QuestionDefinition question = new DateQuestionDefinition(config);

    ImmutableSet<CiviFormError> errors = question.validate();

    assertThat(errors)
        .isEqualTo(
            expectedErrorMessage
                .map(CiviFormError::of)
                .map(ImmutableSet::of)
                .orElse(ImmutableSet.of()));
  }

  private QuestionDefinitionConfig.Builder makeConfigBuilder() {
    return QuestionDefinitionConfig.builder()
        .setName("name")
        .setDescription("description")
        .setQuestionText(LocalizedStrings.of(Locale.US, "question?"));
  }
}
