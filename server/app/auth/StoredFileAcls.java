package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
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
  public StoredFileAcls(@JsonProperty("programReadAcls") HashSet<String> programReadAcls) {
    this.programReadAcls = checkNotNull(programReadAcls);
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
}
