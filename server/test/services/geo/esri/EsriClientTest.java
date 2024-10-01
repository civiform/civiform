package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.After;
import org.junit.Test;
import services.Address;
import services.geo.AddressSuggestion;
import services.geo.AddressSuggestionGroup;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.geo.esri.EsriTestHelper.TestType;
import services.geo.esri.models.Attributes;

public class EsriClientTest {
  private EsriTestHelper helper;

  @After
  public void tearDown() throws IOException {
    if (helper != null) {
      helper.stopServer();
    }
  }

  @Test
  public void getServiceAreaInclusionGroup() throws Exception {
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION);
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
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION_NOT_INCLUDED);
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
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION_NO_FEATURES);
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
    helper = new EsriTestHelper(TestType.SERVICE_AREA_VALIDATION_ERROR);
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
    helper = new EsriTestHelper(TestType.STANDARD);
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    CompletionStage<AddressSuggestionGroup> group =
        helper.getClient().getAddressSuggestions(address);
    ImmutableList<AddressSuggestion> suggestions =
        group.toCompletableFuture().join().getAddressSuggestions();
    // First item is guaranteed to be here since the response is taken from the JSON file.
    // This also tests that we are rejecting the responses that do not include a number
    // in the street address or any street address at all.
    assertThat(suggestions).hasSizeGreaterThan(0);
    Optional<AddressSuggestion> addressSuggestion = suggestions.stream().findFirst();
    assertThat(addressSuggestion.isPresent()).isTrue();
    String street = addressSuggestion.get().getAddress().getStreet();
    assertThat(street).isEqualTo("Address In Area");
  }

  @Test
  public void getAddressSuggestionsIncludesOriginalAddress() throws Exception {
    helper = new EsriTestHelper(TestType.STANDARD);
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    CompletionStage<AddressSuggestionGroup> group =
        helper.getClient().getAddressSuggestions(address);
    Address originalAddress = group.toCompletableFuture().join().getOriginalAddress();

    assertThat(originalAddress.getStreet()).isEqualTo(address.getStreet());
    assertThat(originalAddress.getLine2()).isEqualTo(address.getLine2());
    assertThat(originalAddress.getCity()).isEqualTo(address.getCity());
    assertThat(originalAddress.getState()).isEqualTo(address.getState());
    assertThat(originalAddress.getZip()).isEqualTo(address.getZip());
  }

  @Test
  public void getAddressSuggestionsWithNoCandidates() throws Exception {
    helper = new EsriTestHelper(TestType.NO_CANDIDATES);
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(address).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getOriginalAddress()).isEqualTo(address);
  }

  @Test
  public void getAddressSuggestionsWithEmptyResponse() throws Exception {
    helper = new EsriTestHelper(TestType.EMPTY_RESPONSE);
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(address).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getWellKnownId()).isEqualTo(0);
    assertThat(group.getOriginalAddress()).isEqualTo(address);
  }

  @Test
  public void getAddressSuggestionsWithError() throws Exception {
    helper = new EsriTestHelper(TestType.ERROR);
    Address address =
        Address.builder()
            .setStreet("380 New York St")
            .setLine2("")
            .setCity("Redlands")
            .setState("CA")
            .setZip("92373")
            .build();

    AddressSuggestionGroup group =
        helper.getClient().getAddressSuggestions(address).toCompletableFuture().join();
    ImmutableList<AddressSuggestion> suggestions = group.getAddressSuggestions();
    assertThat(suggestions).isEmpty();
    assertThat(group.getWellKnownId()).isEqualTo(0);
    assertThat(group.getOriginalAddress()).isEqualTo(address);
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
