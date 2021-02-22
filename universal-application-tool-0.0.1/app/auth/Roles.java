package auth;

public enum Roles {
  ROLE_APPLICANT("ROLE_APPLICANT"),
  ROLE_TI("ROLE_TI"),
  ROLE_UAT_ADMIN("ROLE_UAT_ADMIN"),
  ROLE_PROGRAM_ADMIN("ROLE_PROGRAM_ADMIN");

  private final String roleName;

  Roles(String roleName) {
    this.roleName = roleName;
  }

  public String toString() {
    return this.roleName;
  }
}
