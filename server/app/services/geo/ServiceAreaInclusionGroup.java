package services.geo;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains methods for serializing and deserializing stored values for {@link
 * ServiceAreaInclusion}.
 */
public final class ServiceAreaInclusionGroup {
  private static final Logger logger = LoggerFactory.getLogger(ServiceAreaInclusionGroup.class);

  private ServiceAreaInclusionGroup() {}

  /**
   * Takes a serialized string from {@link ApplicantData} of service areas and transforms them into
   * a list of {@link ServiceAreaInclusion}
   */
  public static ImmutableList<ServiceAreaInclusion> deserialize(String serviceAreas) {
    ImmutableList.Builder<ServiceAreaInclusion> listBuilder = ImmutableList.builder();
    for (String serviceAreaString : Splitter.on(",").split(serviceAreas)) {
      List<String> serviceAreaParts = Splitter.on("_").splitToList(serviceAreaString);
      try {
        listBuilder.add(
            ServiceAreaInclusion.builder()
                .setServiceAreaId(serviceAreaParts.get(0))
                .setState(ServiceAreaState.getEnumFromSerializedFormat(serviceAreaParts.get(1)))
                .setTimeStamp(Long.parseLong(serviceAreaParts.get(2)))
                .build());
      } catch (Exception e) {
        logger.error("Error while deserializing service area string {}", serviceAreaString);
        throw e;
      }
    }
    return listBuilder.build();
  }

  /**
   * Takes a list of {@link ServiceAreaInclusion}s and transforms them into a string to store in
   * {@link ApplicantData}.
   */
  public static String serialize(ImmutableList<ServiceAreaInclusion> serviceAreaInclusionGroup) {
    return serviceAreaInclusionGroup.stream()
        .map(
            (area) ->
                area.getServiceAreaId()
                    + "_"
                    + area.getState().getSerializationFormat()
                    + "_"
                    + area.getTimeStamp())
        .collect(Collectors.joining(","));
  }
}
