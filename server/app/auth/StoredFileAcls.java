package auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import models.AccountModel;
import services.program.ProgramDefinition;

/**
 * Stores access control state for {@link models.StoredFileModel}s.
 *
 * <p>Program admins may read a file if they are an admin for a program included in the {@code
 * programReadAcls} for that file.
 *
 * <p>Applicants may read a file if they are included in the {@code applicantReadAcls} for that
 * file. Note: The Applicant acl is new as of February 2026, and it serves as a supplement to the
 * existing acl which is the uploader's applicant id in the formated stored file name.
 */
public final class StoredFileAcls {

  @JsonProperty("programReadAcls")
  private Set<String> programReadAcls;

  /** Applicant IDs additionally able to access the file. */
  @JsonProperty("applicantReadAcls")
  private Set<Long> applicantReadAcls;

  public StoredFileAcls() {
    programReadAcls = new HashSet<>();
    applicantReadAcls = new HashSet<>();
  }

  @JsonCreator
  public StoredFileAcls(
      @Nullable @JsonProperty("programReadAcls") Set<String> programReadAcls,
      @Nullable @JsonProperty("applicantReadAcls") Set<Long> applicantReadAcls) {
    // If the file was created before the migration to using StoredFileAcls,
    // programReadAcls will be null on initial load. In this case we initialize
    // the internal state of the ACLs to an empty collection so the migration
    // task can populate them.
    // It is safest to leave this code in, even after the migration, in the
    // event that somehow a StoredFile is created without serializing an
    // instance StoredFileAcls.
    this.programReadAcls = Objects.requireNonNullElseGet(programReadAcls, HashSet::new);
    // Before February 2026 applicantReadAcls will not be present.
    this.applicantReadAcls = Objects.requireNonNullElseGet(applicantReadAcls, HashSet::new);
  }

  public ImmutableSet<String> getProgramReadAcls() {
    return ImmutableSet.copyOf(programReadAcls);
  }

  public StoredFileAcls addProgramToReaders(ProgramDefinition programDefinition) {
    programReadAcls.add(programDefinition.adminName());
    return this;
  }

  public boolean hasProgramReadPermission(AccountModel account) {
    return account.getAdministeredProgramNames().stream().anyMatch(programReadAcls::contains);
  }

  public ImmutableSet<Long> getApplicantReadAcls() {
    return ImmutableSet.copyOf(applicantReadAcls);
  }

  public void addApplicantToReaders(long applicantId) {
    applicantReadAcls.add(applicantId);
  }

  public boolean hasApplicantReadPermission(long applicantId) {
    return applicantReadAcls.contains(applicantId);
  }
}
