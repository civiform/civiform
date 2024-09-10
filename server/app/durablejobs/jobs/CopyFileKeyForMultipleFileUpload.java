package durablejobs.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.QueryIterator;
import io.ebean.Transaction;
import java.util.Locale;
import models.ApplicantModel;
import models.ApplicationModel;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;

/**
 * Iterates through all applicants and all their applications and updates them for multiple file
 * uploads.
 *
 * <p>Looks at applicant data for any node containing "file_key" and creates a sibling node
 * "file_keys" with the same data.
 */
public final class CopyFileKeyForMultipleFileUpload extends DurableJob {
  private static final String FILE_KEY_PROPERTY = Scalar.FILE_KEY.name().toLowerCase(Locale.ROOT);
  private static final String FILE_KEY_LIST_PROPERTY =
      Scalar.FILE_KEY_LIST.name().toLowerCase(Locale.ROOT);

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CopyFileKeyForMultipleFileUpload.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final PersistedDurableJobModel persistedDurableJobModel;
  private final Database database;

  public CopyFileKeyForMultipleFileUpload(PersistedDurableJobModel persistedDurableJobModel) {
    this.persistedDurableJobModel = persistedDurableJobModel;
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    LOGGER.info("Copying file keys for applicants.");

    try (Transaction jobTransaction = database.beginTransaction()) {
      int errorCount = 0;

      try (QueryIterator<ApplicantModel> applicants =
          database.find(ApplicantModel.class).findIterate()) {
        while (applicants.hasNext()) {
          try {
            ApplicantModel applicant = applicants.next();
            ApplicantData migratedData = migrateApplicantData(applicant.getApplicantData());
            applicant.setApplicantData(migratedData);
            applicant.save(jobTransaction);
          } catch (Exception e) {
            errorCount++;
            LOGGER.error(e.getMessage(), e);
          }
        }
      }

      LOGGER.info("Copying file keys for applications.");

      try (QueryIterator<ApplicationModel> applications =
          database.find(ApplicationModel.class).findIterate()) {
        while (applications.hasNext()) {
          try {
            ApplicationModel application = applications.next();
            application.setApplicantData(migrateApplicantData(application.getApplicantData()));
            application.save(jobTransaction);
          } catch (Exception e) {
            errorCount++;
            LOGGER.error(e.getMessage(), e);
          }
        }
      }

      if (errorCount == 0) {
        LOGGER.info("Finished copying file keys for multiple file upload feature.");
        jobTransaction.commit();
      } else {
        LOGGER.error(
            "Failed to copy file keys for multiple file upload feature. See previous logs for"
                + " failures. Total failures: {0}",
            errorCount);
        jobTransaction.rollback();
      }
    }
  }

  @VisibleForTesting
  ApplicantData migrateApplicantData(ApplicantData applicantData) throws Exception {
    JsonNode rootJsonNode = OBJECT_MAPPER.readTree(applicantData.asJsonString());

    for (JsonNode node : rootJsonNode.findParents(FILE_KEY_PROPERTY)) {
      if (!node.has(FILE_KEY_LIST_PROPERTY)) {
        JsonNode fileKeyNode = node.get(FILE_KEY_PROPERTY);
        // The first putArray() adds an empty array node on the "file_key"list" property, then
        // .add() adds the string value node that we got from the "file_key" property to that array.
        ((ObjectNode) node).putArray(FILE_KEY_LIST_PROPERTY).add(fileKeyNode);
      }
    }
    return new ApplicantData(rootJsonNode.toString(), applicantData.getApplicant());
  }
}
