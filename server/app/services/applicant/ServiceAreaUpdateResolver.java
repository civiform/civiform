package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.geo.AddressLocation;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaState;
import services.geo.esri.EsriClient;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.geo.esri.EsriServiceAreaValidationOption;

/** Contains methods for resolving {@link ServiceAreaUpdate}s to update applicant data. */
final class ServiceAreaUpdateResolver {
  private final Logger logger = LoggerFactory.getLogger(ServiceAreaUpdateResolver.class);
  private final EsriClient esriClient;
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;

  /**
   * Regex matcher used to extract service_area index number Match examples:
   * <li>applicant.question_name.service_area[0].service_area_id
   * <li>applicant.question_name.service_area[0].state
   * <li>applicant.question_name.service_area[0].timestamp
   */
  private final Pattern pathIndexPattern =
      Pattern.compile(
          String.format(
              "applicant.[\\w]+.%s\\[(?<index>[0-9]+)\\].(%s|%s|%s)",
              Scalar.SERVICE_AREA.toDisplayString(),
              Scalar.SERVICE_AREA_ID.toDisplayString(),
              Scalar.SERVICE_AREA_STATE.toDisplayString(),
              Scalar.SERVICE_AREA_TIMESTAMP.toDisplayString()));

  @Inject
  public ServiceAreaUpdateResolver(
      EsriClient esriClient,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      ClassLoaderExecutionContext classLoaderExecutionContext) {
    this.esriClient = checkNotNull(esriClient);
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
  }

  /**
   * Get a {@link ServiceAreaUpdate} for a {@link Block} with the provided update map.
   *
   * <p>Checks to see if the specified service areas to validate are already validated, and if so
   * returns without re-validating.
   *
   * <p>Returns empty under the following conditions:
   *
   * <ul>
   *   <li>The block does not contain and address question with address correction enabled.
   *   <li>There are no {@link EsriServiceAreaValidationOption}s corresponding to service area ideas
   *       configured for eligibility.
   *   <li>The address has not been corrected.
   * </ul>
   */
  public CompletionStage<Optional<ServiceAreaUpdate>> getServiceAreaUpdate(
      Block block, ImmutableMap<String, String> updateMap) {
    Optional<ImmutableList<EsriServiceAreaValidationOption>> maybeOptions =
        esriServiceAreaValidationConfig.getOptionsByServiceAreaIds(
            block.getLeafAddressNodeServiceAreaIds().get());
    Optional<ApplicantQuestion> maybeAddressQuestion =
        block.getAddressQuestionWithCorrectionEnabled();

    if (maybeAddressQuestion.isEmpty() || maybeOptions.isEmpty()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    ImmutableList<EsriServiceAreaValidationOption> serviceAreaOptions = maybeOptions.get();
    ApplicantQuestion addressQuestion = maybeAddressQuestion.get();
    Boolean hasCorrectedAddress = doesUpdateContainCorrectedAddress(addressQuestion, updateMap);

    if (!hasCorrectedAddress) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    Path serviceAreaPath = addressQuestion.getContextualizedPath().join(Scalar.SERVICE_AREA);
    ImmutableList<ServiceAreaInclusion> existingServiceAreaInclusionGroup =
        getExistingServiceAreaInclusionGroup(serviceAreaPath, updateMap);

    // filter out the serviceAreaOptions that have already been validated for this address
    ImmutableList<EsriServiceAreaValidationOption> serviceAreaOptionsToValidate =
        serviceAreaOptions.stream()
            .filter(
                (option) ->
                    !option.isServiceAreaOptionInInclusionGroup(existingServiceAreaInclusionGroup))
            .collect(ImmutableList.toImmutableList());

    if (serviceAreaOptionsToValidate.size() == 0) {
      // return an update with the existing service areas
      return CompletableFuture.completedFuture(
          Optional.of(
              ServiceAreaUpdate.create(serviceAreaPath, existingServiceAreaInclusionGroup)));
    }

    AddressLocation addressLocation = getAddressLocationFromUpdates(addressQuestion, updateMap);

    ImmutableList<CompletionStage<ImmutableList<ServiceAreaInclusion>>>
        serviceAreaInclusionGroupFutures =
            serviceAreaOptionsToValidate.stream()
                .map(
                    (option) -> {
                      return esriClient.getServiceAreaInclusionGroup(option, addressLocation);
                    })
                .collect(ImmutableList.toImmutableList());

    return CompletableFuture.allOf(
            serviceAreaInclusionGroupFutures.toArray(
                new CompletableFuture[serviceAreaInclusionGroupFutures.size()]))
        .thenApplyAsync(
            (u) -> {
              ImmutableList<ServiceAreaInclusion> serviceAreaInclusionGroup =
                  serviceAreaInclusionGroupFutures.stream()
                      .map((future) -> future.toCompletableFuture().join())
                      .flatMap(Collection::stream)
                      .collect(ImmutableList.toImmutableList());

              ImmutableList<ServiceAreaInclusion> newServiceAreaInclusionGroup =
                  existingServiceAreaInclusionGroup.isEmpty()
                      ? serviceAreaInclusionGroup
                      : mergeServiceAreaInclusionGroups(
                          existingServiceAreaInclusionGroup, serviceAreaInclusionGroup);

              return Optional.of(
                  ServiceAreaUpdate.create(serviceAreaPath, newServiceAreaInclusionGroup));
            },
            classLoaderExecutionContext.current());
  }

  private ImmutableList<ServiceAreaInclusion> mergeServiceAreaInclusionGroups(
      ImmutableList<ServiceAreaInclusion> existingServiceAreaInclusionGroup,
      ImmutableList<ServiceAreaInclusion> serviceAreaInclusionGroup) {
    return Stream.of(existingServiceAreaInclusionGroup, serviceAreaInclusionGroup)
        .flatMap(List::stream)
        .collect(
            Collectors.toMap(
                ServiceAreaInclusion::getServiceAreaId,
                area -> area,
                (ServiceAreaInclusion existingInclusion, ServiceAreaInclusion newInclusion) ->
                    newInclusion == null ? existingInclusion : newInclusion))
        .values()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  private Boolean doesUpdateContainCorrectedAddress(
      ApplicantQuestion addressQuestion, ImmutableMap<String, String> updateMap) {
    Path correctedPath = addressQuestion.getContextualizedPath().join(Scalar.CORRECTED);
    String correctedValue = updateMap.get(correctedPath.toString());

    if (correctedValue == null) {
      return false;
    }

    return correctedValue.equals(CorrectedAddressState.CORRECTED.getSerializationFormat());
  }

  private ImmutableList<ServiceAreaInclusion> getExistingServiceAreaInclusionGroup(
      Path serviceAreaPath, ImmutableMap<String, String> updateMap) {

    return updateMap.keySet().stream()
        .filter(x -> x.startsWith(serviceAreaPath.toString()))
        .map(this::extractIndexFromPath)
        .filter(x -> x >= 0)
        .distinct()
        .sorted()
        .map(index -> getServiceAreaInclusion(updateMap, serviceAreaPath, index))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Attempts to find the index for a service_area array.
   *
   * @param updateMapKey Key from the updateMap in path form. Example:
   *     `applicant.question_name.service_area[0].service_area_id`.
   *     --------------------------------------^
   * @return the int value of the index or -1 if errors/no matches found
   */
  private int extractIndexFromPath(String updateMapKey) {
    Matcher matcher = pathIndexPattern.matcher(updateMapKey);
    if (!matcher.find()) {
      logger.error(
          "Could not match '{}' against pattern '{}'", updateMapKey, pathIndexPattern.pattern());
      return -1;
    }

    String groupValue = matcher.group("index");

    if (groupValue == null) {
      logger.error(
          "Could not find group 'index' in '{}' with pattern '{}'",
          updateMapKey,
          pathIndexPattern.pattern());
      return -1;
    }

    try {
      return Integer.parseInt(groupValue);
    } catch (NumberFormatException ex) {
      logger.error(
          "Could not parse 'index' value of '{}' from {} with pattern '{}'",
          groupValue,
          updateMapKey,
          pathIndexPattern.pattern());
      return -1;
    }
  }

  /** Build a {@link ServiceAreaInclusion} object from the values of an update map */
  private ServiceAreaInclusion getServiceAreaInclusion(
      ImmutableMap<String, String> updateMap, Path serviceAreaPath, Integer index) {
    Path elementAtIndex = serviceAreaPath.asArrayElement().atIndex(index);

    String serviceAreaIdPath =
        elementAtIndex.join(Scalar.SERVICE_AREA_ID.toDisplayString()).toString();
    String statePath = elementAtIndex.join(Scalar.SERVICE_AREA_STATE.toDisplayString()).toString();
    String timestampPath =
        elementAtIndex.join(Scalar.SERVICE_AREA_TIMESTAMP.toDisplayString()).toString();

    if (!updateMap.containsKey(serviceAreaIdPath)
        || !updateMap.containsKey(statePath)
        || !updateMap.containsKey(timestampPath)) {
      logger.error(
          "'updateMap' index {} missing one or more required fields {}, {}, {}.",
          index,
          serviceAreaIdPath,
          statePath,
          timestampPath);
    }

    String timestampStr = updateMap.get(timestampPath);
    ServiceAreaState serviceAreaState =
        ServiceAreaState.getEnumFromSerializedFormat(updateMap.get(statePath));
    long timestamp = Long.parseLong(timestampStr == null ? "0" : timestampStr);

    return ServiceAreaInclusion.builder()
        .setServiceAreaId(updateMap.get(serviceAreaIdPath))
        .setState(serviceAreaState)
        .setTimeStamp(timestamp)
        .build();
  }

  private AddressLocation getAddressLocationFromUpdates(
      ApplicantQuestion addressQuestion, ImmutableMap<String, String> updateMap) {
    Path latitudePath = addressQuestion.getContextualizedPath().join(Scalar.LATITUDE);
    Path longitudePath = addressQuestion.getContextualizedPath().join(Scalar.LONGITUDE);
    Path wellKnownIdPath = addressQuestion.getContextualizedPath().join(Scalar.WELL_KNOWN_ID);

    return AddressLocation.builder()
        .setLatitude(Double.parseDouble(updateMap.get(latitudePath.toString())))
        .setLongitude(Double.parseDouble(updateMap.get(longitudePath.toString())))
        .setWellKnownId(Integer.parseInt(updateMap.get(wellKnownIdPath.toString())))
        .build();
  }
}
