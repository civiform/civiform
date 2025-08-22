package services.question.types;

import com.google.common.collect.ImmutableSet;

public final class YesNoQuestionConstants {

  public static final long YES_OPTION_ID = 1L;
  public static final long NO_OPTION_ID = 0L;
  public static final long NOT_SURE_OPTION_ID = 2L;
  public static final long MAYBE_OPTION_ID = 3L;

  public static final String YES_ADMIN_NAME = "yes";
  public static final String NO_ADMIN_NAME = "no";
  public static final String NOT_SURE_ADMIN_NAME = "not-sure";
  public static final String MAYBE_ADMIN_NAME = "maybe";

  public static final ImmutableSet<String> VALID_YES_NO_OPTIONS =
      ImmutableSet.of(YES_ADMIN_NAME, NO_ADMIN_NAME, MAYBE_ADMIN_NAME, NOT_SURE_ADMIN_NAME);

  public static final ImmutableSet<String> REQUIRED_YES_NO_OPTIONS =
      ImmutableSet.of(YES_ADMIN_NAME, NO_ADMIN_NAME);

  private YesNoQuestionConstants() {}
}
