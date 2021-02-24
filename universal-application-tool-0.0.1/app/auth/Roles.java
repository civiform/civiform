package auth;

public enum Roles {
  ROLE_APPLICANT("ROLE_APPLICANT"),
  ROLE_TI("ROLE_TI"),
  ROLE_UAT_ADMIN("ROLE_UAT_ADMIN"),
  ROLE_PROGRAM_ADMIN("ROLE_PROGRAM_ADMIN");

  private final String roleName;
  public static final String APPLICANT_AUTHORIZER = "applicant";
  public static final String UAT_ADMIN_AUTHORIZER = "uatadmin";
  public static final String TI_AUTHORIZER = "trustedintermediary";
  public static final String PROGRAM_ADMIN_AUTHORIZER = "programadmin";

  Roles(String roleName) {
    this.roleName = roleName;
  }

  @Override
  public String toString() {
    return this.roleName;
  }
}
