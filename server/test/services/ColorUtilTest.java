package services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class ColorUtilTest {

  @Test
  public void contrastsWithWhite_true() {
    String darkBlue = "#0747ad";
    assertTrue(ColorUtil.contrastsWithWhite(darkBlue));
  }

  @Test
  public void contrastsWithWhite_false() {
    String lightBlue = "#8bb4f7";
    assertFalse(ColorUtil.contrastsWithWhite(lightBlue));
  }

  @Test
  public void contrastsWithWhite_threeDigitHex() {
    String black = "#000";
    assertTrue(ColorUtil.contrastsWithWhite(black));
  }

  @Test
  public void contrastsWithWhite_badHexCode() {
    String badColor = "#000FFFEE";
    assertFalse(ColorUtil.contrastsWithWhite(badColor));
  }
}
