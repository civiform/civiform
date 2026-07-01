package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;
import static services.geo.esri.EsriClient.ESRI_LOOKUP_COUNT;
import static services.geo.esri.EsriClient.LOOKUP_LABEL_FULL_ADDRESS;
import static services.geo.esri.EsriClient.LOOKUP_LABEL_NO_SUGGESTIONS;
import static services.geo.esri.EsriClient.LOOKUP_LABEL_PARSE_FAILURE;
import static services.geo.esri.EsriClient.LOOKUP_LABEL_PARTIAL_ADDRESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.After;
import org.junit.Test;
import play.test.WithApplication;
import services.Address;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.geo.esri.EsriTestHelper.TestType;
import services.geo.esri.models.Attributes;

public class EsriClientTest extends WithApplication {
  public static final Address ADDRESS =
      Address.builder()
          .setStreet("380 New York St")
          .setLine2("")
          .setCity("Redlands")
          .setState("CA")
          .setZip("92373")
          .build();
  private EsriTestHelper helper;

  @After
  public void tearDown() throws IOException {
    if (helper != null) {
      helper.stopServer();
    }
  }

  @Test
  public void getServiceAreaInclusionGroup() throws Exception {
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION, instanceOf(ObjectMapper.class));
    ImmutableList<ServiceAreaInclusion> inclusionList =
        helper
            .getClient()
            .getServiceAreaInclusionGroup(
                EsriTestHelper.ESRI_SERVICE_AREA_VALIDATION_OPTION, EsriTestHelper.LOCATION)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertThat(area.get().getServiceAreaId()).isEqualTo("Seattle");
    assertThat(area.get().getState()).isEqualTo(ServiceAreaState.IN_AREA);
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }

  @Test
  public void getServiceAreaInclusionGroupAreaNotIncluded() throws Exception {
    helper =
        new EsriTestHelper(
            TestType.SERVICE_AREA_VALIDATION_NOT_INCLUDED, instanceOf(ObjectMapper.class));
    ImmutableList<ServiceAreaInclusion> inclusionList =
        helper
            .getClient()
            .getServiceAreaInclusionGroup(
                EsriTestHelper.ESRI_SERVICE_AREA_VALIDATION_OPTION, EsriTestHelper.LOCATION)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertThat(area.get().getServiceAreaId()).isEqualTo("Seattle");
    assertThat(area.get().getState()).isEqualTo(ServiceAreaState.NOT_IN_AREA);
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }

  @Test
  public void getServiceAreaInclusionGroupNoFeatures() throws Exception {
    helper =
        new EsriTestHelper(
            TestType.SERVICE_AREA_VALIDATION_NO_FEATURES, instanceOf(ObjectMapper.class));
    ImmutableList<ServiceAreaInclusion> inclusionList =
        helper
            .getClient()
            .getServiceAreaInclusionGroup(
                EsriTestHelper.ESRI_SERVICE_AREA_VALIDATION_OPTION, EsriTestHelper.LOCATION)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertThat(area.get().getServiceAreaId()).isEqualTo("Seattle");
    assertThat(area.get().getState()).isEqualTo(ServiceAreaState.NOT_IN_AREA);
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }

  @Test
  public void getServiceAreaInclusionGroupError() throws Exception {
    helper =
        new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION_ERROR, instanceOf(ObjectMapper.class));
    ImmutableList<ServiceAreaInclusion> inclusionList =
        helper
            .getClient()
            .getServiceAreaInclusionGroup(
                EsriTestHelper.ESRI_SERVICE_AREA_VALIDATION_OPTION, EsriTestHelper.LOCATION)
            .toCompletableFuture()
            .join();
    Optional<ServiceAreaInclusion> area = inclusionList.stream().findFirst();
    assertThat(area.isPresent()).isTrue();
    assertThat(area.get().getServiceAreaId()).isEqualTo("Seattle");
    assertThat(area.get().getState()).isEqualTo(ServiceAreaState.FAILED);
    assertThat(area.get().getTimeStamp()).isInstanceOf(Long.class);
  }

  @Test
  public void getAddressSuggestions() throws Exception {
    helper = new EsriTestHelper(TestType.STANDARD, instanceOf(ObjectMapper.class));
    double fullBefore = ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_FULL_ADDRESS).get();
    double partialBefore = ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_PARTIAL_ADDRESS).get();

    CompletionStage<AddressSuggestionGroup> group =
        helper.getClient().getAddressSuggestions(ADDRESS);
    ImmutableList<AddressSuggestion> suggestions =
        group.toCompletableFuture().join().getAddressSuggestions();
    // First item is guaranteed to be here since the response is taken from the JSON file.
    // This also tests that we are rejecting the responses that do not include a number
    // in the street address or any street address at all.
    assertThat(suggestions).hasSizeGreaterThan(0);
    Optional<AddressSuggestion> addressSuggestion = suggestions.stream().findFirst();
    String street = addressSuggestion.get().getAddress().getStreet();
    assertThat(street).isEqualTo("Address In Area");

    assertThat(ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_FULL_ADDRESS).get() - fullBefore)
        .isEqualTo(4.0);
    assertThat(ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_PARTIAL_ADDRESS).get() - partialBefore)
        .isEqualTo(1.0);
  }

  @Test
  public void getAddressSuggestionsIncludesOriginalAddress() throws Exception {
    helper = new EsriTestHelper(TestType.STANDARD, instanceOf(ObjectMapper.class));
    CompletionStage<AddressSuggestionGroup> group =
        helper.getClient().getAddressSuggestions(ADDRESS);
    Address originalAddress = group.toCompletableFuture().join().getOriginalAddress();

    assertThat(originalAddress.getStreet()).isEqualTo(ADDRESS.getStreet());
    assertThat(originalAddress.getLine2()).isEqualTo(ADDRESS.getLine2());
    assertThat(originalAddress.getCity()).isEqualTo(ADDRESS.getCity());
    assertThat(originalAddress.getState()).isEqualTo(ADDRESS.getState());
    assertThat(originalAddress.getZip()).isEqualTo(ADDRESS.getZip());
  }

  @Test
  public void getAddressSuggestionsWithNoCandidates() throws Exception {
    helper = new EsriTestHelper(TestType.NO_CANDIDATES, instanceOf(ObjectMapper.class));

    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(ADDRESS).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getOriginalAddress()).isEqualTo(ADDRESS);
  }

  @Test
  public void getAddressSuggestionsWithEmptyResponse() throws Exception {
    helper = new EsriTestHelper(TestType.EMPTY_RESPONSE, instanceOf(ObjectMapper.class));
    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(ADDRESS).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getWellKnownId()).isEqualTo(0);
    assertThat(group.getOriginalAddress()).isEqualTo(ADDRESS);
  }

  @Test
  public void getAddressSuggestionsWithEsriErrorResponse() {
    helper = new EsriTestHelper(TestType.ESRI_ERROR_RESPONSE, instanceOf(ObjectMapper.class));
    double before = ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_NO_SUGGESTIONS).get();

    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(ADDRESS).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getWellKnownId()).isEqualTo(0);
    assertThat(group.getOriginalAddress()).isEqualTo(ADDRESS);

    assertThat(ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_NO_SUGGESTIONS).get() - before).isEqualTo(1.0);
  }

  @Test
  public void getAddressSuggestionsWithParseFailure() {
    helper = new EsriTestHelper(TestType.PARSE_FAILURE, instanceOf(ObjectMapper.class));
    double before = ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_PARSE_FAILURE).get();

    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(ADDRESS).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getWellKnownId()).isEqualTo(0);
    assertThat(group.getOriginalAddress()).isEqualTo(ADDRESS);

    double after = ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_PARSE_FAILURE).get();
    assertThat(after - before).isEqualTo(1.0);
  }

  @Test
  public void getAddressSuggestionsWithError() throws Exception {
    helper = new EsriTestHelper(TestType.ERROR, instanceOf(ObjectMapper.class));
    double before = ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_NO_SUGGESTIONS).get();

    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(ADDRESS).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getWellKnownId()).isEqualTo(0);
    assertThat(group.getOriginalAddress()).isEqualTo(ADDRESS);

    assertThat(ESRI_LOOKUP_COUNT.labels(LOOKUP_LABEL_NO_SUGGESTIONS).get() - before).isEqualTo(1.0);
  }

  @Test
  public void verifyMappingAddressFromJsonAttributes_useRegionAbbrField() {
    Attributes attributes =
        new Attributes(
            "line2-expected", "street-expected", "city-expected", null, "WA", "11111-expected");

    runMapAddressAttributesJsonAndAssertResults(
        attributes, "street-expected", "line2-expected", "city-expected", "WA", "11111-expected");
  }

  @Test
  public void verifyMappingAddressFromJsonAttributes_useRegionField() {
    Attributes attributes =
        new Attributes(
            "line2-expected",
            "street-expected",
            "city-expected",
            "WA",
            "Washington",
            "11111-expected");

    runMapAddressAttributesJsonAndAssertResults(
        attributes, "street-expected", "line2-expected", "city-expected", "WA", "11111-expected");
  }

  @Test
  public void verifyMappingAddressFromJsonAttributes_bothRegionFieldsAreLongStrings() {
    Attributes attributes =
        new Attributes(
            "line2-expected",
            "street-expected",
            "city-expected",
            "Washington",
            "Washington",
            "11111-expected");

    runMapAddressAttributesJsonAndAssertResults(
        attributes, "street-expected", "line2-expected", "city-expected", "CA", "11111-expected");
  }

  @Test
  public void verifyMappingAddressFromJsonAttributes_useLine2AsEnteredIfNull() {
    Attributes attributes =
        new Attributes(
            null, "street-expected", "city-expected", "WA", "Washington", "11111-expected");

    runMapAddressAttributesJsonAndAssertResults(
        attributes, "street-expected", "line2-user", "city-expected", "WA", "11111-expected");
  }

  @Test
  public void verifyMappingAddressFromJsonAttributes_useLine2AsEnteredIfEmpty() {
    Attributes attributes =
        new Attributes(
            "", "street-expected", "city-expected", "WA", "Washington", "11111-expected");

    runMapAddressAttributesJsonAndAssertResults(
        attributes, "street-expected", "line2-user", "city-expected", "WA", "11111-expected");
  }

  private void runMapAddressAttributesJsonAndAssertResults(
      Attributes attributes,
      String streetExpected,
      String line2Expected,
      String cityExpected,
      String stateExpected,
      String zipExpected) {
    Address userEnteredAddress =
        Address.builder()
            .setStreet("street-user")
            .setLine2("line2-user")
            .setCity("city-user")
            .setState("CA")
            .setZip("11111-user")
            .build();

    Address result = EsriClient.mapAddressAttributesJson(attributes, userEnteredAddress);

    assertThat(result.getStreet()).isEqualTo(streetExpected);
    assertThat(result.getLine2()).isEqualTo(line2Expected);
    assertThat(result.getCity()).isEqualTo(cityExpected);
    assertThat(result.getState()).isEqualTo(stateExpected);
    assertThat(result.getZip()).isEqualTo(zipExpected);
  }
}
