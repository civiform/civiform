package services;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import services.applicant.Currency;
import services.applicant.JsonPathProvider;
import services.applicant.exception.JsonPathTypeMismatchException;
import services.applicant.predicate.JsonPathPredicate;
import services.applicant.question.Scalar;

// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING CfJsonDocumentContext

public class CfJsonDocumentContext {
  private static final TypeRef<List<Object>> LIST_OF_OBJECTS_TYPE = new TypeRef<>() {};
  private static final TypeRef<ImmutableList<Long>> IMMUTABLE_LIST_LONG_TYPE = new TypeRef<>() {};

  protected boolean locked = false;
  protected final DocumentContext jsonData;

  public CfJsonDocumentContext(DocumentContext jsonData) {
    this.jsonData = checkNotNull(jsonData);
  }

  public CfJsonDocumentContext(String jsonString) {
    this(JsonPathProvider.getJsonPath().parse(checkNotNull(jsonString)));
  }

  public CfJsonDocumentContext() {
    this("{}");
  }

  public DocumentContext getDocumentContext() {
    return jsonData;
  }

  /** Makes this instance immutable. A locked instance cannot be unlocked. */
  public void lock() {
    locked = true;
  }

  /**
   * Checks whether the given path exists in the JSON data. Returns true if the path is present;
   * false otherwise.
   *
   * <p>Semantically, in the context of an applicant or application, this checks whether the
   * applicant has answered this question before.
   *
   * @param path the {@link Path} to check
   * @return true if path is present; false otherwise
   */
  public boolean hasPath(Path path) {
    try {
      jsonData.read(path.toString());
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
   * Stores the dollars currency string as a long of the currency cents at the given {@link Path}.
   *
   * <p>This method requires the input string to be a number, optionally with commas and optionally
   * with exactly 2 decimal points.
   */
  public void putCurrencyDollars(Path path, String dollars) {
    if (dollars.isEmpty()) {
      putNull(path);
      return;
    }
    Currency currency = Currency.parse(dollars);
    put(path, currency.getCents());
  }

  /**
   * Stores the date string as a millisecond timestamp at the given {@link Path}.
   *
   * <p>This method requires the input string to be in "yyyy-MM-dd" format.
   */
  public void putDate(Path path, String dateString) {
    if (dateString.isEmpty()) {
      putNull(path);
    } else {
      LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      put(path, localDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
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

  /** Writes a long value. */
  public void putLong(Path path, long value) {
    put(path, value);
  }

  /** Writes a double value. */
  public void putDouble(Path path, double value) {
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
   * Parses and writes a Double value, given as a string. If the string is empty, a null value is
   * written.
   */
  public void putDouble(Path path, String value) {
    if (value.isEmpty()) {
      putNull(path);
    } else {
      put(path, Double.parseDouble(value));
    }
  }

  /**
   * Puts the names of the repeated entities at the path. Each element in the JSON array at the path
   * is a JSON object that has at minimum a property {@link Scalar#ENTITY_NAME} that contains a
   * string value, along with possibly other nested answers to questions or repeated entities.
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
        putString(path.atIndex(i).join(Scalar.ENTITY_NAME), entityNames.get(i));
      }
    }
  }

  /**
   * Puts null at the provided path.
   *
   * @param path a path
   */
  public void putNull(Path path) {
    if (!path.isArrayElement()) {
      put(path, null);
    }
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
    checkLocked();
    putParentIfMissing(path);
    if (path.isArrayElement()) {
      putArrayIfMissing(path.withoutArrayReference());
      addAt(path, value);
    } else {
      putAt(path, value);
    }
  }

  /**
   * Puts an array at a given path, building parent objects as needed.
   *
   * @param path the {@link Path} where the array should be added.
   * @param list a {@link List} containing scalar values such as strings or longs.
   */
  public void putArray(Path path, List list) {
    checkLocked();
    putParentIfMissing(path);
    jsonData.put(path.parentPath().toString(), path.keyName(), list);
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

  /**
   * Clears an array in preparation of updates if the path is pointing to an array element,
   * regardless of whether there are any values present.
   */
  public void maybeClearArray(Path path) {
    checkLocked();
    if (path.isArrayElement()) {
      putParentIfMissing(path);
      maybeDelete(path.withoutArrayReference());
    }
  }

  /** Delete whatever is there if it exists. Returns whether a delete actually happened. */
  public void maybeDelete(Path path) {
    checkLocked();
    if (hasPath(path)) {
      jsonData.delete(path.toString());
    }
  }

  private void putAt(Path path, Object value) {
    checkLocked();
    jsonData.put(path.parentPath().toString(), path.keyName(), value);
  }

  private void addAt(Path path, Object value) {
    checkLocked();
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
    } else {
      Path fakePathForRecursion = path.parentPath().atIndex(index - 1).join("fake");
      putParentIfMissing(fakePathForRecursion);
      addAt(parentPath, new HashMap<>());
    }
  }

  public Optional<LocalDate> readDate(Path path) {
    return readLong(path)
        .map(epoch -> Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC).toLocalDate());
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
   * Attempt to read a currency value at the given path. validating the value.
   *
   * <p>Validates the value is of an expected format and converts to the number of cents.
   *
   * <p>Returns {@code Optional#empty} if the path does not exist or a value other than Long is
   * found.
   */
  public Optional<Currency> readCurrency(Path path) {
    try {
      return this.read(path, Long.class).map(Currency::new);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempt to read a integer at the given path. Returns {@code Optional#empty} if the path does
   * not exist or a value other than a number is found. If the number has a decimal, the decimal
   * will be truncated.
   */
  public Optional<Long> readLong(Path path) {
    try {
      return this.read(path, Long.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempt to read a double at the given path. Returns {@code Optional#empty} if the path does not
   * exist or a value other than a number is found.
   */
  public Optional<Double> readDouble(Path path) {
    try {
      return this.read(path, Double.class);
    } catch (JsonPathTypeMismatchException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempt to read a list at the given {@link Path}. Returns {@code Optional#empty} if the path
   * does not exist or a value other than an {@link ImmutableList} of longs is found.
   */
  public Optional<ImmutableList<Long>> readList(Path path) {
    try {
      return this.read(path, IMMUTABLE_LIST_LONG_TYPE);
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
      listBuilder.add(readString(path.atIndex(index).join(Scalar.ENTITY_NAME)).orElse(""));
      index++;
    }
    return listBuilder.build();
  }

  /**
   * If there are no repeated entities at the path, remove the array entirely. Returns true if there
   * are no repeated entities anymore.
   *
   * <p>This method needs to check that there are no repeated entity data stored before deleting
   * because we do not want to delete repeated entity data via this method. To delete data for
   * repeated entities, use {@link #deleteRepeatedEntities(Path, ImmutableList)};
   */
  public boolean maybeClearRepeatedEntities(Path path) {
    checkLocked();
    if (readRepeatedEntities(path).isEmpty()) {
      maybeDelete(path.withoutArrayReference());
      return true;
    }
    return false;
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
      return Optional.ofNullable(jsonData.read(path.toString(), type));
    } catch (PathNotFoundException e) {
      return Optional.empty();
    } catch (MappingException e) {
      throw new JsonPathTypeMismatchException(path, type, e);
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
      return Optional.ofNullable(jsonData.read(path.toString(), type));
    } catch (PathNotFoundException e) {
      return Optional.empty();
    } catch (MappingException e) {
      throw new JsonPathTypeMismatchException(path, type.getClass(), e);
    }
  }

  /**
   * Reads the value at the given path as a string. Returns {@link Optional#empty} if there is no
   * value at the path. For JSON arrays of strings, this formats the array as a string according to
   * {@link ImmutableList#toString}.
   *
   * @param path the {@link Path} to read
   * @return optionally returns the value at the path as a string if it exists, or empty if not
   */
  public Optional<String> readAsString(Path path) {
    if (isJsonArray(path)) {
      return readList(path).map(ImmutableList::toString);
    }

    return readString(path);
  }

  /**
   * Delete the ENTIRE repeated entity for each entity specified by the path to an array element and
   * list of indices.
   *
   * @return true if something is deleted.
   */
  public boolean deleteRepeatedEntities(Path path, ImmutableList<Integer> indices) {
    checkLocked();

    /** Early return if there's nothing to delete */
    if (indices.isEmpty()) {
      return false;
    }

    ImmutableList<Integer> reverseSortedIndices =
        indices.stream()
            .sorted(Collections.reverseOrder())
            .collect(ImmutableList.toImmutableList());

    // Make sure there are enough things to delete by checking the first index in the reverse sorted
    // list
    if (!hasPath(path.atIndex(reverseSortedIndices.get(0)))) {
      return false;
    }

    // Delete in reverse sorted order because deletion is index based, and indices would need to be
    // decremented for each deletion if it wasn't reverse sorted.
    for (int index : reverseSortedIndices) {
      jsonData.delete(path.atIndex(index).toString());
    }
    return true;
  }

  /** Returns true if the value at the path is a JSON array of longs, and false otherwise. */
  private boolean isJsonArray(Path path) {
    try {
      this.read(path, IMMUTABLE_LIST_LONG_TYPE);
      return true;
    } catch (JsonPathTypeMismatchException e) {
      return false;
    }
  }

  /**
   * Evaluates a {@code JsonPathPredicate} query string returning true if there is matching data.
   */
  public boolean evalPredicate(JsonPathPredicate jsonPathPredicate) {
    try {
      return jsonData.read(jsonPathPredicate.pathPredicate(), LIST_OF_OBJECTS_TYPE).size() > 0;
    } catch (PathNotFoundException e) {
      return false;
    }
  }

  public String asJsonString() {
    return jsonData.jsonString();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof CfJsonDocumentContext) {
      CfJsonDocumentContext that = (CfJsonDocumentContext) object;
      // Need to compare the JSON strings rather than the DocumentContexts themselves since
      // DocumentContext does not override equals.
      return jsonData.jsonString().equals(that.jsonData.jsonString());
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
  public ImmutableList<Path> mergeFrom(CfJsonDocumentContext other) {
    Map<?, ?> rootAsMap = other.jsonData.read("$", Map.class);
    return mergeFrom(Path.empty(), rootAsMap);
  }

  private ImmutableList<Path> mergeFrom(Path rootKey, Map<?, ?> other) {
    checkLocked();
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
          for (Object item : (List) entry.getValue()) {
            jsonData.add(path.toString(), item);
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

  protected void checkLocked() {
    if (locked) {
      throw new RuntimeException(
          "Cannot change " + this.getClass().getSimpleName() + " after it has been locked.");
    }
  }
}
