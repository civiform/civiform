package services.applicant;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import services.Path;
import services.question.types.EnumeratorQuestionDefinition;

/** A repeated entity represents one of the applicant's answers to an enumerator question. */
@AutoValue
public abstract class RepeatedEntity {

  private static final String REPLACEMENT_STRING = "$this";
  private static final String REPLACEMENT_PARENT_STRING = "parent";

  /**
   * Create all the non-nested repeated entities associated with the enumerator question, with no
   * parent.
   */
  public static ImmutableList<RepeatedEntity> createRepeatedEntities(
      EnumeratorQuestionDefinition enumeratorQuestionDefinition, ApplicantData applicantData) {
    return RepeatedEntity.createRepeatedEntities(
        Optional.empty(), enumeratorQuestionDefinition, applicantData);
  }
  /**
   * Create all the nested repeated entities associated with the enumerator question, with this
   * repeated entity as their parent.
   */
  public ImmutableList<RepeatedEntity> createNestedRepeatedEntities(
      EnumeratorQuestionDefinition enumeratorQuestionDefinition, ApplicantData applicantData) {
    return RepeatedEntity.createRepeatedEntities(
        Optional.of(this), enumeratorQuestionDefinition, applicantData);
  }

  private static ImmutableList<RepeatedEntity> createRepeatedEntities(
      Optional<RepeatedEntity> parent,
      EnumeratorQuestionDefinition enumeratorQuestionDefinition,
      ApplicantData applicantData) {
    Path contextualizedEnumeratorPath =
        parent
            .map(RepeatedEntity::contextualizedPath)
            .orElse(ApplicantData.APPLICANT_PATH)
            .join(enumeratorQuestionDefinition.getQuestionPathSegment());
    ImmutableList<String> entityNames =
        applicantData.readRepeatedEntities(contextualizedEnumeratorPath);
    ImmutableList.Builder<RepeatedEntity> repeatedEntitiesBuilder = ImmutableList.builder();
    for (int i = 0; i < entityNames.size(); i++) {
      repeatedEntitiesBuilder.add(
          create(enumeratorQuestionDefinition, parent, entityNames.get(i), i));
    }
    return repeatedEntitiesBuilder.build();
  }

  private static RepeatedEntity create(
      EnumeratorQuestionDefinition enumeratorQuestionDefinition,
      Optional<RepeatedEntity> parent,
      String entityName,
      int index) {
    assert enumeratorQuestionDefinition.isEnumerator();
    return new AutoValue_RepeatedEntity(enumeratorQuestionDefinition, parent, entityName, index);
  }

  /**
   * The {@link services.question.types.QuestionType#ENUMERATOR} question definition associated with
   * this repeated entity.
   */
  public abstract EnumeratorQuestionDefinition enumeratorQuestionDefinition();

  /** If this is a nested repeated entity, this returns the immediate parent repeated entity. */
  public abstract Optional<RepeatedEntity> parent();

  /** The entity name provided by the applicant. */
  public abstract String entityName();

  /**
   * The positional index of this repeated entity with respect to the other repeated entities for
   * the applicant associated with this repeated entity's enumerator question.
   */
  public abstract int index();

  /** The contextualized path to the root of this repeated entity. */
  public Path contextualizedPath() {
    Path parentPath =
        parent().map(RepeatedEntity::contextualizedPath).orElse(ApplicantData.APPLICANT_PATH);
    return parentPath
        .join(enumeratorQuestionDefinition().getQuestionPathSegment())
        .atIndex(index());
  }

  /** The depth is how deeply nested this repeated entity is. */
  public int depth() {
    return 1 + parent().map(RepeatedEntity::depth).orElse(0);
  }

  /**
   * Contextualize the text with repeated entity names.
   *
   * <p>Replaces "\$this" with this repeated entity's name. "\$this.parent" and
   * "\$this.parent.parent" (ad infinitum) are replaced with the names of the ancestors of this
   * repeated entity.
   */
  public String contextualize(String text) {
    return contextualize(text, REPLACEMENT_STRING);
  }

  /**
   * Recursive helper method for {@link #contextualize(String)}.
   *
   * <p>Recursively do the parents FIRST, because {@link String#replace} is eager and will replace
   * "\$this" first and mess up "\$this.parent".
   */
  private String contextualize(String text, String target) {
    String updatedText =
        parent()
            .map(p -> p.contextualize(text, target + "." + REPLACEMENT_PARENT_STRING))
            .orElse(text);
    return updatedText.replace(target, entityName());
  }
}
