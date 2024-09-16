package durablejobs.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ApplicantModel;
import models.ApplicationModel;
import models.JobType;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.AccountRepository;
import repository.ApplicationRepository;
import repository.ResetPostgres;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.Scalar;
import services.geo.ServiceAreaState;
import support.ProgramBuilder;

@RunWith(JUnitParamsRunner.class)
public class ConvertAddressServiceAreaToArrayJobTest extends ResetPostgres {
  AccountRepository accountRepository;
  ApplicationRepository applicationRepository;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    applicationRepository = instanceOf(ApplicationRepository.class);
  }

  static ImmutableList<Object[]> canConvertStringToArrayParameters() {
    return ImmutableList.of(
        new Object[] {"a_InArea_1", "a", ServiceAreaState.IN_AREA, 1L},
        new Object[] {"a_b_InArea_1", "a_b", ServiceAreaState.IN_AREA, 1L},
        new Object[] {"a_b_c_d_1_2-_1_InArea_1", "a_b_c_d_1_2-_1", ServiceAreaState.IN_AREA, 1L});
  }

  @Test
  @Parameters(method = "canConvertStringToArrayParameters")
  public void canConvertStringToArray(
      String value, String serviceAreaId, ServiceAreaState state, Long timestamp) {
    var row = ConvertAddressServiceAreaToArrayJob.buildServiceAreaArrayFromString(value);

    assertThat(row.getServiceAreaId()).isEqualTo(serviceAreaId);
    assertThat(row.getState()).isEqualTo(state);
    assertThat(row.getTimeStamp()).isEqualTo(timestamp);
  }

  @Test
  public void run_migrates_applicant_records() {
    String originalJson =
        """
        {
          "applicant": {
            "applicant_address": {
              "zip": "92373",
              "city": "Redlands",
              "state": "CA",
              "street": "Address In Area",
              "latitude": 100,
              "corrected": "Corrected",
              "longitude": -100,
              "updated_at": 1700000000000,
              "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
              "well_known_id": 4326,
              "program_updated_in": 1
            }
          }
        }
        """;

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.setApplicantData(new ApplicantData(originalJson, applicant));
    applicant.save();

    // act
    runJob();

    // assert
    ApplicantModel newApplicantModel =
        accountRepository.lookupApplicant(applicant.id).toCompletableFuture().join().get();
    ApplicantData newApplicationData = newApplicantModel.getApplicantData();

    assertServiceAreaValues(Path.create("applicant.applicant_address"), newApplicationData);
  }

  @Test
  public void run_migrates_applicant_records_recursive() {
    String originalJson =
        """
        {
          "applicant": {
            "enumerator_one": [
              {
                "applicant_address_child": {
                  "zip": "92373",
                  "city": "Redlands",
                  "state": "CA",
                  "street": "Address In Area",
                  "latitude": 100,
                  "corrected": "Corrected",
                  "longitude": -100,
                  "updated_at": 1726495903486,
                  "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
                  "well_known_id": 4326,
                  "program_updated_in": 2139
                },
                "updated_at": 1726495899280,
                "entity_name": "a",
                "program_updated_in": 2139
              },
              {
                "applicant_address_child": {
                  "zip": "92373",
                  "city": "Redlands",
                  "state": "CA",
                  "street": "Address In Area",
                  "latitude": 100,
                  "corrected": "Corrected",
                  "longitude": -100,
                  "updated_at": 1726495910796,
                  "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
                  "well_known_id": 4326,
                  "program_updated_in": 2139
                },
                "updated_at": 1726495899280,
                "entity_name": "b",
                "program_updated_in": 2139
              }
            ],
            "applicant_address": {
              "zip": "92373",
              "city": "Redlands",
              "state": "CA",
              "street": "Address In Area",
              "latitude": 100,
              "corrected": "Corrected",
              "longitude": -100,
              "updated_at": 1726495893773,
              "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
              "well_known_id": 4326,
              "program_updated_in": 2139
            }
          }
        }
        """;

    //
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.setApplicantData(new ApplicantData(originalJson, applicant));
    applicant.save();

    // act
    runJob();

    // assert
    ApplicantModel newApplicantModel =
        accountRepository.lookupApplicant(applicant.id).toCompletableFuture().join().get();
    ApplicantData newApplicationData = newApplicantModel.getApplicantData();

    assertServiceAreaValues(Path.create("applicant.applicant_address"), newApplicationData);
    assertServiceAreaValues(
        Path.create("applicant.enumerator_one[0].applicant_address_child"), newApplicationData);
    assertServiceAreaValues(
        Path.create("applicant.enumerator_one[1].applicant_address_child"), newApplicationData);
  }

  @Test
  public void run_migrates_application_records() {
    String originalJson =
        """
        {
          "applicant": {
            "applicant_address": {
              "zip": "92373",
              "city": "Redlands",
              "state": "CA",
              "street": "Address In Area",
              "latitude": 100,
              "corrected": "Corrected",
              "longitude": -100,
              "updated_at": 1700000000000,
              "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
              "well_known_id": 4326,
              "program_updated_in": 1
            }
          }
        }
        """;

    //
    ProgramModel program = ProgramBuilder.newActiveProgram("program1").build();

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.setApplicantData(new ApplicantData(originalJson, applicant));
    applicant.save();

    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    application.setApplicantData(applicant.getApplicantData());
    application.save();

    // act
    runJob();

    // assert
    ApplicationModel newApplication =
        applicationRepository.getApplication(application.id).toCompletableFuture().join().get();
    ApplicantData newApplicationData = newApplication.getApplicantData();
    assertServiceAreaValues(Path.create("applicant.applicant_address"), newApplicationData);
  }

  @Test
  public void run_migrates_application_records_recursive() {
    String originalJson =
        """
        {
          "applicant": {
            "enumerator_one": [
              {
                "applicant_address_child": {
                  "zip": "92373",
                  "city": "Redlands",
                  "state": "CA",
                  "street": "Address In Area",
                  "latitude": 100,
                  "corrected": "Corrected",
                  "longitude": -100,
                  "updated_at": 1726495903486,
                  "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
                  "well_known_id": 4326,
                  "program_updated_in": 2139
                },
                "updated_at": 1726495899280,
                "entity_name": "a",
                "program_updated_in": 2139
              },
              {
                "applicant_address_child": {
                  "zip": "92373",
                  "city": "Redlands",
                  "state": "CA",
                  "street": "Address In Area",
                  "latitude": 100,
                  "corrected": "Corrected",
                  "longitude": -100,
                  "updated_at": 1726495910796,
                  "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
                  "well_known_id": 4326,
                  "program_updated_in": 2139
                },
                "updated_at": 1726495899280,
                "entity_name": "b",
                "program_updated_in": 2139
              }
            ],
            "applicant_address": {
              "zip": "92373",
              "city": "Redlands",
              "state": "CA",
              "street": "Address In Area",
              "latitude": 100,
              "corrected": "Corrected",
              "longitude": -100,
              "updated_at": 1726495893773,
              "service_area": "Bloomington_NotInArea_1234,Seattle_InArea_4567",
              "well_known_id": 4326,
              "program_updated_in": 2139
            }
          }
        }
        """;

    //
    ProgramModel program = ProgramBuilder.newActiveProgram("program1").build();

    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.setApplicantData(new ApplicantData(originalJson, applicant));
    applicant.save();

    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    application.setApplicantData(applicant.getApplicantData());
    application.save();

    // act
    runJob();

    // assert
    ApplicationModel newApplication =
        applicationRepository.getApplication(application.id).toCompletableFuture().join().get();
    ApplicantData newApplicationData = newApplication.getApplicantData();
    assertServiceAreaValues(Path.create("applicant.applicant_address"), newApplicationData);
    assertServiceAreaValues(
        Path.create("applicant.enumerator_one[0].applicant_address_child"), newApplicationData);
    assertServiceAreaValues(
        Path.create("applicant.enumerator_one[1].applicant_address_child"), newApplicationData);
  }

  private static void runJob() {
    ConvertAddressServiceAreaToArrayJob job =
        new ConvertAddressServiceAreaToArrayJob(
            new PersistedDurableJobModel("fake-job", JobType.RUN_ONCE, Instant.now()));

    job.run();
  }

  private static void assertServiceAreaValues(Path rootPath, ApplicantData newApplicationData) {
    // Assert top service area keys are as expected
    assertThat(newApplicationData.hasPath(rootPath.join(Scalar.SERVICE_AREA))).isFalse();
    assertThat(newApplicationData.hasPath(rootPath.join(Scalar.SERVICE_AREAS))).isTrue();

    // Assert expected child keys exist
    Path serviceAreaPath = rootPath.join(Scalar.SERVICE_AREAS).asArrayElement();
    assertThat(
            newApplicationData
                .readString(serviceAreaPath.atIndex(0).join(Scalar.SERVICE_AREA_ID))
                .isPresent())
        .isTrue();
    assertThat(
            newApplicationData
                .readString(serviceAreaPath.atIndex(0).join(Scalar.SERVICE_AREA_STATE))
                .isPresent())
        .isTrue();
    assertThat(
            newApplicationData
                .readString(serviceAreaPath.atIndex(0).join(Scalar.TIMESTAMP))
                .isPresent())
        .isTrue();
    assertThat(
            newApplicationData
                .readString(serviceAreaPath.atIndex(1).join(Scalar.SERVICE_AREA_ID))
                .isPresent())
        .isTrue();
    assertThat(
            newApplicationData
                .readString(serviceAreaPath.atIndex(1).join(Scalar.SERVICE_AREA_STATE))
                .isPresent())
        .isTrue();
    assertThat(
            newApplicationData
                .readString(serviceAreaPath.atIndex(1).join(Scalar.TIMESTAMP))
                .isPresent())
        .isTrue();

    // Assert getting the typed list returns expected values
    var serviceAreas = newApplicationData.readServiceAreaList(serviceAreaPath);
    assertThat(serviceAreas.isPresent()).isTrue();
    assertThat(serviceAreas.get().size()).isEqualTo(2);

    var bloomington =
        serviceAreas.get().stream()
            .filter(x -> x.getServiceAreaId().equals("Bloomington"))
            .findFirst();
    assertThat(bloomington.isPresent()).isTrue();
    assertThat(bloomington.get().getState()).isEqualTo(ServiceAreaState.NOT_IN_AREA);
    assertThat(bloomington.get().getTimeStamp()).isEqualTo(1234);

    var seattle =
        serviceAreas.get().stream().filter(x -> x.getServiceAreaId().equals("Seattle")).findFirst();
    assertThat(seattle.isPresent()).isTrue();
    assertThat(seattle.get().getState()).isEqualTo(ServiceAreaState.IN_AREA);
    assertThat(seattle.get().getTimeStamp()).isEqualTo(4567);
  }
}
