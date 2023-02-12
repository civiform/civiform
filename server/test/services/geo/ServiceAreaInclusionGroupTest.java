package services.geo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ServiceAreaInclusionGroupTest {
  @Test
  public void deserialize() {
    ImmutableList<ServiceAreaInclusion> serviceAreaInclusionList = ServiceAreaInclusionGroup.deserialize("bloomington_Failed_1234,king-county_InArea_2222,seattle_InArea_5678,Arkansas_NotInArea_8765");
    System.out.println(serviceAreaInclusionList);
    assertEquals("bloomington", serviceAreaInclusionList.get(0).getServiceAreaId());
    assertEquals(ServiceAreaState.FAILED, serviceAreaInclusionList.get(0).getState());
    assertEquals(1234, serviceAreaInclusionList.get(0).getTimeStamp());
  }

  @Test
  public void serialize() {
    ImmutableList.Builder<ServiceAreaInclusion> listBuilder = ImmutableList.builder();
    listBuilder
        .add(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("bloomington")
                .setState(ServiceAreaState.FAILED)
                .setTimeStamp(1234).build())
        .add(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("king-county")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(2222).build())
        .add(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("seattle")
                .setState(ServiceAreaState.IN_AREA)
                .setTimeStamp(5678).build())
        .add(
            ServiceAreaInclusion.builder()
                .setServiceAreaId("Arkansas")
                .setState(ServiceAreaState.NOT_IN_AREA)
                .setTimeStamp(8765).build());
    String serialized = ServiceAreaInclusionGroup.serialize(listBuilder.build());
    assertEquals("bloomington_Failed_1234,king-county_InArea_2222,seattle_InArea_5678,Arkansas_NotInArea_8765", serialized);
  }
}
