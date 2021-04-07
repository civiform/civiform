package services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PathTest {

  @Test
  public void pathWithPrefix_removesOnlyPrefixAndWhitespace() {
    String path = "favorite.color";
    assertThat(Path.create("$.applicant." + path).path()).isEqualTo("applicant.favorite.color");
    assertThat(Path.create(path).path()).isEqualTo(path);
    assertThat(Path.create("      applicant.hello   ").path()).isEqualTo("applicant.hello");
    assertThat(Path.create("    $.applicant  ").path()).isEqualTo("applicant");
  }

  @Test
  public void segments_emptyPath() {
    assertThat(Path.empty().segments()).isEmpty();
  }

  @Test
  public void segments_oneSegment() {
    assertThat(Path.create("applicant").segments()).containsExactly("applicant");
  }

  @Test
  public void segments() {
    Path path = Path.create("my.super.long.path");
    assertThat(path.segments()).containsExactly("my", "super", "long", "path");
  }

  @Test
  public void createPathFromString_returnsEmptyPathIfStringIsEmpty() {
    assertThat(Path.create("")).isEqualTo(Path.empty());
  }

  @Test
  public void create_emptyPath_isEmpty() {
    assertThat(Path.empty().isEmpty()).isTrue();
  }

  @Test
  public void test_toString() {
    assertThat(Path.create("a.b.c[3].d").toString()).isEqualTo("a.b.c[3].d");
  }

  @Test
  public void toString_empty_hasSuffix() {
    assertThat(Path.empty().toString()).isEqualTo("$");
  }

  @Test
  public void parentPath_emptyPath() {
    Path path = Path.empty();
    assertThat(path.parentPath()).isEqualTo(Path.create(""));
  }

  @Test
  public void parentPath_oneSegment() {
    Path path = Path.create("applicant");
    assertThat(path.parentPath()).isEqualTo(Path.create(""));
  }

  @Test
  public void parentPath() {
    Path path = Path.create("one.two.three.me");
    assertThat(path.parentPath()).isEqualTo(Path.create("one.two.three"));
  }

  @Test
  public void keyName_emptyPath() {
    Path path = Path.empty();
    assertThat(path.keyName()).isEqualTo("");
  }

  @Test
  public void keyName_oneSegment() {
    Path path = Path.create("applicant");
    assertThat(path.keyName()).isEqualTo("applicant");
  }

  @Test
  public void keyName_arrayElement() {
    Path path = Path.create("applicant.children[3]");
    assertThat(path.keyName()).isEqualTo("children[3]");
  }

  @Test
  public void keyName() {
    Path path = Path.create("one.two.name");
    assertThat(path.keyName()).isEqualTo("name");
  }

  @Test
  public void isArrayElement() {
    assertThat(Path.create("one.two[3]").isArrayElement()).isTrue();
  }

  @Test
  public void isArrayElement_notArrayElement() {
    assertThat(Path.create("one.two").isArrayElement()).isFalse();
  }

  @Test
  public void withoutArrayReference() {
    Path path = Path.create("one.two[3]");
    assertThat(path.withoutArrayReference()).isEqualTo(Path.create("one.two"));
  }

  @Test
  public void arrayIndex() {
    Path path = Path.create("one.two[33]");
    assertThat(path.arrayIndex()).isEqualTo(33);
  }

  @Test
  public void atIndex() {
    Path path = Path.create("one.two[33]");

    Path expected = Path.create("one.two[55]");
    assertThat(path.atIndex(55)).isEqualTo(expected);
  }

  @Test
  public void pathJoin() {
    Path path = Path.create("applicant.my.path");
    assertThat(path.path()).isEqualTo("applicant.my.path");

    path = path.join("another");
    assertThat(path.path()).isEqualTo("applicant.my.path.another");
  }

  @Test
  public void pathJoin_withMultipleSegments_parentPathWorks() {
    Path path = Path.create("one");
    Path path2 = path.join("two.three.four");
    Path actual = path2.parentPath();

    Path expected = Path.create("one.two.three");
    assertThat(actual).isEqualTo(expected);
  }
}
