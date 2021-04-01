package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.Path;

public class ApplicantDataTest {

  @Test
  public void equality() {
    String testData = "{ \"applicant\": { \"testKey\": \"testValue\"}, \"metadata\": {}}";

    new EqualsTester()
        .addEqualityGroup(new ApplicantData(), new ApplicantData())
        .addEqualityGroup(new ApplicantData(testData), new ApplicantData(testData))
        .testEquals();
  }

  @Test
  public void preferredLocale_defaultsToEnglish() {
    ApplicantData data = new ApplicantData();
    assertThat(data.preferredLocale()).isEqualTo(Locale.US);
  }

  @Test
  public void hasPath_withRepeatedEntity() {
    ApplicantData data =
        new ApplicantData(
            "{\"applicant\":{\"children\":[{\"entity\":\"first child\", \"name\": {"
                + " \"first\":\"first\", \"last\": \"last\"}},{\"entity\": \"second child\"}]},"
                + "\"metadata\":{}}");

    Path path = Path.create("applicant.children[0].entity");
    assertThat(data.hasPath(path)).isTrue();

    path = Path.create("applicant.children[1]");
    assertThat(data.hasPath(path)).isTrue();
  }

  @Test
  public void hasPath_returnsTrueForExistingPath() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.school");
    data.putString(path, "Elementary School");

    assertThat(data.hasPath(path)).isTrue();
  }

  @Test
  public void hasPath_returnsTrueForArrayIndex() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.chores[0]");
    data.putString(path, "wash dishes");

    assertThat(data.hasPath(path)).isTrue();
  }

  @Test
  public void hasPath_returnsFalseForMissingPath() {
    ApplicantData data = new ApplicantData();

    assertThat(data.hasPath(Path.create("I_don't_exist!"))).isFalse();
  }

  @Test
  public void hasValueAtPath_returnsTrueIfValuePresent() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.horses");
    data.putLong(path, 278);

    assertThat(data.hasValueAtPath(path)).isTrue();
  }

  @Test
  public void hasValueAtPath_returnsTrueForArrayIndex() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.chores[0]");
    data.putString(path, "wash dishes");

    assertThat(data.hasValueAtPath(path)).isTrue();
  }

  @Test
  public void hasValueAtPath_returnsFalseForNull() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.horses");
    data.putLong(path, "");

    assertThat(data.hasValueAtPath(path)).isFalse();
  }

  @Test
  public void hasValueAtPath_returnsFalseForMissingPath() {
    ApplicantData data = new ApplicantData();

    assertThat(data.hasValueAtPath(Path.create("not_here!"))).isFalse();
  }

  @Test
  public void putScalarAtRoot() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("root"), "value");

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{},\"metadata\":{},\"root\":\"value\"}");
  }

  @Test
  public void putNestedPathAtRoot() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("new.path.at.root"), "hooray");

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{},\"metadata\":{},\"new\":{\"path\":{\"at\":{\"root\":\"hooray\"}}}}");
  }

  @Test
  public void putLong_addsAScalar() {
    ApplicantData data = new ApplicantData();

    data.putLong(Path.create("applicant.age"), 99);

    assertThat(data.asJsonString()).isEqualTo("{\"applicant\":{\"age\":99},\"metadata\":{}}");
  }

  @Test
  public void putString_addsANestedScalar() {
    ApplicantData data = new ApplicantData();
    String expected =
        "{\"applicant\":{\"favorites\":{\"food\":{\"apple\":\"Granny Smith\"}}},\"metadata\":{}}";

    data.putString(Path.create("applicant.favorites.food.apple"), "Granny Smith");

    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void putString_writesNullIfStringIsEmpty() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.name");
    String expected = "{\"applicant\":{\"name\":null},\"metadata\":{}}";

    data.putString(path, "");

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.readString(path)).isEmpty();
  }

  @Test
  public void putString_withFirstRepeatedEntity_putsParentLists() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("applicant.children[0].favorite_color.text"), "Orange");

    String expected =
        "{\"applicant\":{\"children\":[{\"favorite_color\":{\"text\":\"Orange\"}}]},\"metadata\":{}}";
    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  @Test
  public void putString_withSecondRepeatedEntity_addsIt() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("applicant.children[0].favorite_color.text"), "Orange");
    data.putString(Path.create("applicant.children[1].favorite_color.text"), "Brown");

    String expected =
        "{\"applicant\":{\"children\":[{\"favorite_color\":{\"text\":\"Orange\"}},{\"favorite_color\":{\"text\":\"Brown\"}}]},\"metadata\":{}}";
    assertThat(data.asJsonString()).isEqualTo(expected);
  }

  // TODO(#624): get rid of this recursion
  @Test
  public void putString_withNthRepeatedEntity_withoutFirstRepeatedEntity_isOK() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("applicant.children[2].favorite_color.text"), "Orange");

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"children\":[{},{},{\"favorite_color\":{\"text\":\"Orange\"}}]},\"metadata\":{}}");
  }

  @Test
  public void putString_addsFirstElementToArray() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("applicant.allergies[0]"), "peanut");

    assertThat(data.asJsonString())
        .isEqualTo("{\"applicant\":{\"allergies\":[\"peanut\"]},\"metadata\":{}}");
  }

  @Test
  public void putString_addsSeveralElementsToArray() {
    ApplicantData data = new ApplicantData();

    data.putString(Path.create("applicant.allergies[0]"), "peanut");
    data.putString(Path.create("applicant.allergies[1]"), "strawberry");
    data.putString(Path.create("applicant.allergies[2]"), "shellfish");

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"allergies\":[\"peanut\",\"strawberry\",\"shellfish\"]},\"metadata\":{}}");
  }

  @Test
  public void putLong_writesNullIfStringIsEmpty() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.age");
    String expected = "{\"applicant\":{\"age\":null},\"metadata\":{}}";

    data.putLong(path, "");

    assertThat(data.asJsonString()).isEqualTo(expected);
    assertThat(data.readLong(path)).isEmpty();
  }

  @Test
  public void putRepeatedEntities() {
    ApplicantData data = new ApplicantData();
    Path path = Path.create("applicant.children[1].pets[]");
    ImmutableList<String> petNames = ImmutableList.of("bubbles", "luna", "taco");

    data.putRepeatedEntities(path, petNames);

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"children\":[{},{\"pets\":[{\"entity_name\":\"bubbles\"},{\"entity_name\":\"luna\"},{\"entity_name\":\"taco\"}]}]},\"metadata\":{}}");
  }

  @Test
  public void putRepeatedEntities_withPrexistingData() {
    ApplicantData data =
        new ApplicantData(
            "{\"applicant\":{\"children\":[{},{\"entity_name\":\"an old name\",\"pets\":["
                + "{\"entity_name\":\"bubbles\"},"
                + "{\"entity_name\":\"luna\"},"
                + "{\"entity_name\":\"taco\"}]}]},\"metadata\":{}}");
    Path path = Path.create("applicant.children[]");
    ImmutableList<String> childrenNames = ImmutableList.of("alice", "bob");

    data.putRepeatedEntities(path, childrenNames);

    assertThat(data.asJsonString())
        .isEqualTo(
            "{\"applicant\":{\"children\":[{\"entity_name\":\"alice\"},{\"entity_name\":\"bob\",\"pets\":[{\"entity_name\":\"bubbles\"},{\"entity_name\":\"luna\"},{\"entity_name\":\"taco\"}]}]},\"metadata\":{}}");
  }

  @Test
  public void readString_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"favorites\": { \"color\": \"orange\"} } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString(Path.create("applicant.favorites.color"));

    assertThat(found).hasValue("orange");
  }

  @Test
  public void readString_withRepeatedEntity_findsCorrectValue() throws Exception {
    ApplicantData data =
        new ApplicantData(
            "{\"applicant\":{\"children\":["
                + "{\"entity\":\"first child\",\"name\":{\"first\":\"Billy\", \"last\": \"Bob\"}},"
                + "{\"entity\": \"second child\"}]},"
                + "\"metadata\":{}}");

    Optional<String> found = data.readString(Path.create("applicant.children[0].name.first"));

    assertThat(found).hasValue("Billy");
  }

  @Test
  public void readString_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<String> found = data.readString(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readString_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"number\": 27 } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString(Path.create("applicant.object"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readString_returnsEmptyForLists() {
    String testData = "{ \"applicant\": { \"list\":[\"hello\", \"world\"] } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<String> found = data.readString(Path.create("applicant.list"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLong_findsCorrectValue() throws Exception {
    String testData = "{ \"applicant\": { \"age\": 30 } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.age"));

    assertThat(found).hasValue(30L);
  }

  @Test
  public void readLong_pathNotPresent_returnsEmptyOptional() throws Exception {
    ApplicantData data = new ApplicantData();

    Optional<Long> found = data.readLong(Path.create("my.fake.path"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readLong_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"name\": \"John\" } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<Long> found = data.readLong(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readList_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[\"apple\",\"orange\"]}}";
    ApplicantData data = new ApplicantData(testData);

    Optional<ImmutableList<String>> found = data.readList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of("apple", "orange"));
  }

  @Test
  public void readListWithOneValue_findsCorrectValue() {
    String testData = "{\"applicant\":{\"favorite_fruits\":[\"apple\"]}}";
    ApplicantData data = new ApplicantData(testData);

    Optional<ImmutableList<String>> found = data.readList(Path.create("applicant.favorite_fruits"));

    assertThat(found).hasValue(ImmutableList.of("apple"));
  }

  @Test
  public void readList_pathNotPresent_returnsEmptyOptional() {
    ApplicantData data = new ApplicantData();

    Optional<ImmutableList<String>> found = data.readList(Path.create("not.here"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readList_returnsEmptyWhenTypeMismatch() {
    String testData = "{ \"applicant\": { \"object\": { \"age\": 12 } } }";
    ApplicantData data = new ApplicantData(testData);

    Optional<ImmutableList<String>> found = data.readList(Path.create("applicant.object.name"));

    assertThat(found).isEmpty();
  }

  @Test
  public void readAsString_readsAListAsAString() {
    ApplicantData data = new ApplicantData("{\"applicant\":{\"list\":[\"hello\",\"world\"]}}");

    assertThat(data.readAsString(Path.create("applicant.list"))).hasValue("[hello, world]");
  }

  @Test
  public void readAsString_readsNumberAsString() {
    ApplicantData data = new ApplicantData("{\"applicant\":{\"classes\":2}}");

    assertThat(data.readAsString(Path.create("applicant.classes"))).hasValue("2");
  }

  @Test
  public void readRepeatedEntities() {
    String testData =
        "{\"applicant\":{\"children\":[{},{\"pets\":["
            + "{\"entity_name\":\"bubbles\"},"
            + "{\"entity_name\":\"luna\"},"
            + "{\"entity_name\":\"taco\"}"
            + "]}]},\"metadata\":{}}";
    ApplicantData data = new ApplicantData(testData);
    Path path = Path.create("applicant.children[1].pets[]");

    ImmutableList<String> found = data.readRepeatedEntities(path);

    assertThat(found).containsExactly("bubbles", "luna", "taco");
  }
}
