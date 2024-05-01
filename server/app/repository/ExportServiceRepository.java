package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import javax.inject.Inject;
import services.question.types.QuestionDefinition;

/** Implements queries related to CSV exporting needs. */
public final class ExportServiceRepository {
  private final Database database;

  @Inject
  public ExportServiceRepository() {
    this.database = DB.getDefault();
  }

  /**
   * Queries for all unique option admin names ordered by their creation time.
   *
   * @param questionDefinition {@link QuestionDefinition} of a Checkbox question
   * @throws RuntimeException when the questionDefinition is not of type Checkbox
   */
  public ImmutableList<String> getAllHistoricMultiOptionAdminNames(
      QuestionDefinition questionDefinition) {
    if (!questionDefinition.getQuestionType().isMultiOptionType()) {
      throw new RuntimeException("The Question Type is not a multi-option type");
    }
    String questionName = questionDefinition.getName();
    ImmutableList<String> allOptions =
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
                    + "  INNER JOIN versions_questions vq ON  vq.questions_id = q.id "
                    + "  INNER JOIN versions v ON vq.versions_id = v.id "
                    + "  WHERE lifecycle_stage IN ('obsolete', 'active') "
                    + "  AND name = :currentQuestion "
                    + ") AS all_options "
                    + "GROUP BY all_options.admin_name "
                    + "ORDER BY MIN(all_options.create_time), MIN(all_options.id) ASC")
            .setParameter("currentQuestion", questionName)
            .setLabel("ExportServiceRepository.getAllHistoricOptionAdminNames")
            .findList()
            .stream()
            .map(sqlRow -> sqlRow.getString("admin_name"))
            .collect(ImmutableList.toImmutableList());
    if (allOptions.size() < 1) {
      throw new RuntimeException("Draft questions cannot be exported");
    }
    return allOptions;
  }
}
