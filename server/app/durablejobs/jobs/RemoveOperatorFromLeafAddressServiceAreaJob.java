package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SqlRow;
import io.ebean.Transaction;
import java.util.List;
import java.util.Objects;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This job searches for {@link services.program.predicate.LeafAddressServiceAreaExpressionNode}s
 * found in the programs.block_definitions columns of the database. When it finds one it removes the
 * property named `operator`.
 *
 * <p>Additional notes:
 * <li>Nodes may have children so this recursively searches the tree
 * <li>Both hidePredicate and eligibilityDefinition.predicate are checked
 * <li>Will not modify nodes that do not have this property
 * <li>Idempotent
 * <li>Any failure will rollback the entire changeset
 */
public final class RemoveOperatorFromLeafAddressServiceAreaJob extends DurableJob {
  private static final Logger logger =
      LoggerFactory.getLogger(RemoveOperatorFromLeafAddressServiceAreaJob.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Database database;
  private final PersistedDurableJobModel persistedDurableJobModel;

  /**
   * @param persistedDurableJobModel Job model for integrating with the durable job system
   * @param cerberusSafeguard Guarding the gate making sure this doesn't run by accident
   */
  public RemoveOperatorFromLeafAddressServiceAreaJob(
      PersistedDurableJobModel persistedDurableJobModel, String cerberusSafeguard) {
    if (!Objects.equals(cerberusSafeguard, "Orpheus's lyre")) {
      throw new IllegalArgumentException(
          """
You attempted to use the RemoveOperatorFromLeafAddressServiceAreaJob. This job is here
only in case of an emergency so great that we need to rollback the
companion AddOperatorToLeafAddressServiceAreaJob.

Most likely you should NOT use this job.
""");
    }

    this.persistedDurableJobModel = checkNotNull(persistedDurableJobModel);
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    logger.debug("Run - Begin");

    String selectSql =
        """
SELECT id, block_definitions
FROM programs
WHERE jsonb_path_exists(block_definitions, '$.hidePredicate.rootNode.**.node ? (@.type == "leafAddressServiceArea")')
OR jsonb_path_exists(block_definitions, '$.eligibilityDefinition.predicate.rootNode.**.node ? (@.type == "leafAddressServiceArea")')
""";

    List<SqlRow> programs = database.sqlQuery(selectSql).findList();

    try (Transaction jobTransaction = database.beginTransaction()) {
      jobTransaction.setNestedUseSavepoint();
      int errorCount = 0;

      for (SqlRow program : programs) {
        logger.debug("id: {}", program.getLong("id"));

        try {
          JsonNode rootJsonNode = objectMapper.readTree(program.getString("block_definitions"));
          int startingRootJsonNodeHashCode = rootJsonNode.hashCode();

          if (!rootJsonNode.isArray()) {
            logger.error("block_definitions is not an array");
            continue;
          }

          for (var blockDefinitionJsonNode : rootJsonNode) {
            JsonNode nodeJsonNode =
                blockDefinitionJsonNode.at("/eligibilityDefinition/predicate/rootNode/node");
            if (!nodeJsonNode.isMissingNode()) {
              removeOperatorFromLeafAddressServiceAreaNode(nodeJsonNode);
            }
          }

          for (var blockDefinitionJsonNode : rootJsonNode) {
            JsonNode nodeJsonNode = blockDefinitionJsonNode.at("/hidePredicate/rootNode/node");
            if (!nodeJsonNode.isMissingNode()) {
              removeOperatorFromLeafAddressServiceAreaNode(nodeJsonNode);
            }
          }

          if (startingRootJsonNodeHashCode == rootJsonNode.hashCode()) {
            logger.debug("No changes made to JsonNode. No need to update the database.");
            continue;
          }

          String updateSql =
              """
              update programs
              set block_definitions = CAST(:block_definitions AS jsonb)
              where id = :id
              """;

          try (Transaction stepTransaction = database.beginTransaction()) {
            database
                .sqlUpdate(updateSql)
                .setParameter("id", program.getLong("id"))
                .setParameter("block_definitions", rootJsonNode.toString())
                .execute();

            stepTransaction.commit();
            logger.debug("JsonNode change. Updated database.");
          }

        } catch (Exception e) {
          logger.error(e.getMessage(), e);
          errorCount++;
        }
      }
      if (errorCount == 0) {
        logger.debug("Job succeeded.");
        jobTransaction.commit();
      } else {
        logger.error("Job failed to add operator. All changes undone. Error count: {}", errorCount);
        jobTransaction.rollback();
      }
    }

    logger.debug("Run - End");
  }

  public void removeOperatorFromLeafAddressServiceAreaNode(JsonNode nodeJsonNode) {
    if (!nodeJsonNode.isObject()) {
      return;
    }

    if (nodeJsonNode.has("children") && nodeJsonNode.get("children").isArray()) {
      for (JsonNode childNodeJsonNode : nodeJsonNode.get("children")) {
        if (childNodeJsonNode.has("node")) {
          removeOperatorFromLeafAddressServiceAreaNode(childNodeJsonNode.get("node"));
        }
      }
    } else if (nodeJsonNode.has("type")
        && Objects.equals(nodeJsonNode.get("type").textValue(), "leafAddressServiceArea")
        && nodeJsonNode.has("operator")) {
      ((ObjectNode) nodeJsonNode).remove("operator");
    }
  }
}
