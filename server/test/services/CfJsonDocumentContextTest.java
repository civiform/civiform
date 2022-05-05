package services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.util.Optional;
import org.junit.Test;
import services.applicant.predicate.JsonPathPredicate;

public class CfJsonDocumentContextTest {

  @Test
  public void equality() {
    String testData = "{ \"applicant\": { \"testKey\": \"testValue\"} }";

    new EqualsTester()
        .addEqualityGroup(new CfJsonDocumentContext(), new CfJsonDocumentContext())
        .addEqualityGroup(new CfJsonDocumentContext(testData), new CfJsonDocumentContext(testData))
        .testEquals();
  }

  @Test
  public void overwriteDataAtSamePath_succeeds() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.target");

    data.putString(path, "hello");
    assertThat(data.readString(path)).hasValue("hello");

    data.putString(path, "world");
    assertThat(data.readString(path)).hasValue("world");
  }

  @Test
  public void hasPath_withRepeatedEntity() {
    CfJsonDocumentContext data =
        new CfJsonDocumentContext(
            "{\"applicant\":{\"children\":[{\"entity\":\"first child\", \"name\": {"
                + " \"first\":\"first\", \"last\": \"last\"}},{\"entity\": \"second child\"}]}}");

    Path path = Path.create("applicant.children[0].entity");
    assertThat(data.hasPath(path)).isTrue();

    path = Path.create("applicant.children[1]");
    assertThat(data.hasPath(path)).isTrue();
  }

  @Test
  public void hasPath_returnsTrueForExistingPath() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.school");
    data.putString(path, "Elementary School");

    assertThat(data.hasPath(path)).isTrue();
  }

  @Test
  public void hasPath_returnsTrueForArrayIndex() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.chores[0]");
    data.putString(path, "wash dishes");

    assertThat(data.hasPath(path)).isTrue();
  }

  @Test
  public void hasPath_returnsFalseForMissingPath() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    assertThat(data.hasPath(Path.create("I_don't_exist!"))).isFalse();
  }

  @Test
  public void hasValueAtPath_returnsTrueIfValuePresent() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.horses");
    data.putLong(path, 278);

    assertThat(data.hasValueAtPath(path)).isTrue();
  }

  @Test
  public void hasValueAtPath_returnsTrueForArrayIndex() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.chores[0]");
    data.putString(path, "wash dishes");

    assertThat(data.hasValueAtPath(path)).isTrue();
  }

  @Test
  public void hasValueAtPath_returnsFalseForNull() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.horses");
    data.putLong(path, "");

    assertThat(data.hasValueAtPath(path)).isFalse();
  }

  @Test
  public void hasValueAtPath_returnsFalseForMissingPath() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    assertThat(data.hasValueAtPath(Path.create("not_here!"))).isFalse();
  }

  @Test
  public void putScalarAtRoot() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("root"), "value");

    assertThat(data.asJsonString()).isEqualTo("{\"root\":\"value\"}");
  }

  @Test
  public void putNestedPathAtRoot() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("new.path.at.root"), "hooray");

    assertThat(data.asJsonString())
        .isEqualTo("{\"new\":{\"path\":{\"at\":{\"root\":\"hooray\"}}}}");
  }

  @Test
  public void putLong_addsAScalar() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putLong(Path.create("applicant.age"), 99);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"age\":99}}");
  }

  @Test
  public void putDouble_addsAScalar() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putDouble(Path.create("applicant.monthly_income"), 99.9);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"monthly_income\":99.9}}");
  }

  @Test
  public void putString_addsANestedScalar() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    String expected = "{\"applicant\":{\"favorites\":{\"food\":{\"apple\":\"Granny Smith\"}}}}";

    data.putString(Path.create("applicant.favorites.food.apple"), "Granny Smith");

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void putString_writesNullIfStringIsEmpty() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.name");
    String expected = "{\"applicant\":{\"name\":null}}";

    data.putString(path, "");

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.readString(path)).isEmpty();
  }

  @Test
  public void putString_withFirstRepeatedEntity_putsParentLists() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("applicant.children[0].favorite_color.text"), "Orange");

    String expected = "{\"applicant\":{\"children\":[{\"favorite_color\":{\"text\":\"Orange\"}}]}}";
    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void putString_withSecondRepeatedEntity_addsIt() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("applicant.children[0].favorite_color.text"), "Orange");
    data.putString(Path.create("applicant.children[1].favorite_color.text"), "Brown");

    String expected =
        "{\"applicant\":{\"children\":[{\"favorite_color\":{\"text\":\"Orange\"}},{\"favorite_color\":{\"text\":\"Brown\"}}]}}";
    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void putString_withNthRepeatedEntity_withoutFirstRepeatedEntity_isOK() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("applicant.children[2].favorite_color.text"), "Orange");

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"children\":[{},{},{\"favorite_color\":{\"text\":\"Orange\"}}]}}");
  }

  @Test
  public void putString_addsFirstElementToArray() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("applicant.allergies[0]"), "peanut");

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"allergies\":[\"peanut\"]}}");
  }

  @Test
  public void putString_addsSeveralElementsToArray() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("applicant.allergies[0]"), "peanut");
    data.putString(Path.create("applicant.allergies[1]"), "strawberry");
    data.putString(Path.create("applicant.allergies[2]"), "shellfish");

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"allergies\":[\"peanut\",\"strawberry\",\"shellfish\"]}}");
  }

  @Test
  public void putString_addsSeveralElementsToArray_withReusedIndex() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putString(Path.create("applicant.allergies[0]"), "peanut");
    data.putString(Path.create("applicant.allergies[0]"), "strawberry");
    data.putString(Path.create("applicant.allergies[0]"), "shellfish");

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"allergies\":[\"peanut\",\"strawberry\",\"shellfish\"]}}");
  }

  @Test
  public void putLong_writesNullIfStringIsEmpty() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.age");
    String expected = "{\"applicant\":{\"age\":null}}";

    data.putLong(path, "");

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.readLong(path)).isEmpty();
  }

  @Test
  public void putArray_writesAnArray() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.kitchen_tools");
    String expected = "{\"applicant\":{\"kitchen_tools\":[\"pepper grinder\",\"salt shaker\"]}}";

    data.putArray(path, ImmutableList.of("pepper grinder", "salt shaker"));

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.readLong(path)).isEmpty();
  }

  @Test
  public void putRepeatedEntities() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.children[1].pets[]");
    ImmutableList<String> petNames = ImmutableList.of("bubbles", "luna", "taco");

    data.putRepeatedEntities(path, petNames);

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"children\":[{},{\"pets\":[{\"entity_name\":\"bubbles\"},{\"entity_name\":\"luna\"},{\"entity_name\":\"taco\"}]}]}}");
  }

  @Test
  public void putRepeatedEntities_withPrexistingData() {
    CfJsonDocumentContext data =
        new CfJsonDocumentContext(
            "{\"applicant\":{\"children\":[{},{\"entity_name\":\"an old name\",\"pets\":["
                + "{\"entity_name\":\"bubbles\"},"
                + "{\"entity_name\":\"luna\"},"
                + "{\"entity_name\":\"taco\"}]}]}}");
    Path path = Path.create("applicant.children[]");
    ImmutableList<String> childrenNames = ImmutableList.of("alice", "bob");

    data.putRepeatedEntities(path, childrenNames);

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"children\":[{\"entity_name\":\"alice\"},{\"entity_name\":\"bob\",\"pets\":[{\"entity_name\":\"bubbles\"},{\"entity_name\":\"luna\"},{\"entity_name\":\"taco\"}]}]}}");
  }

  @Test
  public void readString_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<String> found = data.readString(Path.create("applicant.favorites.color"));

    assertThat(found).hasValue("orange");
  }

  @Test
  public void readString_withRepeatedEntity_findsCorrectValue() throws Exception {
    CfJsonDocumentContext data =
        new CfJsonDocumentContext(
            "{\"applicant\":{\"children\":["
                + "{\"entity\":\"first child\",\"name\":{\"first\":\"Billy\", \"last\": \"Bob\"}},"
                + "{\"entity\": \"second child\"}]}}");

    Optional<String> found = data.readString(Path.create("applicant.children[0].name.first"));

    assertThat(found).hasValue("Billy");
  }

  @Test
  public void readString_pathNotPresent_returnsEmptyOptional() throws Exception {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    Optional<String> found = data.readString(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readString_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"number\": 27 } } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<String> found = data.readString(Path.create("applicant.object"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readString_returnsEmptyForLists() {
    String testData = "{ \"applicant\": { \"list\":[\"hello\", \"world\"] } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<String> found = data.readString(Path.create("applicant.list"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLong_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"age\": 30 } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.age"));

    assertThat(found).hasValue(30L);
  }

  @Test
  public void readDouble_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"monthly_income\": 99.9 } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<Double> found = data.readDouble(Path.create("applicant.monthly_income"));

    assertThat(found).hasValue(99.9);
  }

  @Test
  public void readLong_pathNotPresent_returnsEmptyOptional() throws Exception {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    Optional<Long> found = data.readLong(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLong_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"name\": \"John\" } } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readList_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[1, 2]}}";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<Long>> found = data.readList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of(1L, 2L));
  }

  @Test
  public void readListWithOneValue_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[1]}}";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<Long>> found = data.readList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of(1L));
  }

  @Test
  public void readList_pathNotPresent_returnsEmptyOptional() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    Optional<ImmutableList<Long>> found = data.readList(Path.create("not.here"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readList_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"age\": 12 } } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<Long>> found = data.readList(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readAsString_readsAListAsAString() {
    CfJsonDocumentContext data = new CfJsonDocumentContext("{\"applicant\":{\"list\":[1, 2]}}");

    assertThat(data.readAsString(Path.create("applicant.list"))).hasValue("[1, 2]");
  }

  @Test
  public void readAsString_readsNumberAsString() {
    CfJsonDocumentContext data = new CfJsonDocumentContext("{\"applicant\":{\"classes\":2}}");

    assertThat(data.readAsString(Path.create("applicant.classes"))).hasValue("2");
  }

  @Test
  public void readRepeatedEntities() {
    String testData =
        "{\"applicant\":{\"children\":[{},{\"pets\":["
            + "{\"entity_name\":\"bubbles\"},"
            + "{\"entity_name\":\"luna\"},"
            + "{\"entity_name\":\"taco\"}]}]}}";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);
    Path path = Path.create("applicant.children[1].pets[]");

    ImmutableList<String> found = data.readRepeatedEntities(path);

    assertThat(found).containsExactly("bubbles", "luna", "taco");
  }

  @Test
  public void deleteRepeatedEntities_indexTooBig_doesNotDeleteAnything() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("entities[]");
    data.putRepeatedEntities(path, ImmutableList.of("a", "b", "c"));

    assertThat(data.deleteRepeatedEntities(path, ImmutableList.of(1, 2, 3, 4, 5))).isFalse();

    ImmutableList<String> remaining = data.readRepeatedEntities(path);
    assertThat(remaining).containsExactly("a", "b", "c");
  }

  @Test
  public void deleteRepeatedEntities() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("entities[]");
    data.putRepeatedEntities(path, ImmutableList.of("a", "b", "c", "d", "e"));

    assertThat(data.deleteRepeatedEntities(path, ImmutableList.of(0, 2, 4))).isTrue();

    ImmutableList<String> remaining = data.readRepeatedEntities(path);
    assertThat(remaining).containsExactly("b", "d");
  }

  @Test
  public void locked_makesCfJsonDocumentContextImmutable() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    // Can mutate before CfJsonDocumentContext is locked.
    data.putString(Path.create("applicant.planet"), "Earth");

    data.lock();

    // Cannot mutate after CfJsonDocumentContext is locked.
    assertThatThrownBy(() -> data.putString(Path.create("applicant.planets[1].planetName"), "Mars"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Cannot change CfJsonDocumentContext after it has been locked.");
    assertThatThrownBy(() -> data.putLong(Path.create("applicant.planets[3].planetSize"), 5L))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Cannot change CfJsonDocumentContext after it has been locked.");
    assertThatThrownBy(() -> data.putLong(Path.create("applicant.planets[3].planetSize"), "5"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Cannot change CfJsonDocumentContext after it has been locked.");
    assertThatThrownBy(
            () ->
                data.putRepeatedEntities(
                    Path.create("applicant.planets[]"), ImmutableList.of("earth", "mars")))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Cannot change CfJsonDocumentContext after it has been locked.");
  }

  @Test
  public void clearArray() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    data.putString(Path.create("applicant.things[0]"), "dog");
    data.putString(Path.create("applicant.things[1]"), "cat");
    data.putString(Path.create("applicant.things[2]"), "horse");
    data.putString(Path.create("applicant.stuff"), "cars");

    String expected = "{\"applicant\":{\"things\":[\"dog\",\"cat\",\"horse\"],\"stuff\":\"cars\"}}";
    assertThat(data.asJsonString()).isEqualTo(expected);

    data.maybeClearArray(Path.create("applicant.things[0]"));

    String nextExpected = "{\"applicant\":{\"stuff\":\"cars\"}}";
    assertThat(data.asJsonString()).isEqualTo(nextExpected);
  }

  @Test
  public void clearArray_ignoresScalar() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    data.putString(Path.create("applicant.things[0]"), "dog");
    data.putString(Path.create("applicant.things[1]"), "cat");
    data.putString(Path.create("applicant.things[2]"), "horse");
    data.putString(Path.create("applicant.stuff"), "cars");

    String expected = "{\"applicant\":{\"things\":[\"dog\",\"cat\",\"horse\"],\"stuff\":\"cars\"}}";
    assertThat(data.asJsonString()).isEqualTo(expected);

    data.maybeClearArray(Path.create("applicant.cars"));
    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void evalPredicate_pathDoesNotExist() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    JsonPathPredicate predicate = JsonPathPredicate.create("$.applicant.things[0][?(@.one)]");

    assertThat(data.evalPredicate(predicate)).isFalse();
  }

  @Test
  public void evalPredicate_stringComparison() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    data.putString(Path.create("applicant.one"), "test");
    data.putString(Path.create("applicant.two[0].three"), "three");

    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one == \"test\")]")))
        .isTrue();
    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one == \"fail\")]")))
        .isFalse();
    assertThat(
            data.evalPredicate(
                JsonPathPredicate.create("$.applicant.two[0][?(@.three == \"three\")]")))
        .isTrue();
    assertThat(
            data.evalPredicate(
                JsonPathPredicate.create("$.applicant.two[1][?(@.three == \"other\")]")))
        .isFalse();
  }

  @Test
  public void evalPredicate_numberComparison() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    data.putLong(Path.create("applicant.one"), 1L);
    data.putLong(Path.create("applicant.two"), 2L);

    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one > 0)]"))).isTrue();
    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one < 2)]"))).isTrue();
    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one == 1)]"))).isTrue();
    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one == 2)]")))
        .isFalse();
    assertThat(
            data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one < $.applicant.two)]")))
        .isTrue();
    assertThat(
            data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one > $.applicant.two)]")))
        .isFalse();
  }

  @Test
  public void evalPredicate_anyOf() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    data.putLong(Path.create("applicant.one[0]"), 2L);

    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one anyof [1, 2])]")))
        .isTrue();
    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one anyof [3, 4])]")))
        .isFalse();
  }

  @Test
  public void evalPredicate_in() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    data.putString(Path.create("applicant.one"), "test");

    assertThat(
            data.evalPredicate(
                JsonPathPredicate.create("$.applicant[?(@.one in [\"test\", \"other\"])]")))
        .isTrue();
    assertThat(data.evalPredicate(JsonPathPredicate.create("$.applicant[?(@.one in [\"other\"])]")))
        .isFalse();
  }
}
