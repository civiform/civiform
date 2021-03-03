package views;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class StyleUtilsTest {

    @Test
    public void applyUtilityClass_singleReturnsExpected() {
        String expected = "hover:bg-blue-100";
        String result = StyleUtils.applyUtilityClass(StyleUtils.HOVER, Styles.BG_BLUE_100);

        assertEquals(expected, result);
    }

    @Test
    public void applyUtility_multiReturnsExpected() {
        String expected = "focus:outline-none focus:ring-2";
        String result = StyleUtils.applyUtilityClass(
            StyleUtils.FOCUS, ImmutableList.of(Styles.OUTLINE_NONE, Styles.RING_2));

        assertEquals(expected, result);
    }
    
}