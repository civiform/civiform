package auth;

/** Enum class of all roles a civiform profile can have. */
public enum Role {
  ROLE_APPLICANT("ROLE_APPLICANT"),
  ROLE_TI("ROLE_TI"),
  ROLE_CIVIFORM_ADMIN("ROLE_CIVIFORM_ADMIN"),
  ROLE_PROGRAM_ADMIN("ROLE_PROGRAM_ADMIN");

  private final String roleName;

  Role(String roleName) {
    this.roleName = roleName;
  }

  @Override
  public String toString() {
    return this.roleName;
  }
}
