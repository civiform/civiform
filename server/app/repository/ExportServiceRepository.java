package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import java.util.LinkedHashSet;
import javax.inject.Inject;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Implements queries related to CSV exporting needs. */
public final class ExportServiceRepository {
  private final Database database;

  @Inject
  public ExportServiceRepository() {
    this.database = DB.getDefault();
  }

  /**
   * This method queries for all unique option admin names ordered by is creation time.
   *
   * @param (@link QuestionDefinition) of a Checkbox question
   * @throws (@link RuntimeException) when the questionDefinition is not of type Checkbox
   */
  public ImmutableList<String> getAllHistoricMultiOptionAdminNames(
      QuestionDefinition questionDefinition) {
    if (!questionDefinition.getQuestionType().equals(QuestionType.CHECKBOX)) {
      throw new RuntimeException("The Question Type is not checkbox");
    }
    String questionName = questionDefinition.getName();
    LinkedHashSet<String> allOptions = new LinkedHashSet<>();
    database
        .sqlQuery(
            "SELECT"
                + "  all_options.admin_name "
                + "FROM ( "
                + "  SELECT "
                + "    q.create_time, "
                + "    jsonb_array_elements(q.question_options) ->>'adminName' AS admin_name, "
                + "    jsonb_array_elements(q.question_options) ->>'id' AS id "
                + "  FROM questions q "
                + "  INNER JOIN versions_questions qv ON qv.questions_id = q.id "
                + "  INNER JOIN versions v ON qv.versions_id = v.id "
                + "  WHERE lifecycle_stage IN ('obsolete', 'active') "
                + "  AND name = :currentQuestion "
                + ") AS all_options "
                + "GROUP BY all_options.admin_name "
                + "ORDER BY MIN(all_options.create_time), MIN(all_options.id) ASC")
        .setParameter("currentQuestion", questionName)
        .findList()
        .stream()
        .forEach(sqlRow -> allOptions.add(sqlRow.getString("admin_name")));
    if (allOptions.size() < 1) {
      throw new RuntimeException("Draft questions cannot be exported");
    }
    ImmutableList.Builder<String> immtableListBuilder = ImmutableList.builder();
    return immtableListBuilder.addAll(allOptions).build();
  }
}
