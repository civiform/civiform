package auth;

/**
 * This enum represents Authorizers to be used with pac4j.
 *
 * <p>The enum values should always look like &lt;Authorizer&gt;(Labels.&lt;Authorizer&gt;).
 */
public enum Authorizers {
  APPLICANT(Labels.APPLICANT),
  UAT_ADMIN(Labels.UAT_ADMIN),
  TI(Labels.TI),
  PROGRAM_ADMIN(Labels.PROGRAM_ADMIN);

  /**
   * This Labels class is required to provide references to String constants for {@link
   * org.pac4j.play.java.Secure} annotations. This nested static class should only contain constant
   * string values that are used to initialize {@link Authorizers}.
   */
  public static final class Labels {
    public static final String APPLICANT = "applicant";
    public static final String UAT_ADMIN = "uatadmin";
    public static final String TI = "trustedintermediary";
    public static final String PROGRAM_ADMIN = "programadmin";
  }

  private final String label;

  private Authorizers(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
