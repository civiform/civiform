package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import play.libs.concurrent.HttpExecutionContext;
import services.Path;
import services.applicant.question.ApplicantQuestion;
import services.applicant.question.Scalar;
import services.geo.AddressLocation;
import services.geo.CorrectedAddressState;
import services.geo.ServiceAreaInclusion;
import services.geo.ServiceAreaInclusionGroup;
import services.geo.esri.EsriClient;
import services.geo.esri.EsriServiceAreaValidationConfig;
import services.geo.esri.EsriServiceAreaValidationOption;

/** Contains methods for resolving {@link ServiceAreaUpdate}s to update applicant data. */
final class ServiceAreaUpdateResolver {
  private final EsriClient esriClient;
  private final EsriServiceAreaValidationConfig esriServiceAreaValidationConfig;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ServiceAreaUpdateResolver(
      EsriClient esriClient,
      EsriServiceAreaValidationConfig esriServiceAreaValidationConfig,
      HttpExecutionContext httpExecutionContext) {
    this.esriClient = checkNotNull(esriClient);
    this.esriServiceAreaValidationConfig = checkNotNull(esriServiceAreaValidationConfig);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
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
            httpExecutionContext.current());
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
    String serviceAreaValue = updateMap.get(serviceAreaPath.toString());
    ImmutableList.Builder<ServiceAreaInclusion> existingServiceAreaInclusionGroupBuilder =
        ImmutableList.builder();
    if (serviceAreaValue != null) {
      existingServiceAreaInclusionGroupBuilder.addAll(
          ServiceAreaInclusionGroup.deserialize(serviceAreaValue));
    }

    return existingServiceAreaInclusionGroupBuilder.build();
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
