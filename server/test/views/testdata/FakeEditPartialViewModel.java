package views.testdata;

import views.BaseViewModel;

/**
 * Test-only view model in a subpackage of {@code views}, used by {@link views.PartialViewTest} to
 * verify that the package is converted to a template directory path.
 */
public record FakeEditPartialViewModel() implements BaseViewModel {}
