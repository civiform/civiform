package services;

/** WellKnownPaths are paths commonly referenced, e.g. the applicant's name. */
public class WellKnownPaths {
  // These need to stay in sync with the correct paths for a top-level question
  // asking the applicant their name. This is seeded by the DatabaseSeedTask.
  public static Path APPLICANT_FIRST_NAME = Path.create("applicant.name.first_name");
  public static Path APPLICANT_MIDDLE_NAME = Path.create("applicant.name.middle_name");
  public static Path APPLICANT_LAST_NAME = Path.create("applicant.name.last_name");
  public static Path APPLICANT_NAME = Path.create("applicant.name");
  public static Path APPLICANT_DOB = Path.create("applicant.applicant_date_of_birth");
}
