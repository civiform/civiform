package services.question;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;

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

  /** Finds an option by its admin name. */
  public static YesNoQuestionOption fromId(long id) {
    return Arrays.stream(values())
        .filter(option -> option.getId() == id)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown id: %d".formatted(id)));
  }

  /**
   * Convert to optional boolean.
   *
   * <p>
   *
   * <ul>
   *   <li>{@link YesNoQuestionOption#YES} is true
   *   <li>{@link YesNoQuestionOption#NO} is false
   *   <li>Other values are empty optional
   * </ul>
   */
  public Optional<Boolean> toOptionalBoolean() {
    return switch (this) {
      case YES -> Optional.of(true);
      case NO -> Optional.of(false);
      default -> Optional.empty();
    };
  }

  /**
   * Convert from a boolean.
   *
   * <p>
   *
   * <ul>
   *   <li>True is {@link YesNoQuestionOption#YES}
   *   <li>False is {@link YesNoQuestionOption#NO}
   * </ul>
   */
  public static YesNoQuestionOption fromBoolean(Boolean value) {
    if (value == true) {
      return YES;
    }

    return NO;
  }
}
