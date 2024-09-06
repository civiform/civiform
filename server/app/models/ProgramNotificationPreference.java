package models;

import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;
import java.util.List;

/** Represents the notification preferences for a Program. */
public enum ProgramNotificationPreference {
  // Send an email notification to the program admin for every application submission
  EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS;

  @DbEnumValue(storage = DbEnumType.VARCHAR)
  public String getValue() {
    return this.name();
  }

  public static ImmutableList<ProgramNotificationPreference> getDefaults() {
    return ImmutableList.of(EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS);
  }

  public static List<String> getDefaultsForForm() {
    return getDefaults().stream().map(p -> p.getValue()).toList();
  }
}
