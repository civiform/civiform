package services.program;

import models.ApplicationModel;
import services.statuses.StatusDefinitions;

import java.time.Instant;
import java.util.Optional;

public final record ProgramApplicationTableRow(String  applicantName, Long applicationId, String applicationStatus, String eligibilityStatus, String submitTime, Long applicationProgramId) {

  }


