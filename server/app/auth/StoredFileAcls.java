package auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import models.AccountModel;
import services.program.ProgramDefinition;

/**
 * Stores access control state for {@link models.StoredFileModel}s.
 *
 * <p>Program admins may read a file if they are an admin for a program included in the {@code
 * programReadAcls} for that file.
 */
public final class StoredFileAcls {

  @JsonProperty("programReadAcls")
  private Set<String> programReadAcls;

  public StoredFileAcls() {
    this.programReadAcls = new HashSet<>();
  }

  @JsonCreator
  public StoredFileAcls(@Nullable @JsonProperty("programReadAcls") Set<String> programReadAcls) {
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

  public boolean hasProgramReadPermission(AccountModel account) {
    return account.getAdministeredProgramNames().stream().anyMatch(programReadAcls::contains);
  }

  public boolean hasProgramReadPermission(ProgramDefinition programDefinition) {
    return programReadAcls.contains(programDefinition.adminName());
  }
}
