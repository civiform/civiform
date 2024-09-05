package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SqlRow;
import io.ebean.Transaction;
import io.ebean.TxScope;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.program.predicate.Operator;

public final class ConvertAddressServiceAreaToArrayJob extends DurableJob {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ConvertAddressServiceAreaToArrayJob.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Database database;
  private final PersistedDurableJobModel persistedDurableJobModel;

  public record Row(String serviceAreaId, String state, Long timestamp) {
    public static Row create(String value) {

      String[] parts = value.split("_");

      if (parts.length < 3) {
        throw new IllegalArgumentException(
            String.format(
                "Could not create ConvertAddressServiceAreaToArrayJob.Row. Expected three parts"
                    + " from '%s' when split on '_'.",
                value));
      }

      String serviceAreaId = String.join("_", Arrays.copyOfRange(parts, 0, parts.length - 2));
      String state = parts[parts.length - 2];
      String timestampStr = parts[parts.length - 1];

      return new Row(serviceAreaId, state, Long.parseLong(timestampStr));
    }
  }

  public ConvertAddressServiceAreaToArrayJob(PersistedDurableJobModel persistedDurableJobModel) {
    this.persistedDurableJobModel = checkNotNull(persistedDurableJobModel);
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    LOGGER.info("Run - Begin");

    String selectSql =
        """
SELECT id, (object#>>'{}')::jsonb as object
FROM applicants
WHERE jsonb_path_exists((object#>>'{}')::jsonb, '$.applicant.**.service_area ? (@.type() == "string")')
""";

    // AND id in (350680, 350668, 350663, 350660, 350653, 350637, 350634, 350632, 349516)

    List<SqlRow> programs = database.sqlQuery(selectSql).findList();

    try (Transaction jobTransaction = database.beginTransaction()) {
      jobTransaction.setNestedUseSavepoint();
      int errorCount = 0;
      for (SqlRow program : programs) {
        LOGGER.debug("id: {}", program.getLong("id"));

        try {
          JsonNode rootJsonNode = objectMapper.readTree(program.getString("object"));
          int startingRootJsonNodeHashCode = rootJsonNode.hashCode();

          if (!rootJsonNode.isObject()) {
            LOGGER.error("object is not an object");
            continue;
          }

          if (!rootJsonNode.has("applicant")) {
            LOGGER.error("object is missing applicant node");
          }

          for (var questionJsonNode : rootJsonNode.get("applicant")) {
            if (questionJsonNode.has("service_area")
                && questionJsonNode.get("service_area").getNodeType() == JsonNodeType.STRING) {
              List<Row> items =
                  Arrays.stream(questionJsonNode.get("service_area").asText().split(","))
                      .map(Row::create)
                      .collect(ImmutableList.toImmutableList());

              JsonNode newJsonNode = objectMapper.valueToTree(items);
              ((ObjectNode) questionJsonNode).replace("service_area", newJsonNode);
            }
          }

          if (startingRootJsonNodeHashCode == rootJsonNode.hashCode()) {
            LOGGER.debug("No changes made to JsonNode. No need to update the database.");
            continue;
          }

          String updateApplicantsSql =
              """
              update applicants
              set object = to_jsonb(((:objectJson)::jsonb)::text)
              where id = :id
              """;

          try (Transaction stepTransaction = database.beginTransaction(TxScope.mandatory())) {
            database
                .sqlUpdate(updateApplicantsSql)
                .setParameter("id", program.getLong("id"))
                .setParameter("objectJson", rootJsonNode.toString())
                .execute();

            stepTransaction.commit();
            LOGGER.debug("JsonNode change. Updated database.");
          }
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
          errorCount++;
        }
      }

      if (errorCount == 0) {
        LOGGER.debug("Job succeeded.");
        jobTransaction.commit();
      } else {
        LOGGER.error(
            "Job failed to convert service area. All changes undone. Error count: {}", errorCount);
        jobTransaction.rollback();
      }
    }

    LOGGER.info("Run - End");
  }

  public void addOperatorToLeafAddressServiceAreaNode(JsonNode nodeJsonNode) {
    if (!nodeJsonNode.isObject()) {
      return;
    }

    if (nodeJsonNode.has("children") && nodeJsonNode.get("children").isArray()) {
      for (JsonNode childNodeJsonNode : nodeJsonNode.get("children")) {
        if (childNodeJsonNode.has("node")) {
          addOperatorToLeafAddressServiceAreaNode(childNodeJsonNode.get("node"));
        }
      }
    } else if (nodeJsonNode.has("type")
        && Objects.equals(nodeJsonNode.get("type").textValue(), "leafAddressServiceArea")
        && !nodeJsonNode.has("operator")) {
      ((ObjectNode) nodeJsonNode).put("operator", Operator.IN_SERVICE_AREA.name());
    }
  }
}
