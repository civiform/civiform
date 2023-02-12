package services.geo;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * Contains methods for serializing and deserializing stored values for {@link ServiceAreaInclusion}.
 */
public final class ServiceAreaInclusionGroup {
  private ServiceAreaInclusionGroup() {}

  public static ImmutableList<ServiceAreaInclusion> deserialize(String serviceAreas) {
    ImmutableList.Builder<ServiceAreaInclusion> listBuilder = ImmutableList.builder();
    for (String serviceAreaString : Splitter.on(",").split(serviceAreas)) {
      List<String> serviceAreaParts = Splitter.on("_").splitToList(serviceAreaString);
      listBuilder.add(
        ServiceAreaInclusion.builder()
            .setServiceAreaId(serviceAreaParts.get(0))
            .setState(ServiceAreaState.getEnumFromSerializedFormat(serviceAreaParts.get(1)))
            .setTimeStamp(Long.parseLong(serviceAreaParts.get(2)))
            .build());
    }
    return listBuilder.build();
  }

  public static String serialize(ImmutableList<ServiceAreaInclusion> serviceAreaInclusionGroup) {
    return serviceAreaInclusionGroup.stream().map((area) -> area.getServiceAreaId() + "_" + area.getState().getSerializationFormat() + "_" + area.getTimeStamp()).collect(Collectors.joining(","));
  }
}
