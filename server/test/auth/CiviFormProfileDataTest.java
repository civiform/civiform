package auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ObjectStreamClass;
import org.junit.Test;

public class CiviFormProfileDataTest {
  @Test
  // Ensure that the serialVersionUID does not change. If it changes, then existing profiles cannot
  // be read and guest sessions will be lost.
  public void testSerialVersionUID() {
    ObjectStreamClass civiFormProfileDataOSC = ObjectStreamClass.lookup(CiviFormProfileData.class);
    assertThat(civiFormProfileDataOSC.getSerialVersionUID()).isEqualTo(3142603030317816700L);
  }
}
