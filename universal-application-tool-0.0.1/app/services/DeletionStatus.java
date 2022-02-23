package services;

/**
 * DeletionStatus denotes the state of a question. A draft question not referenced in any programs
 * is deletable. A question referenced in at least one program is not deletable. A question to be
 * archived in the next version is pending deletion, and an archived question is not active.
 */
public enum DeletionStatus {
  NOT_DELETABLE,
  DELETABLE,
  PENDING_DELETION,
  NOT_ACTIVE;
}
