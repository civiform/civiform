package helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class BetterStringJoinerTest {

  @Test
  public void testBasicJoin() {
    BetterStringJoiner joiner = new BetterStringJoiner(", ");
    joiner.add("A").add("B").add("C");

    assertThat(joiner.toString()).isEqualTo("A, B, C");
  }

  @Test
  public void testAddIfNotBlank_skipsBlankValues() {
    BetterStringJoiner joiner = new BetterStringJoiner(", ");
    joiner
        .addIfNotBlank("A")
        .addIfNotBlank("")
        .addIfNotBlank("   ")
        .addIfNotBlank(null)
        .addIfNotBlank("B");

    assertThat(joiner.toString()).isEqualTo("A, B");
  }

  @Test
  public void testAddIf_withCondition() {
    BetterStringJoiner joiner = new BetterStringJoiner(" ");
    joiner.addIf(true, "Added").addIf(false, "NotAdded");

    assertThat(joiner.toString()).isEqualTo("Added");
  }

  @Test
  public void testWithSpaceDelimiter() {
    BetterStringJoiner joiner = BetterStringJoiner.withSpaceDelimiter();
    joiner.add("Hello").add("World");

    assertThat(joiner.toString()).isEqualTo("Hello World");
  }

  @Test
  public void testWithNewlineDelimiter() {
    BetterStringJoiner joiner = BetterStringJoiner.withNewlineDelimiter();
    joiner.add("Hello").add("World");

    assertThat(joiner.toString()).isEqualTo("Hello\nWorld");
  }
}
