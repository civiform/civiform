package controllers.geojson;

import static j2html.TagCreator.div;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.forbidden;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import auth.Authorizers;
import java.net.MalformedURLException;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.geojson.GeoJsonAccessException;
import services.geojson.GeoJsonClient;
import services.geojson.GeoJsonNotFoundException;
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
        .fetchAndSaveGeoJson(geoJsonEndpoint)
        .thenApplyAsync(
            geoJsonResponse ->
                ok(mapQuestionSettingsPartialView.render(
                        request,
                        MapQuestionSettingsPartialViewModel.withEmptyDefaults(
                            geoJsonResponse.getPossibleKeys())))
                    .as(Http.MimeTypes.HTML))
        .exceptionally(
            ex -> {
              logger.error("An error occurred trying to retrieve GeoJSON", ex);
              Throwable rootCause = ex;
              while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
              }
              String errorMessage =
                  String.format(
                      "Error: GeoJSON unable to be retrieved - %s", rootCause.getMessage());

              if (rootCause instanceof MalformedURLException) {
                return badRequest(
                        div(errorMessage)
                            .withClasses("text-red-500", "text-base py-2")
                            .withId("geoJsonURL-errors")
                            .toString())
                    .as(Http.MimeTypes.HTML);
              } else if (rootCause instanceof GeoJsonAccessException) {
                return forbidden(
                        div(errorMessage)
                            .withClasses("text-red-500", "text-base py-2")
                            .withId("geoJsonURL-errors")
                            .toString())
                    .as(Http.MimeTypes.HTML);
              } else if (rootCause instanceof GeoJsonNotFoundException) {
                return notFound(
                        div(errorMessage)
                            .withClasses("text-red-500", "text-base py-2")
                            .withId("geoJsonURL-errors")
                            .toString())
                    .as(Http.MimeTypes.HTML);
              } else {
                return internalServerError(
                        div("GeoJSON unable to be retrieved. Please try re-entering the endpoint"
                                + " URL.")
                            .withClasses("text-red-500", "text-base py-2")
                            .withId("geoJsonURL-errors")
                            .toString())
                    .as(Http.MimeTypes.HTML);
              }
            });
  }
}
