package auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import javax.annotation.Nullable;
import models.Account;
import services.program.ProgramDefinition;

/**
 * Stores access control state for {@link models.StoredFile}s.
 *
 * <p>Program admins may read a file if they are an admin for a program included in the {@code
 * programReadAcls} for that file.
 */
public class StoredFileAcls {

  @JsonProperty("programReadAcls")
  private HashSet<String> programReadAcls;

  public StoredFileAcls() {
    this.programReadAcls = new HashSet<>();
  }

  @JsonCreator
  public StoredFileAcls(
      @Nullable @JsonProperty("programReadAcls") HashSet<String> programReadAcls) {
    // If the file was created before the migration to using StoredFileAcls,
    // programReadAcls will be null on initial load. In this case we initialize
    // the internal state of the ACLs to an empty collection so the migration
    // task can populate them.
    // It is safest to leave this code in, even after the migration, in the
    // event that somehow a StoredFile is created without serializing an
    // instance StoredFileAcls.
    if (programReadAcls == null) {
      this.programReadAcls = new HashSet<>();
    } else {
      this.programReadAcls = programReadAcls;
    }
  }

  public ImmutableSet<String> getProgramReadAcls() {
    return ImmutableSet.copyOf(programReadAcls);
  }

  public StoredFileAcls addProgramToReaders(ProgramDefinition programDefinition) {
    this.programReadAcls.add(programDefinition.adminName());
    return this;
  }

  public boolean hasProgramReadPermission(Account account) {
    return account.getAdministeredProgramNames().stream().anyMatch(programReadAcls::contains);
  }

  public boolean hasProgramReadPermission(ProgramDefinition programDefinition) {
    return programReadAcls.contains(programDefinition.adminName());
  }
}
