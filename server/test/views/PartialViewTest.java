package views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Before;
import org.junit.Test;
import views.shared.BaseViewDeps;
import views.testdata.FakeEditPartialViewModel;

public class PartialViewTest {
  private BaseViewDeps baseViewDeps;

  @Before
  public void setup() {
    baseViewDeps = mock(BaseViewDeps.class);
  }

  @Test
  public void pageTemplate_modelInViewsPackageRoot_stripsViewModelSuffix() {
    PartialView<FakeMessagePartialViewModel> view =
        new PartialView<>(baseViewDeps, TypeLiteral.get(FakeMessagePartialViewModel.class));

    assertThat(view.pageTemplate()).isEqualTo("FakeMessagePartial");
  }

  @Test
  public void pageTemplate_modelInSubPackage_convertsPackageToDirectoryPath() {
    PartialView<FakeEditPartialViewModel> view =
        new PartialView<>(baseViewDeps, TypeLiteral.get(FakeEditPartialViewModel.class));

    assertThat(view.pageTemplate()).isEqualTo("testdata/FakeEditPartial");
  }

  @Test
  public void pageTemplate_modelNameWithoutViewModelSuffix_throws() {
    PartialView<FakeSettings> view =
        new PartialView<>(baseViewDeps, TypeLiteral.get(FakeSettings.class));

    assertThatThrownBy(view::pageTemplate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ending in \"ViewModel\"");
  }

  @Test
  public void pageTemplate_nestedModelClass_throws() {
    PartialView<NestedPartialViewModel> view =
        new PartialView<>(baseViewDeps, TypeLiteral.get(NestedPartialViewModel.class));

    assertThatThrownBy(view::pageTemplate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("top-level class");
  }

  @Test
  public void injectedByGuice_resolvesModelTypeWithoutExplicitBinding() {
    Injector injector =
        Guice.createInjector(binder -> binder.bind(BaseViewDeps.class).toInstance(baseViewDeps));

    PartialView<FakeEditPartialViewModel> view =
        injector.getInstance(Key.get(new TypeLiteral<PartialView<FakeEditPartialViewModel>>() {}));

    assertThat(view.pageTemplate()).isEqualTo("testdata/FakeEditPartial");
  }

  /** Nesting places this model inside the test class, giving it a {@code $} in its name. */
  public record NestedPartialViewModel() implements BaseViewModel {}
}

/** In the {@code views} package root, so its template path has no directory component. */
record FakeMessagePartialViewModel() implements BaseViewModel {}

/** Missing the {@code ViewModel} class name suffix required to derive a template path. */
record FakeSettings() implements BaseViewModel {}
