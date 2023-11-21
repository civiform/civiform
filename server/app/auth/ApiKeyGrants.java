package auth;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * Stores permissions for {@link models.ApiKeyModel}s.
 *
 * <p>Permissions are represented as a resource identifier paired with an ability. E.g.
 * "utility-discount" program with "read" ability. Multiple permissions may be stored for the same
 * resource. E.g. having both the "read" and "write" permission for a given resource.
 */
public final class ApiKeyGrants {

  /** Enumerates the abilities an ApiKey may have with respect to a resource. */
  public enum Permission {
    WRITE,
    READ
  }

  @JsonProperty("programGrants")
  private Multimap<String, Permission> programGrants;

  /** Create a new instance with no grants. */
  public ApiKeyGrants() {
    this.programGrants = HashMultimap.create();
  }

  /** Used by EBean to deserialize an instance of ApiKeyGrants stored in the database. */
  @JsonCreator
  public ApiKeyGrants(@JsonProperty("programGrants") Multimap<String, Permission> programGrants) {
    this.programGrants = checkNotNull(programGrants);
  }

  public ImmutableMultimap<String, Permission> getProgramGrants() {
    return ImmutableMultimap.copyOf(programGrants);
  }

  /**
   * Grant the ability to do {@code permission} for the program identified by {@code programSlug}.
   */
  public void grantProgramPermission(String programSlug, Permission permission) {
    programGrants.put(programSlug, permission);
  }

  /**
   * Revoke the ability to do {@code permission} for the program identified by {@code programSlug}.
   */
  public void revokeProgramPermission(String programSlug, Permission permission) {
    programGrants.remove(programSlug, permission);
  }

  /**
   * Check for the ability to do {@code permission} for the program identified by {@code
   * programSLug}.
   */
  public boolean hasProgramPermission(String programSlug, Permission permission) {
    return programGrants.get(programSlug).contains(permission);
  }

  /** Revoke all permissions for all programs. */
  public void revokeAllProgramPermissions() {
    this.programGrants = HashMultimap.create();
  }
}
