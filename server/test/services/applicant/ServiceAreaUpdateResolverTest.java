package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static play.mvc.Results.ok;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import play.libs.ws.WSClient;
import play.routing.RoutingDsl;
import play.server.Server;
import repository.ResetPostgres;
import services.Path;
import services.applicant.question.Scalar;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.geo.esri.EsriClient;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
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
    // configure EsriClient instance for ServiceAreaUpdateResolver
    Config config = ConfigFactory.load();
    Clock clock = instanceOf(Clock.class);
    EsriServiceAreaValidationConfig esriServiceAreaValidationConfig =
        instanceOf(EsriServiceAreaValidationConfig.class);
    Server server =
        Server.forRouter(
            (components) ->
                RoutingDsl.fromComponents(components)
                    .GET("/query")
                    .routingTo(request -> ok().sendResource("esri/serviceAreaFeatures.json"))
                    .build());
    WSClient ws = play.test.WSTestClient.newClient(server.httpPort());
    EsriClient esriClient = new EsriClient(config, clock, esriServiceAreaValidationConfig, ws);
    serviceAreaUpdateResolver = instanceOf(ServiceAreaUpdateResolver.class);
    // set instance of esriClient for ServiceAreaUpdateResolver
    FieldUtils.writeField(serviceAreaUpdateResolver, "esriClient", esriClient, true);

    applicantData = new ApplicantData();
    pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.applicantAddress().getQuestionDefinition(), Optional.of(programId))
            .setAddressCorrectionEnabled(true);
    addressQuestion = testQuestionBank.applicantAddress().getQuestionDefinition();
    eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Seattle")),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .setEligibilityDefinition(eligibilityDef)
            .addQuestion(pqd)
            .build();

    block = new Block("id", blockDefinition, applicantData, Optional.empty());
  }

  @Test
  public void getServiceAreaUpdate() {
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
                "47.578374020558954")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LONGITUDE).toString(),
                "-122.3360380354971")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.WELL_KNOWN_ID).toString(),
                "4326")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREA).toString(),
                "Bloomington_NotInArea_1234,Seattle_Failed_4567")
            .build();

    CompletionStage<Optional<ServiceAreaUpdate>> serviceAreaUpdateFuture =
        serviceAreaUpdateResolver.getServiceAreaUpdate(block, updates);
    Optional<ServiceAreaUpdate> maybeServiceAreaUpdate =
        serviceAreaUpdateFuture.toCompletableFuture().join();
    assertThat(maybeServiceAreaUpdate.isPresent()).isTrue();
    ServiceAreaUpdate serviceAreaUpdate = maybeServiceAreaUpdate.get();
    assertEquals(
        serviceAreaUpdate.path(),
        Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREA));
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
    assertEquals(serviceAreaUpdate.value().get(0).getServiceAreaId(), seattle.getServiceAreaId());
    assertEquals(serviceAreaUpdate.value().get(0).getState(), seattle.getState());
    assertEquals(
        serviceAreaUpdate.value().get(1).getServiceAreaId(), bloomington.getServiceAreaId());
    assertEquals(serviceAreaUpdate.value().get(1).getState(), bloomington.getState());
    assertEquals(serviceAreaUpdate.value().get(1).getTimeStamp(), bloomington.getTimeStamp());
  }

  @Test
  public void getServiceAreaUpdate_noCorrectionEnabled() {
    ApplicantData applicantData = new ApplicantData();
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            testQuestionBank.applicantAddress().getQuestionDefinition(), Optional.of(programId));
    QuestionDefinition addressQuestion =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Moon")),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .setEligibilityDefinition(eligibilityDef)
            .addQuestion(pqd)
            .build();

    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());
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
                testQuestionBank.applicantAddress().getQuestionDefinition(), Optional.of(programId))
            .setAddressCorrectionEnabled(true);
    QuestionDefinition addressQuestion =
        testQuestionBank.applicantAddress().getQuestionDefinition();
    EligibilityDefinition eligibilityDef =
        EligibilityDefinition.builder()
            .setPredicate(
                PredicateDefinition.create(
                    PredicateExpressionNode.create(
                        LeafAddressServiceAreaExpressionNode.create(
                            addressQuestion.getId(), "Moon")),
                    PredicateAction.ELIGIBLE_BLOCK))
            .build();
    BlockDefinition blockDefinition =
        BlockDefinition.builder()
            .setId(1L)
            .setName("name")
            .setDescription("desc")
            .setEligibilityDefinition(eligibilityDef)
            .addQuestion(pqd)
            .build();

    Block block = new Block("id", blockDefinition, applicantData, Optional.empty());
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
                "47.578374020558954")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LONGITUDE).toString(),
                "-122.3360380354971")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.WELL_KNOWN_ID).toString(),
                "4326")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREA).toString(),
                "Bloomington_NotInArea_1234,Seattle_Failed_4567")
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
    assertThat(maybeServiceAreaUpdate.isEmpty()).isTrue();
  }

  @Test
  public void getServiceAreaUpdate_no_new_areas_to_validate() {
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
                "47.578374020558954")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.LONGITUDE).toString(),
                "-122.3360380354971")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.WELL_KNOWN_ID).toString(),
                "4326")
            .put(
                Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREA).toString(),
                "Bloomington_NotInArea_1234,Seattle_InArea_4567")
            .build();

    CompletionStage<Optional<ServiceAreaUpdate>> serviceAreaUpdateFuture =
        serviceAreaUpdateResolver.getServiceAreaUpdate(block, updates);
    Optional<ServiceAreaUpdate> maybeServiceAreaUpdate =
        serviceAreaUpdateFuture.toCompletableFuture().join();
    assertThat(maybeServiceAreaUpdate.isPresent()).isTrue();
    ServiceAreaUpdate serviceAreaUpdate = maybeServiceAreaUpdate.get();
    assertEquals(
        serviceAreaUpdate.path(),
        Path.create("applicant.applicant_address").join(Scalar.SERVICE_AREA));
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
    assertEquals(
        serviceAreaUpdate.value().get(0).getServiceAreaId(), bloomington.getServiceAreaId());
    assertEquals(serviceAreaUpdate.value().get(0).getState(), bloomington.getState());
    assertEquals(serviceAreaUpdate.value().get(0).getTimeStamp(), bloomington.getTimeStamp());
    assertEquals(serviceAreaUpdate.value().get(1).getServiceAreaId(), seattle.getServiceAreaId());
    assertEquals(serviceAreaUpdate.value().get(1).getState(), seattle.getState());
    assertEquals(serviceAreaUpdate.value().get(1).getTimeStamp(), seattle.getTimeStamp());
  }
}
