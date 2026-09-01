package support;

import views.BaseViewModel;

/**
 * Test-only view model outside the {@code views} package, used by {@link views.PartialViewTest} to
 * verify that a template path cannot be derived for it.
 */
public record FakeOutsidePartialViewModel() implements BaseViewModel {}
