package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.Path;
import services.applicant.question.Scalar;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.Operator;
import services.program.predicate.PredicateAction;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;
import support.TestQuestionBank;

public class ServiceAreaUpdateResolverTest extends ResetPostgres {
  private ServiceAreaUpdateResolver serviceAreaUpdateResolver;
  private ApplicantData applicantData;
  private long programId = 5L;
  private ProgramQuestionDefinition pqd;
  private QuestionDefinition addressQuestion;
  private EligibilityDefinition eligibilityDef;
  private BlockDefinition blockDefinition;
  private Block block;

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Before
  public void setUp() throws Exception {
    serviceAreaUpdateResolver = instanceOf(ServiceAreaUpdateResolver.class);

    applicantData = new ApplicantData();
    pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.addressApplicantAddress().getQuestionDefinition(),
                Optional.of(programId))
            .setAddressCorrectionEnabled(true);
    addressQuestion = testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Seattle", Operator.IN_SERVICE_AREA)),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .setLocalizedName(LocalizedStrings.withDefaultValue("name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("desc"))
            .setEligibilityDefinition(eligibilityDef)
            .addQuestion(pqd)
            .build();

    block = new Block("id", blockDefinition, new ApplicantModel(), applicantData, Optional.empty());
  }

  @Test
  public void getServiceAreaUpdate() {
    Path rootPath = Path.create("applicant.applicant_address");
    Path serviceAreaPath = rootPath.join(Scalar.SERVICE_AREAS).asArrayElement();
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(rootPath.join(Scalar.STREET).toString(), "555 E 5th St.")
            .put(rootPath.join(Scalar.CITY).toString(), "City")
            .put(rootPath.join(Scalar.STATE).toString(), "State")
            .put(rootPath.join(Scalar.ZIP).toString(), "55555")
            .put(
                rootPath.join(Scalar.CORRECTED).toString(),
                CorrectedAddressState.CORRECTED.getSerializationFormat())
            .put(rootPath.join(Scalar.LATITUDE).toString(), "100.0")
            .put(rootPath.join(Scalar.LONGITUDE).toString(), "-100.0")
            .put(rootPath.join(Scalar.WELL_KNOWN_ID).toString(), "4326")
            .put(serviceAreaPath.atIndex(0).join(Scalar.SERVICE_AREA_ID).toString(), "Bloomington")
            .put(
                serviceAreaPath.atIndex(0).join(Scalar.SERVICE_AREA_STATE).toString(),
                ServiceAreaState.NOT_IN_AREA.getSerializationFormat())
            .put(serviceAreaPath.atIndex(0).join(Scalar.TIMESTAMP).toString(), "1234")
            .put(serviceAreaPath.atIndex(1).join(Scalar.SERVICE_AREA_ID).toString(), "Seattle")
            .put(
                serviceAreaPath.atIndex(1).join(Scalar.SERVICE_AREA_STATE).toString(),
                ServiceAreaState.FAILED.getSerializationFormat())
            .put(serviceAreaPath.atIndex(1).join(Scalar.TIMESTAMP).toString(), "4567")
            .build();

    CompletionStage<Optional<ServiceAreaUpdate>> serviceAreaUpdateFuture =
        serviceAreaUpdateResolver.getServiceAreaUpdate(block, updates);
    Optional<ServiceAreaUpdate> maybeServiceAreaUpdate =
        serviceAreaUpdateFuture.toCompletableFuture().join();
    assertThat(maybeServiceAreaUpdate.isPresent()).isTrue();
    ServiceAreaUpdate serviceAreaUpdate = maybeServiceAreaUpdate.get();
    assertThat(serviceAreaUpdate.path())
        .isEqualTo(Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREAS));
    ServiceAreaInclusion bloomington =
        ServiceAreaInclusion.builder()
            .setServiceAreaId("Bloomington")
            .setState(ServiceAreaState.NOT_IN_AREA)
            .setTimeStamp(1234)
            .build();
    ServiceAreaInclusion seattle =
        ServiceAreaInclusion.builder()
            .setServiceAreaId("Seattle")
            .setState(ServiceAreaState.IN_AREA)
            .setTimeStamp(1234)
            .build();
    assertThat(serviceAreaUpdate.value().get(0).getServiceAreaId())
        .isEqualTo(seattle.getServiceAreaId());
    assertThat(serviceAreaUpdate.value().get(0).getState()).isEqualTo(seattle.getState());
    assertThat(serviceAreaUpdate.value().get(1).getServiceAreaId())
        .isEqualTo(bloomington.getServiceAreaId());
    assertThat(serviceAreaUpdate.value().get(1).getState()).isEqualTo(bloomington.getState());
    assertThat(serviceAreaUpdate.value().get(1).getTimeStamp())
        .isEqualTo(bloomington.getTimeStamp());
  }

  @Test
  public void getServiceAreaUpdate_noCorrectionEnabled() {
    ApplicantData applicantData = new ApplicantData();
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            testQuestionBank.addressApplicantAddress().getQuestionDefinition(),
            Optional.of(programId));
    QuestionDefinition addressQuestion =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Moon", Operator.IN_SERVICE_AREA)),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .setLocalizedName(LocalizedStrings.withDefaultValue("name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("desc"))
            .setEligibilityDefinition(eligibilityDef)
            .addQuestion(pqd)
            .build();

    Block block =
        new Block("id", blockDefinition, new ApplicantModel(), applicantData, Optional.empty());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(
                Path.create("applicant.applicant_address").join(Scalar.STREET).toString(),
                "555 E 5th St.")
            .put(Path.create("applicant.applicant_address").join(Scalar.CITY).toString(), "City")
            .put(Path.create("applicant.applicant_address").join(Scalar.STATE).toString(), "State")
            .put(Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(), "55555")
            .build();

    CompletionStage<Optional<ServiceAreaUpdate>> serviceAreaUpdateFuture =
        serviceAreaUpdateResolver.getServiceAreaUpdate(block, updates);
    Optional<ServiceAreaUpdate> maybeServiceAreaUpdate =
        serviceAreaUpdateFuture.toCompletableFuture().join();
    assertThat(maybeServiceAreaUpdate.isEmpty()).isTrue();
  }

  @Test
  public void getServiceAreaUpdate_noOptions() {
    ApplicantData applicantData = new ApplicantData();
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.addressApplicantAddress().getQuestionDefinition(),
                Optional.of(programId))
            .setAddressCorrectionEnabled(true);
    QuestionDefinition addressQuestion =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Moon", Operator.IN_SERVICE_AREA)),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .setLocalizedName(LocalizedStrings.withDefaultValue("name"))
            .setLocalizedDescription(LocalizedStrings.withDefaultValue("desc"))
            .setEligibilityDefinition(eligibilityDef)
            .addQuestion(pqd)
            .build();

    Block block =
        new Block("id", blockDefinition, new ApplicantModel(), applicantData, Optional.empty());
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(
                Path.create("applicant.applicant_address").join(Scalar.STREET).toString(),
                "555 E 5th St.")
            .put(Path.create("applicant.applicant_address").join(Scalar.CITY).toString(), "City")
            .put(Path.create("applicant.applicant_address").join(Scalar.STATE).toString(), "State")
            .put(Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(), "55555")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.CORRECTED).toString(),
                CorrectedAddressState.CORRECTED.getSerializationFormat())
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LATITUDE).toString(),
                "101.0")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LONGITUDE).toString(),
                "-101.0")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.WELL_KNOWN_ID).toString(),
                "4326")
            .put(
                Path.create("applicant.applicant_address.service_area[0].service_area_id")
                    .toString(),
                "Bloomington")
            .put(
                Path.create("applicant.applicant_address.service_area[0].state").toString(),
                "NotInArea")
            .put(
                Path.create("applicant.applicant_address.service_area[0].timestamp").toString(),
                "1234")
            .put(
                Path.create("applicant.applicant_address.service_area[1].service_area_id")
                    .toString(),
                "Seattle")
            .put(
                Path.create("applicant.applicant_address.service_area[1].state").toString(),
                "Failed")
            .put(
                Path.create("applicant.applicant_address.service_area[1].timestamp").toString(),
                "4567")
            .build();

    CompletionStage<Optional<ServiceAreaUpdate>> serviceAreaUpdateFuture =
        serviceAreaUpdateResolver.getServiceAreaUpdate(block, updates);
    Optional<ServiceAreaUpdate> maybeServiceAreaUpdate =
        serviceAreaUpdateFuture.toCompletableFuture().join();
    assertThat(maybeServiceAreaUpdate.isEmpty()).isTrue();
  }

  @Test
  public void getServiceAreaUpdate_noCorrectedAddress() {
    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(
                Path.create("applicant.applicant_address").join(Scalar.STREET).toString(),
                "555 E 5th St.")
            .put(Path.create("applicant.applicant_address").join(Scalar.CITY).toString(), "City")
            .put(Path.create("applicant.applicant_address").join(Scalar.STATE).toString(), "State")
            .put(Path.create("applicant.applicant_address").join(Scalar.ZIP).toString(), "55555")
            .build();

    CompletionStage<Optional<ServiceAreaUpdate>> serviceAreaUpdateFuture =
        serviceAreaUpdateResolver.getServiceAreaUpdate(block, updates);
    Optional<ServiceAreaUpdate> maybeServiceAreaUpdate =
        serviceAreaUpdateFuture.toCompletableFuture().join();
    assertThat(maybeServiceAreaUpdate.get().value().isEmpty()).isTrue();
  }

  @Test
  public void getServiceAreaUpdate_no_new_areas_to_validate() {
    Path rootPath = Path.create("applicant.applicant_address");
    Path serviceAreaPath = rootPath.join(Scalar.SERVICE_AREAS).asArrayElement();

    ImmutableMap<String, String> updates =
        ImmutableMap.<String, String>builder()
            .put(rootPath.join(Scalar.STREET).toString(), "555 E 5th St.")
            .put(rootPath.join(Scalar.CITY).toString(), "City")
            .put(rootPath.join(Scalar.STATE).toString(), "State")
            .put(rootPath.join(Scalar.ZIP).toString(), "55555")
            .put(
                rootPath.join(Scalar.CORRECTED).toString(),
                CorrectedAddressState.CORRECTED.getSerializationFormat())
            .put(rootPath.join(Scalar.LATITUDE).toString(), "100.0")
            .put(rootPath.join(Scalar.LONGITUDE).toString(), "-100.0")
            .put(rootPath.join(Scalar.WELL_KNOWN_ID).toString(), "4326")
            .put(serviceAreaPath.atIndex(0).join(Scalar.SERVICE_AREA_ID).toString(), "Bloomington")
            .put(
                serviceAreaPath.atIndex(0).join(Scalar.SERVICE_AREA_STATE).toString(),
                ServiceAreaState.NOT_IN_AREA.getSerializationFormat())
            .put(serviceAreaPath.atIndex(0).join(Scalar.TIMESTAMP).toString(), "1234")
            .put(serviceAreaPath.atIndex(1).join(Scalar.SERVICE_AREA_ID).toString(), "Seattle")
            .put(
                serviceAreaPath.atIndex(1).join(Scalar.SERVICE_AREA_STATE).toString(),
                ServiceAreaState.IN_AREA.getSerializationFormat())
            .put(serviceAreaPath.atIndex(1).join(Scalar.TIMESTAMP).toString(), "4567")
            .build();

    CompletionStage<Optional<ServiceAreaUpdate>> serviceAreaUpdateFuture =
        serviceAreaUpdateResolver.getServiceAreaUpdate(block, updates);
    Optional<ServiceAreaUpdate> maybeServiceAreaUpdate =
        serviceAreaUpdateFuture.toCompletableFuture().join();
    assertThat(maybeServiceAreaUpdate.isPresent()).isTrue();
    ServiceAreaUpdate serviceAreaUpdate = maybeServiceAreaUpdate.get();
    assertThat(serviceAreaUpdate.path())
        .isEqualTo(Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREAS));
    ServiceAreaInclusion bloomington =
        ServiceAreaInclusion.builder()
            .setServiceAreaId("Bloomington")
            .setState(ServiceAreaState.NOT_IN_AREA)
            .setTimeStamp(1234)
            .build();
    ServiceAreaInclusion seattle =
        ServiceAreaInclusion.builder()
            .setServiceAreaId("Seattle")
            .setState(ServiceAreaState.IN_AREA)
            .setTimeStamp(4567)
            .build();
    assertThat(serviceAreaUpdate.value().get(0).getServiceAreaId())
        .isEqualTo(bloomington.getServiceAreaId());
    assertThat(serviceAreaUpdate.value().get(0).getState()).isEqualTo(bloomington.getState());
    assertThat(serviceAreaUpdate.value().get(0).getTimeStamp())
        .isEqualTo(bloomington.getTimeStamp());
    assertThat(serviceAreaUpdate.value().get(1).getServiceAreaId())
        .isEqualTo(seattle.getServiceAreaId());
    assertThat(serviceAreaUpdate.value().get(1).getState()).isEqualTo(seattle.getState());
    assertThat(serviceAreaUpdate.value().get(1).getTimeStamp()).isEqualTo(seattle.getTimeStamp());
  }
}
