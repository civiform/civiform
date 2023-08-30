package services;

/** WellKnownPaths are paths commonly referenced, e.g. the applicant's name. */
public final class WellKnownPaths {
  // These need to stay in sync with the correct paths for a top-level question
  // asking the applicant their name. This is seeded by the DatabaseSeedTask.
  public static Path APPLICANT_FIRST_NAME = Path.create("actionable_answers.name.first_name");
  public static Path APPLICANT_MIDDLE_NAME = Path.create("actionable_answers.name.middle_name");
  public static Path APPLICANT_LAST_NAME = Path.create("actionable_answers.name.last_name");
  public static Path APPLICANT_NAME = Path.create("actionable_answers.name");
  public static Path APPLICANT_DOB = Path.create("applicant.applicant_date_of_birth");
  public static Path APPLICANT_EMAIL = Path.create("actionable_answers.email");
  public static Path APPLICANT_PHONE_NUMBER = Path.create("actionable_answers.phone_number.phone_number");
  public static Path APPLICANT_COUNTRY_CODE = Path.create("actionable_answers.phone_number.country_code");
}
