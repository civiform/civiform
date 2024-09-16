package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.TxScope;
import java.util.Arrays;
import java.util.Locale;
import models.ApplicantModel;
import models.ApplicationModel;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;

public final class ConvertAddressServiceAreaToArrayJob extends DurableJob {
  private static final Logger logger =
      LoggerFactory.getLogger(ConvertAddressServiceAreaToArrayJob.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Database database;
  private final PersistedDurableJobModel persistedDurableJobModel;

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
    logger.info("Run - Begin");

    try (Transaction jobTransaction = database.beginTransaction()) {
      jobTransaction.setNestedUseSavepoint();
      int errorCount = 0;

      // Filter to only include rows what have a service_area key that is a string type. Vastly
      // improves the run time cutting out a large number of unneeded records.
      String filter =
          """
jsonb_path_exists((object#>>'{}')::jsonb, '$.applicant.**.service_area ? (@.type() == "string")')
""";

      try (var query = database.find(ApplicantModel.class).where().raw(filter).findIterate()) {
        while (query.hasNext()) {
          try (Transaction stepTransaction = database.beginTransaction(TxScope.mandatory())) {
            ApplicantModel applicant = query.next();
            logger.debug("Converting service area for applicant id {}", applicant.id);
            applicant.setApplicantData(processRow(applicant.getApplicantData()));
            applicant.save(stepTransaction);
            stepTransaction.commit();
          } catch (Exception e) {
            errorCount++;
            logger.error(e.getMessage(), e);
          }
        }
      }

      try (var query = database.find(ApplicationModel.class).where().raw(filter).findIterate()) {
        while (query.hasNext()) {
          try (Transaction stepTransaction = database.beginTransaction(TxScope.mandatory())) {
            ApplicationModel application = query.next();
            logger.debug("Converting service area for application id {}", application.id);
            application.setApplicantData(processRow(application.getApplicantData()));
            application.save(stepTransaction);
            stepTransaction.commit();
          } catch (Exception e) {
            errorCount++;
            logger.error(e.getMessage(), e);
          }
        }
      }

      if (errorCount == 0) {
        logger.info("Job succeeded");
        jobTransaction.commit();
      } else {
        logger.error("Failed to convert service area. All changes undone. Errors: {}", errorCount);
        jobTransaction.rollback();
      }
    }

    logger.info("Run - End");
  }

  private ApplicantData processRow(ApplicantData applicantData) throws JsonProcessingException {
    String serviceAreaName = Scalar.SERVICE_AREA.name().toLowerCase(Locale.ROOT);
    JsonNode rootJsonNode = objectMapper.readTree(applicantData.asJsonString());

    for (var questionJsonNode : rootJsonNode.findParents(serviceAreaName)) {
      if (questionJsonNode.has(serviceAreaName)
          && questionJsonNode.get(serviceAreaName).getNodeType() == JsonNodeType.STRING) {
        ImmutableList<ServiceAreaInclusion> items =
            Arrays.stream(questionJsonNode.get(serviceAreaName).asText().split(","))
                .map(ConvertAddressServiceAreaToArrayJob::buildServiceAreaArrayFromString)
                .collect(ImmutableList.toImmutableList());

        JsonNode newJsonNode = objectMapper.valueToTree(items);

        // Singular "service_area"
        ((ObjectNode) questionJsonNode).remove(serviceAreaName);

        // Plural "service_areas"
        ((ObjectNode) questionJsonNode)
            .set(Scalar.SERVICE_AREAS.name().toLowerCase(Locale.ROOT), newJsonNode);
      }
    }

    return new ApplicantData(rootJsonNode.toString(), applicantData.getApplicant());
  }

  @VisibleForTesting
  static ServiceAreaInclusion buildServiceAreaArrayFromString(String value) {
    String[] parts = value.split("_");

    if (parts.length < 3) {
      throw new IllegalArgumentException(
          String.format(
              "Not enough parts available to build record from '%s' when split on '_'.", value));
    }

    // The original string was in the pattern serviceareaid_state_timestamp. The state and
    // timestamp are have known fixed values. While there was a regular expression that should
    // only have allowed alphanumerics and a hyphen to be used for the serviceareaid it's
    // technically free form from the user data. If there were a bug that allowed an underscore
    // to get through it would prevent breaking this delimited string up correctly.
    //
    // Since the code changes for service area can handle serviceareaid being anything we'll treat
    // all parts of the array excluding the last two as the serviceareaid value.
    String serviceAreaId = String.join("_", Arrays.copyOfRange(parts, 0, parts.length - 2));

    // The second to last part is the state value
    String state = parts[parts.length - 2];
    ServiceAreaState serviceAreaState = ServiceAreaState.getEnumFromSerializedFormat(state);

    // The last part is the timestamp
    String timestampStr = parts[parts.length - 1];

    return ServiceAreaInclusion.create(
        serviceAreaId, serviceAreaState, Long.parseLong(timestampStr));
  }
}
