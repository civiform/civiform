package services.program;

public final record ProgramApplicationTableRow(
    String applicantName,
    Long applicationId,
    String applicationStatus,
    String eligibilityStatus,
    String submitTime,
    Long applicationProgramId) {}
