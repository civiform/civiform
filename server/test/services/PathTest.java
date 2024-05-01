package services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import services.applicant.question.Scalar;
import services.export.enums.ApiPathSegment;

public class PathTest {

  @Test
  public void pathWithPrefix_removesOnlyPrefixAndWhitespace() {
    String path = "favorite.color";
    assertThat(Path.create("$.applicant." + path).toString()).isEqualTo("applicant.favorite.color");
    assertThat(Path.create(path).toString()).isEqualTo(path);
    assertThat(Path.create("      applicant.hello   ").toString()).isEqualTo("applicant.hello");
    assertThat(Path.create("    $.applicant  ").toString()).isEqualTo("applicant");
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
  public void replacingLastSegment() {
    Path path = Path.create("one.two.three.me");
    assertThat(path.replacingLastSegment("you")).isEqualTo(Path.create("one.two.three.you"));
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
  public void asApplicationPath_firstSegmentIsApplicant() {
    Path path = Path.create("applicant.two.three");
    assertThat(path.asApplicationPath()).isEqualTo(Path.create("application.two.three"));
  }

  @Test
  public void asNestedEntitiesPath_noArrayElements() {
    Path path = Path.create("one.two.three");
    assertThat(path.asNestedEntitiesPath()).isEqualTo(Path.create("one.two.three"));
  }

  @Test
  public void asNestedEntitiesPath_firstSegmentArrayElement() {
    Path path = Path.create("one[2].three.four");
    assertThat(path.asNestedEntitiesPath()).isEqualTo(Path.create("one.entities[2].three.four"));
  }

  @Test
  public void asNestedEntitiesPath_lastSegmentArrayElement() {
    Path path = Path.create("one.two.three[4]");
    assertThat(path.asNestedEntitiesPath()).isEqualTo(Path.create("one.two.three.entities[4]"));
  }

  @Test
  public void asNestedEntitiesPath_middleSegmentArrayElement() {
    Path path = Path.create("one.two[3].four");
    assertThat(path.asNestedEntitiesPath()).isEqualTo(Path.create("one.two.entities[3].four"));
  }

  @Test
  public void asNestedEntitiesPath_multipleSegmentArrayElement() {
    Path path = Path.create("one.two[3].four[5].six");
    assertThat(path.asNestedEntitiesPath())
        .isEqualTo(Path.create("one.two.entities[3].four.entities[5].six"));
  }

  @Test
  public void asNestedEntitiesPath_isIdempotent() {
    Path path = Path.create("one.two.entities[3]");
    assertThat(path.asNestedEntitiesPath()).isEqualTo(Path.create("one.two.entities[3]"));
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
  public void asArrayElement() {
    Path path = Path.create("one.two[3].four").asArrayElement();

    assertThat(path.isArrayElement()).isTrue();
    assertThat(path.toString()).isEqualTo("one.two[3].four[]");
  }

  @Test
  public void asArrayElement_alreadyAnArrayElement() {
    Path path = Path.create("one.two[3].four[5]").asArrayElement();

    assertThat(path.isArrayElement()).isTrue();
    assertThat(path.toString()).isEqualTo("one.two[3].four[5]");
  }

  @Test
  public void withoutArrayReference() {
    Path path = Path.create("one.two[3]");
    assertThat(path.withoutArrayReference()).isEqualTo(Path.create("one.two"));
  }

  @Test
  public void withoutArrayReference_forNonIndexedArrayPath() {
    Path path = Path.create("one.two[]");
    assertThat(path.withoutArrayReference()).isEqualTo(Path.create("one.two"));
  }

  @Test
  public void safeWithoutArrayReference() {
    Path path = Path.create("one.two[3]");
    assertThat(path.safeWithoutArrayReference()).isEqualTo(Path.create("one.two"));
  }

  @Test
  public void safeWithoutArrayReference_withNoArrayReference() {
    Path path = Path.create("one.two");
    assertThat(path.safeWithoutArrayReference()).isEqualTo(Path.create("one.two"));
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
  public void atIndex_forNonIndexedArrayPath() {
    Path path = Path.create("one.two[]");

    Path expected = Path.create("one.two[55]");
    assertThat(path.atIndex(55)).isEqualTo(expected);
  }

  @Test
  public void join() {
    Path path = Path.create("applicant.my.path");
    assertThat(path.toString()).isEqualTo("applicant.my.path");

    path = path.join("another");
    assertThat(path.toString()).isEqualTo("applicant.my.path.another");
  }

  @Test
  public void join_withMultipleSegments_parentPathWorks() {
    Path path = Path.create("one");
    Path path2 = path.join("two.three.four");
    Path actual = path2.parentPath();

    Path expected = Path.create("one.two.three");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void join_withScalarEnum() {
    Path path = Path.create("start").join(Scalar.FIRST_NAME);

    assertThat(path).isEqualTo(Path.create("start.first_name"));
  }

  @Test
  public void join_withApiPathSegmentEnum() {
    Path path = Path.create("start").join(ApiPathSegment.QUESTION_TYPE);
    assertThat(path).isEqualTo(Path.create("start.question_type"));
  }

  @Test
  public void join_withPath() {
    Path path = Path.create("start").join(Path.create("middle.end"));
    assertThat(path).isEqualTo(Path.create("start.middle.end"));
  }

  @Test
  public void startsWith_isTrue() {
    assertThat(Path.create("a.b.c").startsWith(Path.create("a.b"))).isTrue();
  }

  @Test
  public void startsWith_ignoresArrays_isTrue() {
    assertThat(Path.create("a.b[].c[].d[]").startsWith(Path.create("a.b.c"))).isTrue();
    assertThat(Path.create("a.b[].c[].d[]").startsWith(Path.create("a.b.c.d"))).isTrue();
  }

  @Test
  public void startsWith_otherStartsWithThis_isFalse() {
    Path path = Path.create("a.b.c");
    Path other = Path.create("a.b.c.d");

    assertThat(path.startsWith(other)).isFalse();
    assertThat(other.startsWith(path)).isTrue();
  }

  @Test
  public void startsWith_stringStartsWithButPathSegmentsDont_isFalse() {
    Path path = Path.create("a.b.c_d");
    Path other = Path.create("a.b.c");

    assertThat(path.startsWith(other)).isFalse();
  }
}
