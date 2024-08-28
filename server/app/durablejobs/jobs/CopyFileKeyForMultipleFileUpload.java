package durablejobs.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import durablejobs.DurableJob;
import io.ebean.QueryIterator;
import java.util.Locale;
import models.ApplicantModel;
import models.ApplicationModel;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;
import repository.ApplicationRepository;
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
  private final AccountRepository accountRepository;
  private final ApplicationRepository applicationRepository;

  public CopyFileKeyForMultipleFileUpload(
      PersistedDurableJobModel persistedDurableJobModel,
      AccountRepository accountRepository,
      ApplicationRepository applicationRepository) {
    this.persistedDurableJobModel = persistedDurableJobModel;
    this.accountRepository = accountRepository;
    this.applicationRepository = applicationRepository;
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    LOGGER.atInfo().log("Copying file keys for applicants.");

    try (QueryIterator<ApplicantModel> applicants = accountRepository.streamAllApplicants()) {
      while (applicants.hasNext()) {
        try {
          ApplicantModel applicant = applicants.next();
          ApplicantData migratedData = migrateApplicantData(applicant.getApplicantData());
          applicant.setApplicantData(migratedData);
          applicant.save();
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
        }
      }
    }

    LOGGER.atInfo().log("Copying file keys for applications.");

    try (QueryIterator<ApplicationModel> applications =
        applicationRepository.streamAllApplications()) {
      while (applications.hasNext()) {
        try {
          ApplicationModel application = applications.next();
          application.setApplicantData(migrateApplicantData(application.getApplicantData()));
          application.save();
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
        }
      }
    }

    LOGGER.atInfo().log("Finished copying file keys.");
  }

  @VisibleForTesting
  ApplicantData migrateApplicantData(ApplicantData applicantData) throws Exception {
    JsonNode rootJsonNode = OBJECT_MAPPER.readTree(applicantData.asJsonString());

    for (JsonNode node : rootJsonNode.findParents(FILE_KEY_PROPERTY)) {
      if (!node.has(FILE_KEY_LIST_PROPERTY)) {
        JsonNode fileKeyNode = node.get(FILE_KEY_PROPERTY);
        ((ObjectNode) node).putArray(FILE_KEY_LIST_PROPERTY).add(fileKeyNode);
      }
    }
    return new ApplicantData(rootJsonNode.toString(), applicantData.getApplicant());
  }
}
