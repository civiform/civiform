package views;

import com.google.inject.TypeLiteral;
import javax.inject.Inject;
import views.shared.BaseViewDeps;

/**
 * A {@link BaseView} that derives {@link #pageTemplate()} from its model type instead of requiring
 * a view subclass to specify the template path.
 *
 * <p>The template path is the model's fully qualified class name relative to the {@link views}
 * package, with the trailing {@code ViewModel} removed. For example {@code
 * views.admin.questions.MapQuestionSettingsPartialViewModel} maps to the template {@code
 * admin/questions/MapQuestionSettingsPartial} ({@code .html} is appended by the template resolver).
 *
 * <p>No view subclass or Guice binding is needed; inject it with the model type as the type
 * argument:
 *
 * <pre>{@code
 * @Inject
 * MyController(PartialView<MapQuestionSettingsPartialViewModel> mapQuestionSettingsPartialView)
 * }</pre>
 *
 * <p>Use this when the partial has no reason to override any other {@link BaseView} behavior; views
 * that do should extend {@link BaseView} directly.
 *
 * @param <TModel> A class or record that implements {@link BaseViewModel}
 */
public final class PartialView<TModel extends BaseViewModel> extends BaseView<TModel> {
  private final TypeLiteral<TModel> modelType;

  @Inject
  public PartialView(BaseViewDeps baseViewDeps, TypeLiteral<TModel> modelType) {
    super(baseViewDeps);
    this.modelType = modelType;
  }

  @Override
  protected String pageTemplate() {
    String modelClassName = modelType.getRawType().getName();

    if (!modelClassName.startsWith("views.")) {
      throw new IllegalStateException(
          String.format(
              "%s must be in the views package to derive a template path", modelClassName));
    }

    if (!modelClassName.endsWith("ViewModel")) {
      throw new IllegalStateException(
          String.format(
              "%s must have a class name ending in \"ViewModel\" to derive a template path",
              modelClassName));
    }

    if (modelClassName.indexOf('$') >= 0) {
      throw new IllegalStateException(
          String.format("%s must be a top-level class to derive a template path", modelClassName));
    }

    return modelClassName
        .substring("views.".length(), modelClassName.length() - "ViewModel".length())
        .replace('.', '/');
  }
}
