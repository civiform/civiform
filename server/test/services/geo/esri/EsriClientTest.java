package services.geo.esri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

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

public class EsriClientTest {
  private EsriTestHelper helper;

  @After
  public void tearDown() throws IOException {
    helper.stopServer();
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
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.IN_AREA, area.get().getState());
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
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.NOT_IN_AREA, area.get().getState());
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
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.NOT_IN_AREA, area.get().getState());
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
    assertEquals("Seattle", area.get().getServiceAreaId());
    assertEquals(ServiceAreaState.FAILED, area.get().getState());
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
    // first item is guaranteed to be here since the response is taken from  JSON file
    Optional<AddressSuggestion> addressSuggestion = suggestions.stream().findFirst();
    assertThat(addressSuggestion.isPresent()).isTrue();
    String street = addressSuggestion.get().getAddress().getStreet();
    assertEquals("Address In Area", street);
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

    assertEquals(address.getStreet(), originalAddress.getStreet());
    assertEquals(address.getLine2(), originalAddress.getLine2());
    assertEquals(address.getCity(), originalAddress.getCity());
    assertEquals(address.getState(), originalAddress.getState());
    assertEquals(address.getZip(), originalAddress.getZip());
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
    assertEquals(0, suggestions.size());
    assertEquals(address, group.getOriginalAddress());
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
    assertEquals(0, suggestions.size());
    assertEquals(0, group.getWellKnownId());
    assertEquals(address, group.getOriginalAddress());
  }
}
