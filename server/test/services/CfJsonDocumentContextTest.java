package services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import com.jayway.jsonpath.PathNotFoundException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.applicant.predicate.JsonPathPredicate;

@RunWith(JUnitParamsRunner.class)
public class CfJsonDocumentContextTest {

  private static final ZoneId BEHIND_UTC_ZONE = ZoneId.of("America/New_York");

  private static TimeZone origTimeZone;

  @BeforeClass
  public static void setTimeZoneToNonAmerica() {
    origTimeZone = TimeZone.getDefault();
    // Set the default time zone to something behind UTC. We rely on this for
    // the date serialization / deserialization test below, where we expect
    // everything to occur in terms of UTC rather than the system default
    // time zone.
    TimeZone.setDefault(TimeZone.getTimeZone(BEHIND_UTC_ZONE));
  }

  @AfterClass
  public static void resetTimeZone() {
    // Reset the time zone to whatever the system would resolve it to.
    TimeZone.setDefault(origTimeZone);
  }

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

    assertThat(data.hasValueAtPath(Path.create("not_here"))).isFalse();
  }

  @Test
  public void hasNullValueAtPath_returnsTrueForNull() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.horses");
    data.putLong(path, "");

    assertThat(data.hasNullValueAtPath(path)).isTrue();
  }

  @Test
  public void hasNullValueAtPath_returnsFalseForValue() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.horses");
    data.putLong(path, 15);

    assertThat(data.hasNullValueAtPath(path)).isFalse();
  }

  @Test
  public void hasNullValueAtPath_throwsForMissingPath() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    assertThatThrownBy(() -> data.hasNullValueAtPath(Path.create("not_here")))
        .isInstanceOf(PathNotFoundException.class);
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
  public void putPhoneNumber_AddsAPhoneNumberWithoutFormat() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    data.putPhoneNumber(Path.create("applicant.phone_number"), "(707) -123-1234");

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"phone_number\":\"7071231234\"}}");
  }

  @Test
  @Parameters({"123123", "1122233334445556666", "123456789", "(707)-123-455"})
  public void putPhoneNumber_AddingInvalidNumberResultsInEmptyPath(String phoneNumber) {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    assertThatThrownBy(
            () -> data.putPhoneNumber(Path.create("applicant.phone_number"), phoneNumber))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            String.format("Invalid phone number format: %s", phoneNumber.replaceAll("\\d", "X")));
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
  public void putNull_putsNullAtSpecifiedPath() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.age");
    String expected = "{\"applicant\":{\"age\":null}}";

    data.putNull(path);

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.hasNullValueAtPath(path)).isTrue();
  }

  @Test
  public void putNull_isNoopIfPathIsArrayElement() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();
    Path path = Path.create("applicant.allergies[0]");
    String expected = "{\"applicant\":{\"allergies\":[\"peanut\",\"kiwi\"]}}";

    data.putString(Path.create("applicant.allergies[0]"), "peanut");
    data.putString(Path.create("applicant.allergies[1]"), "kiwi");

    data.putNull(path);

    assertThat(data.asJsonString()).isEqualTo(expected);
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
  public void readLongList_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[1, 2]}}";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<Long>> found =
        data.readLongList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of(1L, 2L));
  }

  @Test
  public void readLongList_withOneValue_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[1]}}";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<Long>> found =
        data.readLongList(Path.create("applicant.favorite_fruits"));
    assertThat(found).hasValue(ImmutableList.of(1L));
  }

  @Test
  public void readLongList_pathNotPresent_returnsEmptyOptional() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    Optional<ImmutableList<Long>> found = data.readLongList(Path.create("not.here"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLongList_withTypeMismatch_returnsEmptyOptional() {
    String testData = "{ \"applicant\": { \"object\": { \"name\": \"Khalid\" } } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<Long>> found = data.readLongList(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readStringList_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[\"apple\", \"orange\"]}}";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<String>> found =
        data.readStringList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of("apple", "orange"));
  }

  @Test
  public void readStringList_withOneValue_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[\"apple\"]}}";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<String>> found =
        data.readStringList(Path.create("applicant.favorite_fruits"));
    assertThat(found).hasValue(ImmutableList.of("apple"));
  }

  @Test
  public void readStringList_pathNotPresent_returnsEmptyOptional() {
    CfJsonDocumentContext data = new CfJsonDocumentContext();

    Optional<ImmutableList<String>> found = data.readStringList(Path.create("not.here"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readStringList_withTypeMismatch_returnsEmptyOptional() {
    String testData = "{ \"applicant\": { \"object\": { \"age\": 12 } } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    Optional<ImmutableList<String>> found =
        data.readStringList(Path.create("applicant.object.name"));

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
  public void asPrettyJsonString_prettyPrintsDocumentAtPath() {
    String testData =
        "{ \"deeply\": { \"nested\": { \"value\": \"long text to stop formatter de-wrapping\" } }"
            + " }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    String prettyJson = data.asPrettyJsonString(Path.create("$.deeply"));

    assertThat(prettyJson)
        .isEqualTo(
            "{\n"
                + "  \"nested\" : {\n"
                + "    \"value\" : \"long text to stop formatter de-wrapping\"\n"
                + "  }\n"
                + "}");
  }

  @Test
  public void asPrettyJsonString_prettyPrintsNullString() {
    String testData = "{ \"deeply\": { \"nested\": { \"age\": \"null\" } } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    String prettyJson = data.asPrettyJsonString(Path.create("$.deeply.nested.age"));

    // If the leaf node is the String "null", then pretty-print it as a JSON string.
    assertThat(prettyJson).isEqualTo("\"null\"");
  }

  @Test
  public void asPrettyJsonString_prettyPrintsNullValue() {
    String testData = "{ \"deeply\": { \"nested\": { \"age\": null } } }";
    CfJsonDocumentContext data = new CfJsonDocumentContext(testData);

    String prettyJson = data.asPrettyJsonString(Path.create("$.deeply.nested.age"));

    // If the leaf node is the value "null", then pretty-print it as the null value.
    assertThat(prettyJson).isEqualTo("null");
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

  @Test
  public void dateSerializationRoundTrips() {
    // This is a regression test for Issue #2342, where dates were serialized
    // at the start of the day in UTC, but were deserialized in the system default
    // time zone. As such, if the system default time zone were behind UTC, it would
    // be serialized as 2022-01-02 and deserialized as 2022-01-01.
    // To ensure this tests the correct behavior, ensure the system default time zone
    // is behind UTC.
    assertThat(TimeZone.getDefault().getRawOffset()).isLessThan(0);

    CfJsonDocumentContext data = new CfJsonDocumentContext();
    data.putDate(Path.create("applicant.date"), "2022-01-02");

    Optional<LocalDate> result = data.readDate(Path.create("applicant.date"));
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().toString()).isEqualTo("2022-01-02");
  }
}
