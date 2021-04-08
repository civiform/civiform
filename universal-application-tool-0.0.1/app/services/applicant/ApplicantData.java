package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.Path;
import services.WellKnownPaths;
import services.question.RepeaterQuestionDefinition;

public class ApplicantData {
  private static final String EMPTY_APPLICANT_DATA_JSON = "{ \"applicant\": {}, \"metadata\": {} }";
  private static final Locale DEFAULT_LOCALE = Locale.US;
  private static final TypeRef<ImmutableList<String>> IMMUTABLE_LIST_STRING_TYPE =
      new TypeRef<>() {};

  private Locale preferredLocale;
  private final DocumentContext jsonData;

  public ApplicantData() {
    this(EMPTY_APPLICANT_DATA_JSON);
  }

  public ApplicantData(String jsonData) {
    this(DEFAULT_LOCALE, jsonData);
  }

  public ApplicantData(Locale preferredLocale, String jsonData) {
    this.preferredLocale = preferredLocale;
    this.jsonData = JsonPathProvider.getJsonPath().parse(checkNotNull(jsonData));
  }

  public Locale preferredLocale() {
    return this.preferredLocale;
  }

  public void setPreferredLocale(Locale locale) {
    this.preferredLocale = locale;
  }

  /**
   * Checks whether the given path exists in the JSON data. Returns true if the path is present;
   * false otherwise. Semantically, this checks whether the applicant has answered this question
   * before.
   *
   * @param path the {@link Path} to check
   * @return true if path is present for this applicant; false otherwise
   */
  public boolean hasPath(Path path) {
    try {
      this.jsonData.read(path.toString());
    } catch (PathNotFoundException e) {
      return false;
    }
    return true;
  }

  /**
   * Returns true if there is a non-null value at the given {@link Path}; false otherwise. Will
   * return false if there is a null value at the path.
   *
   * @param path the {@link Path} to check
   * @return true if there is a non-null value at the given path; false otherwise
   */
  public boolean hasValueAtPath(Path path) {
    try {
      return read(path, Object.class).isPresent();
    } catch (JsonPathTypeMismatchException e) {
      return false;
    }
  }

  /**
   * Write the given string at the given {@link Path}. If the string is empty, it will write a null
   * value instead.
   */
  public void putString(Path path, String value) {
    if (value.isEmpty()) {
      putNull(path);
    } else {
      put(path, value);
    }
  }

  public void putLong(Path path, long value) {
    put(path, value);
  }

  /**
   * Parses and writes a long value, given as a string. If the string is empty, a null value is
   * written.
   */
  public void putLong(Path path, String value) {
    if (value.isEmpty()) {
      putNull(path);
    } else {
      put(path, Long.parseLong(value));
    }
  }

  /**
   * Puts the names of the repeated entities at the path. Each element in the JSON array at the path
   * is a JSON object that has at minimum a property {@link
   * RepeaterQuestionDefinition#REPEATED_ENTITY_NAME_KEY} that contains a string value, along with
   * possibly other nested answers to questions or repeated entities.
   *
   * <p>This should not affect any other data that may already exist for the repeated entities.
   *
   * @param path a path to repeated entities list
   * @param entityNames the names of repeated entities
   */
  public void putRepeatedEntities(Path path, ImmutableList<String> entityNames) {
    if (entityNames.isEmpty()) {
      put(path, ImmutableList.of());
    } else {
      for (int i = 0; i < entityNames.size(); i++) {
        putString(
            path.atIndex(i).join(RepeaterQuestionDefinition.REPEATED_ENTITY_NAME_KEY),
            entityNames.get(i));
      }
    }
  }

  private void putNull(Path path) {
    put(path, null);
  }

  /**
   * Puts the given value at the given path in the underlying JSON data. Builds up the necessary
   * structure along the way, i.e., creates parent objects where necessary.
   *
   * <p>If the path ends in an array (i.e. we are trying to add an element to a JSON array), this
   * will check to make sure the array is there, then add the given element to the end of the array.
   *
   * @param path the {@link Path} with the fully specified path, e.g.,
   *     "applicant.children[3].favorite_color.text" or the equivalent
   *     "$.applicant.children[3].favorite_color.text".
   * @param value the value to place; values of type Map will create the equivalent JSON structure
   */
  private void put(Path path, Object value) {
    putParentIfMissing(path);
    if (path.isArrayElement()) {
      putArrayIfMissing(path.withoutArrayReference());
      addAt(path, value);
    } else {
      putAt(path, value);
    }
  }

  /**
   * Adds a JSON array at the given path, if it is not there already.
   *
   * @param path the path to the new array - must not end with array suffix [] or [index]
   */
  private void putArrayIfMissing(Path path) {
    if (!hasPath(path)) {
      putAt(path, new ArrayList<>());
    }
  }

  private void putAt(Path path, Object value) {
    jsonData.put(path.parentPath().toString(), path.keyName(), value);
  }

  private void addAt(Path path, Object value) {
    jsonData.add(path.withoutArrayReference().toString(), value);
  }

  /**
   * Put parent of path if it doesn't already exist. There are two types of parents: JSON objects
   * and JSON arrays.
   *
   * <p>For JSON object parents, if it doesn't already exist an empty map is put in the right place.
   *
   * <p>For JSON array parents, if the array (e.g. applicant.children[]) doesn't already exist an
   * empty array is put in the right place, and then if the array element (e.g.
   * applicant.children[3]) doesn't already exist then empty maps are added until an empty map is
   * available at the right index.
   */
  private void putParentIfMissing(Path path) {
    Path parentPath = path.parentPath();

    if (hasPath(parentPath)) {
      return;
    }

    // TODO(#624): get rid of this recursion.
    putParentIfMissing(parentPath);

    if (parentPath.isArrayElement()) {
      putParentArray(path);
    } else {
      putAt(parentPath, new HashMap<>());
    }
  }

  /**
   * Put parent of path if it doesn't already exist, for parents that are arrays (e.g.
   * something[n]), and add empty JSON objects until an element at the index specified by the path
   * is available.
   */
  private void putParentArray(Path path) {
    Path parentPath = path.parentPath();
    int index = parentPath.arrayIndex();

    // For n=0, put a new array in, and add the 0th element.
    if (index == 0) {
      putAt(parentPath.withoutArrayReference(), new ArrayList<>());
      addAt(parentPath, new HashMap<>());

      // For n>0, only add the nth element if the n-1 element exists.
    } else if (hasPath(parentPath.atIndex(index - 1))) {
      addAt(parentPath, new HashMap<>());

      // TODO(#624): remove this recursion.
    } else {
      Path fakePathForRecursion = path.parentPath().atIndex(index - 1).join("fake");
      putParentIfMissing(fakePathForRecursion);
      addAt(parentPath, new HashMap<>());
    }
  }

  /**
   * Attempt to read a string at the given path. Returns {@code Optional#empty} if the path does not
   * exist or a value other than String is found.
   */
  public Optional<String> readString(Path path) {
    try {
      return this.read(path, String.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempt to read a integer at the given path. Returns {@code Optional#empty} if the path does
   * not exist or a value other than Integer is found.
   */
  public Optional<Long> readLong(Path path) {
    try {
      return this.read(path, Long.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempt to read a list at the given {@link Path}. Returns {@code Optional#empty} if the path
   * does not exist or a value other than an {@link ImmutableList} of strings is found.
   */
  public Optional<ImmutableList<String>> readList(Path path) {
    try {
      return this.read(path, IMMUTABLE_LIST_STRING_TYPE);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempts to read the names of the repeated entities at the given {@link Path}.
   *
   * @param path the {@link Path} to the repeated entities list.
   * @return a list of the names of the repeated entities. This is an empty list if there are no
   *     repeated entities at path.
   */
  public ImmutableList<String> readRepeatedEntities(Path path) {
    int index = 0;
    ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
    while (hasPath(path.atIndex(index))) {
      listBuilder.add(
          readString(path.atIndex(index).join(RepeaterQuestionDefinition.REPEATED_ENTITY_NAME_KEY))
              .get());
      index++;
    }
    return listBuilder.build();
  }

  /**
   * Returns the value at the given path, if it exists; otherwise returns {@link Optional#empty}.
   *
   * @param path the {@link Path} for the desired scalar
   * @param type the expected type of the scalar
   * @param <T> the expected type of the scalar
   * @return optionally returns the value at the path if it exists, or empty if not
   * @throws JsonPathTypeMismatchException if the scalar at that path is not the expected type
   */
  private <T> Optional<T> read(Path path, Class<T> type) throws JsonPathTypeMismatchException {
    try {
      return Optional.ofNullable(this.jsonData.read(path.toString(), type));
    } catch (PathNotFoundException e) {
      return Optional.empty();
    } catch (MappingException e) {
      throw new JsonPathTypeMismatchException(path.path(), type, e);
    }
  }

  /**
   * Returns the value at the given path, if it exists; otherwise returns {@link Optional#empty}.
   *
   * @param path the {@link Path} for the desired value
   * @param type the expected type of the value, represented as a {@link TypeRef}
   * @param <T> the expected type of the value
   * @return optionally returns the value at the path if it exists, or empty if not
   * @throws JsonPathTypeMismatchException if the value at that path is not the expected type
   */
  private <T> Optional<T> read(Path path, TypeRef<T> type) throws JsonPathTypeMismatchException {
    try {
      return Optional.ofNullable(this.jsonData.read(path.path(), type));
    } catch (PathNotFoundException e) {
      return Optional.empty();
    } catch (MappingException e) {
      throw new JsonPathTypeMismatchException(path.path(), type.getClass(), e);
    }
  }

  public Instant getCreatedTime() {
    return Instant.parse(this.jsonData.read("$.metadata.created_time"));
  }

  public void setCreatedTime(Instant i) {
    this.jsonData.put("$.metadata", "created_time", i.toString());
  }

  public String asJsonString() {
    return this.jsonData.jsonString();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof ApplicantData) {
      ApplicantData that = (ApplicantData) object;
      // Need to compare the JSON strings rather than the DocumentContexts themselves since
      // DocumentContext does not override equals.
      return this.jsonData.jsonString().equals(that.jsonData.jsonString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jsonData.jsonString());
  }

  /**
   * Copies all keys from {@code other}, recursively. All lists are merged. No values will be
   * overwritten.
   *
   * @return A list of {@code Path}s whose values could not be copied due to conflicts.
   */
  public ImmutableList<Path> mergeFrom(ApplicantData other) {
    Map<?, ?> rootAsMap = other.jsonData.read("$", Map.class);
    return mergeFrom(Path.empty(), rootAsMap);
  }

  private ImmutableList<Path> mergeFrom(Path rootKey, Map<?, ?> other) {
    ImmutableList.Builder<Path> pathsRemoved = new ImmutableList.Builder<>();
    for (Map.Entry<?, ?> entry : other.entrySet()) {
      String key = entry.getKey().toString();
      Path path = rootKey.join(key);
      if (hasPath(path)) {
        if (entry.getValue() instanceof Map) {
          // Recurse into maps.
          pathsRemoved.addAll(mergeFrom(path, (Map) entry.getValue()));
        } else if (entry.getValue() instanceof List) {
          // Add items from lists.
          // TODO(github.com/seattle-uat/civiform/issues/405): improve merge for repeated fields.
          for (Object item : (List) entry.getValue()) {
            this.jsonData.add(path.path(), item);
          }
        } else {
          try {
            if (!this.read(path, Object.class).equals(entry.getValue())) {
              pathsRemoved.add(path);
            }
          } catch (JsonPathTypeMismatchException e) {
            // If we can't confirm they're equal, consider it removed.
            pathsRemoved.add(path);
          }
        }
      } else {
        // currently empty, can add.
        this.put(path, entry.getValue());
      }
    }
    return pathsRemoved.build();
  }

  public void setUserName(String displayName) {
    String firstName;
    String lastName = null;
    String middleName = null;
    List<String> listSplit = Splitter.on(' ').splitToList(displayName);
    switch (listSplit.size()) {
      case 2:
        firstName = listSplit.get(0);
        lastName = listSplit.get(1);
        break;
      case 3:
        firstName = listSplit.get(0);
        middleName = listSplit.get(1);
        lastName = listSplit.get(2);
        break;
      case 1:
        // fallthrough
      default:
        // Too many names - put them all in first name.
        firstName = displayName;
    }
    setUserName(firstName, middleName, lastName);
  }

  private void setUserName(
      String firstName, @Nullable String middleName, @Nullable String lastName) {
    if (!hasPath(WellKnownPaths.APPLICANT_FIRST_NAME)) {
      putString(WellKnownPaths.APPLICANT_FIRST_NAME, firstName);
    }
    if (middleName != null && !hasPath(WellKnownPaths.APPLICANT_MIDDLE_NAME)) {
      putString(WellKnownPaths.APPLICANT_MIDDLE_NAME, middleName);
    }
    if (lastName != null && !hasPath(WellKnownPaths.APPLICANT_LAST_NAME)) {
      putString(WellKnownPaths.APPLICANT_LAST_NAME, lastName);
    }
  }
}
