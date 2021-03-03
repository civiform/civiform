package services.applicant;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.DoNotCall;
import services.program.ProgramDefinition;

public class Updater {
  private ApplicantData applicantData;

  Updater(ApplicantData applicantData) {
    this.applicantData = applicantData;
  }

  /** Performs updates on applicant data. */
  void update(ImmutableSet<Update> updates) {
    updates.forEach(update -> applicantData.put(Path.create(update.path()), update.value()));
  }

  /** Runs validation logic on applicant data. */
  @DoNotCall
  final ImmutableSet<String> validate(ProgramDefinition programDefinition) {
    return null;
  }

  /** Saves applicantData to the database. */
  void save() {
    // TODO: Need to verify validate has been called and/or do some other data scrubbing before
    // saving.
  }
}
