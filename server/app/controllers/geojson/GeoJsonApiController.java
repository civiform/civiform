package controllers.geojson;

import static j2html.TagCreator.div;
import static play.mvc.Results.ok;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import forms.MapQuestionForm;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.geojson.GeoJsonClient;
import views.admin.questions.MapQuestionSettingsPartialView;
import views.admin.questions.MapQuestionSettingsPartialViewModel;

public final class GeoJsonApiController {
  private static final Logger logger = LoggerFactory.getLogger(GeoJsonApiController.class);

  private final FormFactory formFactory;
  private final GeoJsonClient geoJsonClient;
  private final MapQuestionSettingsPartialView mapQuestionSettingsPartialView;

  @Inject
  GeoJsonApiController(
      FormFactory formFactory,
      GeoJsonClient geoJsonClient,
      MapQuestionSettingsPartialView mapQuestionSettingsPartialView) {
    this.formFactory = formFactory;
    this.geoJsonClient = geoJsonClient;
    this.mapQuestionSettingsPartialView = mapQuestionSettingsPartialView;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxGetData(Http.Request request) {
    DynamicForm formData = formFactory.form().bindFromRequest(request);
    String geoJsonEndpoint = formData.get("geoJsonEndpoint");

    return geoJsonClient
        .fetchGeoJson(geoJsonEndpoint)
        .thenApplyAsync(
            geoJsonResponse -> {
              Set<String> possibleKeys = new HashSet<>();
              geoJsonResponse
                  .features()
                  .forEach((feature) -> possibleKeys.addAll(feature.properties().keySet()));
              ImmutableList.Builder<MapQuestionForm.Setting> builder = ImmutableList.builder();
              builder.add(new MapQuestionForm.Setting("", ""));
              builder.add(new MapQuestionForm.Setting("", ""));
              builder.add(new MapQuestionForm.Setting("", ""));
              MapQuestionSettingsPartialViewModel model =
                  new MapQuestionSettingsPartialViewModel(
                      OptionalInt.empty(),
                      new MapQuestionForm.Setting("", "Name"),
                      new MapQuestionForm.Setting("", "Address"),
                      new MapQuestionForm.Setting("", "URL"),
                      builder.build(),
                      possibleKeys);
              return ok(mapQuestionSettingsPartialView.render(request, model))
                  .as(Http.MimeTypes.HTML);
            })
        .exceptionally(
            ex -> {
              logger.error("An error occurred trying to retrieve GeoJSON", ex);
              String errorMessage = "An error occurred trying to retrieve GeoJSON";

              // TODO(#11125): Implement error state.
              return ok(div(errorMessage).withClass("text-red-500").toString())
                  .as(Http.MimeTypes.HTML);
            });
  }
}
