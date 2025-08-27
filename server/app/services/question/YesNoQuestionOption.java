package services.question;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;

/** Enum for YES/NO question options with their IDs and admin names. */
public enum YesNoQuestionOption {
  YES(1L, "yes"),
  NO(0L, "no"),
  NOT_SURE(2L, "not-sure"),
  MAYBE(3L, "maybe");

  private final long id;
  private final String adminName;

  YesNoQuestionOption(long id, String adminName) {
    this.id = id;
    this.adminName = adminName;
  }

  public long getId() {
    return id;
  }

  public String getAdminName() {
    return adminName;
  }

  /** Returns the set of all valid YES/NO option admin names for validation. */
  public static ImmutableSet<String> getAllAdminNames() {
    return Arrays.stream(values())
        .map(YesNoQuestionOption::getAdminName)
        .collect(ImmutableSet.toImmutableSet());
  }

  /** Returns the set of required YES/NO option admin names (yes and no). */
  public static ImmutableSet<String> getRequiredAdminNames() {
    return ImmutableSet.of(YES.getAdminName(), NO.getAdminName());
  }

  /** Finds an option by its admin name. */
  public static YesNoQuestionOption fromAdminName(String adminName) {
    return Arrays.stream(values())
        .filter(option -> option.getAdminName().equals(adminName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown admin name: " + adminName));
  }
}
