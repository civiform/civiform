package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.Path;

/**
 * The enumerator context contains information about a {@link RepeatedEntity}.
 *
 * <p>When there is no repeated entity in scope, this should be an {@link #empty()} enumerator
 * context.
 */
@AutoValue
public abstract class EnumeratorContext {

  /** The {@link RepeatedEntity} this object contains context for. */
  public abstract Optional<RepeatedEntity> repeatedEntity();

  /** A list of ancestors to the {@link #repeatedEntity()}, ending with itself. */
  public abstract ImmutableList<RepeatedEntity> ancestors();

  /** A list of siblings to the {@link #repeatedEntity()}, including itself. */
  public abstract ImmutableList<RepeatedEntity> siblings();

  /** A fully qualified path to the root of this {@link RepeatedEntity} in {@link ApplicantData}. */
  public abstract Path contextualizedPath();

  /** Create an {@link EnumeratorContext}. */
  public static EnumeratorContext create(
      RepeatedEntity repeatedEntity,
      ImmutableList<RepeatedEntity> ancestors,
      ImmutableList<RepeatedEntity> siblings,
      Path contextualizedPath) {
    return new AutoValue_EnumeratorContext(
        Optional.of(repeatedEntity), ancestors, siblings, contextualizedPath);
  }

  /** The enumerator context when there is no repeated entity in scope. */
  public static EnumeratorContext empty() {
    return new AutoValue_EnumeratorContext(
        Optional.empty(), ImmutableList.of(), ImmutableList.of(), ApplicantData.APPLICANT_PATH);
  }

  /** True if there is no repeated entity in scope. */
  public boolean isEmpty() {
    return repeatedEntity().isEmpty();
  }

  /** Returns a new enumerator context for a nested repeated entity. */
  public EnumeratorContext append(
      RepeatedEntity repeatedEntity, ImmutableList<RepeatedEntity> siblings) {
    ImmutableList<RepeatedEntity> updatedAncestors =
        ImmutableList.<RepeatedEntity>builder().addAll(ancestors()).add(repeatedEntity).build();
    Path updatedPath =
        contextualizedPath()
            .join(repeatedEntity.enumeratorQuestionDefinition().getQuestionPathSegment())
            .atIndex(repeatedEntity.index());
    return create(repeatedEntity, updatedAncestors, siblings, updatedPath);
  }
}
