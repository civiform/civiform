# Admin page-level alerts (`#alertContainer`)

The admin layout (`server/app/views/admin/AdminLayout.html`) renders a single
alert region at the top of `<main>`:

```html
<div id="alertContainer" aria-live="polite">...</div>
```

This is the one place page-level errors and notifications appear. Toasts are
going away; do not add new toast call sites.

## The contract

- **One region, three producers.** Server-side rendering, htmx out-of-band
  swaps, and client-side script all write into `#alertContainer`. They all
  produce the same USWDS `usa-alert` markup.
- **Replace, never stack.** New content replaces whatever is in the container.
  If you need to show several messages at once, pass several alerts in a
  single update (a `List<PageAlert>` server-side, or one error summary
  client-side).
- **Announced automatically.** The container is `aria-live="polite"` and
  exists (possibly empty) at page load. Do **not** put `role="alert"` or
  `aria-live` on individual alerts — the region handles announcement, and a
  nested live region double-announces.
- **Scrolled into view.** `AlertContainer.init()` (wired in
  `admin_entry_point.ts`) observes the container and scrolls inserted alerts
  into view, so alerts triggered far down a long page are never off-screen.
  You don't need to do anything to get this.
- **Field-level validation errors stay inline** next to their controls (the
  `usa-error-message` spans emitted by the `cf:*` tags). The container only
  carries page-level messages and the error summary.

The building blocks:

| Piece | Where | Use for |
| --- | --- | --- |
| `PageAlert` | `server/app/views/shared/PageAlert.java` | Server-side alert data (type + text) |
| `BaseView.pageAlerts(model)` | `server/app/views/BaseView.java` | Page-supplied alerts on full renders |
| Flash keys `error` / `warning` / `success` | `controllers.FlashKey` | Alerts after a redirect, zero plumbing |
| `AlertFragment :: alerts(alerts)` | `server/app/views/admin/shared/AlertFragment.html` | Rendering a `PageAlert` list |
| `AlertFragment :: oobAlerts(alerts)` | same file | htmx out-of-band responses |
| `AlertContainer` | `server/app/assets/javascripts/global/shared/alert_container.ts` | Client-originated alerts |

---

## 1. Server-side rendering

`BaseView.render` assembles `templateGlobals.pageAlerts()` from two sources
and the layout renders them — pages never render page-level alerts inside
their own `content` fragment.

1. **Flash scope, automatic.** `PageAlert.fromFlash` maps `FlashKey.ERROR`,
   `FlashKey.WARNING`, and `FlashKey.SUCCESS` to alerts on every page render.
2. **Page-supplied.** Views override `pageAlerts(model)` for messages that
   belong to a re-render (e.g. a failed POST).

### Simple: flash message after a redirect

The post/redirect/get case needs no view or template changes at all. Flash
the message and redirect; whatever admin page renders next shows a success
alert in the container.

```java
// In a controller action:
return redirect(routes.AdminFooController.index())
    .flashing(FlashKey.SUCCESS, "Widget \"" + widget.name() + "\" created.");
```

Same for failures that end in a redirect:

```java
return redirect(routes.AdminFooController.index())
    .flashing(FlashKey.ERROR, "Widget could not be deleted because it is in use.");
```

### Complex: re-render after a failed POST, plus a custom flash key

When a POST fails validation you re-render the same page (no redirect, so no
flash). Thread the message through the ViewModel and surface it via the view's
`pageAlerts` override. `QuestionEditPageView` is the exemplar — it also folds
in a message carried by the non-standard `CONCURRENT_UPDATE` flash key, which
`fromFlash` deliberately ignores:

```java
// Controller: on service-level validation failure, re-render with the error.
String errorText = joinErrors(result.getErrors());
QuestionEditPageViewModel model =
    buildEditQuestionPageModel(request, id, questionForm, Optional.of(errorText));
return ok(questionEditPageView.render(request, model)).as(Http.MimeTypes.HTML);
```

```java
// View: surface the model's message as a page alert. Multiple alerts are
// fine — return them in display order.
@Override
protected ImmutableList<PageAlert> pageAlerts(QuestionEditPageViewModel model) {
  ImmutableList.Builder<PageAlert> alerts = ImmutableList.builder();
  model.getErrorMessage().map(PageAlert::error).ifPresent(alerts::add);
  if (model.isDraftOutOfDate()) {
    alerts.add(PageAlert.warning("A newer draft of this question exists."));
  }
  return alerts.build();
}
```

The template needs nothing: the layout inserts
`AlertFragment :: alerts(${templateGlobals.pageAlerts()})` into the container.

---

## 2. htmx responses (out-of-band swap)

An htmx response can update `#alertContainer` regardless of what its
`hx-target` is, by including an out-of-band carrier. htmx matches the carrier
to the layout's container by id and replaces the container's children:

```html
<div id="alertContainer" hx-swap-oob="innerHTML">
  <!-- alert markup -->
</div>
```

Controllers keep returning `ok(...)` — no status-code discipline required.

### Simple: alert-only failure response that leaves the target alone

`FailedRequestPartial` (predicates) is the pattern for "the request failed,
show an error, touch nothing else." The partial is just the OOB carrier, and
the controller adds `HX-Reswap: none` so the `hx-target` (the conditions
list) is not swapped with the empty remainder of the response:

```html
<!-- FooFailedPartial.html — the whole file is the partial -->
<div id="alertContainer" hx-swap-oob="innerHTML">
  <div class="usa-alert usa-alert--slim usa-alert--error">
    <div class="usa-alert__body">
      <p class="usa-alert__text">Something went wrong. Try again later.</p>
    </div>
  </div>
</div>
```

```java
// Controller helper (see failedRequestResponse in
// AdminProgramBlockPredicatesController.java):
private Result fooFailedResponse(Request request) {
  return ok(fooFailedPartialView.render(request, new FooFailedPartialViewModel()))
      .withHeader("HX-Reswap", "none")
      .as(Http.MimeTypes.HTML);
}
```

If your alerts are `PageAlert`s, skip the hand-written markup and reuse the
shared fragment:

```html
<th:block
  th:replace="~{admin/shared/AlertFragment :: oobAlerts(${model.alerts()})}"
></th:block>
```

### Complex: swap new content *and* raise an alert in one response

A response can do its normal swap and carry the OOB alert alongside. Here a
save action re-renders the list into its `hx-target` and confirms success at
the top of the page:

```html
<!--/* WidgetListPartial.html — main body swaps into the hx-target,
       the trailing OOB div goes to #alertContainer. */-->
<ul id="widget-list">
  <li th:each="widget : ${model.widgets()}" th:text="${widget.name()}"></li>
</ul>

<div id="alertContainer" hx-swap-oob="innerHTML">
  <th:block
    th:insert="~{admin/shared/AlertFragment :: alerts(${model.alerts()})}"
  ></th:block>
</div>
```

```java
// Controller: same render call either way; the model decides the alert.
WidgetListPartialViewModel model =
    WidgetListPartialViewModel.builder()
        .widgets(updatedWidgets)
        .alerts(ImmutableList.of(PageAlert.success("Widget saved.")))
        .build();
return ok(widgetListPartialView.render(request, model)).as(Http.MimeTypes.HTML);
```

To **clear** a previously shown alert on a later success, include an empty
carrier — `<div id="alertContainer" hx-swap-oob="innerHTML"></div>` — in that
response (same trick the geoJSON flow uses for its field errors).

`MessagePartial.html` (API bridge) is a live example of an OOB alert with
richer content (heading + message list) than a `PageAlert` carries.

---

## 3. Client-side (`AlertContainer`)

`AlertContainer` in `global/shared/alert_container.ts` builds the same
`usa-alert` markup from script. Every method no-ops (returning `false` where
applicable) on pages without the container, so shared code can call it
unconditionally.

### Simple: show and clear a notification

```ts
import {AlertContainer} from '@/global/shared/alert_container'

// After a successful copy-to-clipboard:
AlertContainer.show('success', 'Program link copied to clipboard.')

// Types: 'error' | 'info' | 'success' | 'warning'
AlertContainer.show('warning', 'This draft has unsaved changes.')

// Remove whatever is currently displayed:
AlertContainer.clear()
```

### Complex: error summary with links to failing controls

`showErrorSummary` renders a `usa-alert--error` with a heading and one linked
entry per failing control, then moves focus to the summary so it is announced
and visible. Clicking an entry focuses its control. Entries without a
`controlId` render as plain text.

```ts
import {AlertContainer} from '@/global/shared/alert_container'

const shown = AlertContainer.showErrorSummary('Please fix the following errors', [
  {message: 'Question text is required', controlId: 'questionText'},
  {message: 'Administrative name is already in use', controlId: 'questionName'},
  {message: 'At least one option is required'}, // no control to link to
])

if (!shown) {
  // Page has no alert container (e.g. a legacy layout) — fall back to
  // focusing the first invalid control directly.
  document.getElementById('questionText')?.focus()
}
```

### You usually get this for free: `FormValidation`

Forms that opt into client validation with `data-form-type` don't call
`AlertContainer` themselves. On a failed submit, `FormValidation` collects the
inline field errors, shows the summary, and focuses it; a subsequent valid
submit clears the container:

```html
<form data-form-type="dynamic" method="POST" th:action="${model.formActionUrl}">
  <cf:input
    id="questionText"
    name="questionText"
    th:label="#{questionnew.label.questiontext}"
    th:value="${model.questionText}"
    required="true"
  />
  ...
</form>
```

Field-level messages still appear inline next to each control — the summary
links back to them, it does not replace them.

---

## Pitfalls

- **Don't stack.** Never append to the container; every producer replaces its
  content. Batch multiple messages into one update.
- **Don't add `role="alert"`, `aria-live`, or `aria-label` to alerts** going
  into the container. The region announces; nested live regions
  double-announce.
- **Don't render page-level alerts inside a page's `content` fragment.** Use
  `pageAlerts(model)` so they land in the shared container.
- **Alert-only htmx responses need `HX-Reswap: none`**, otherwise the empty
  remainder of the response body is swapped into the `hx-target` and clears
  it.
- **`hx-target-error` / `response-targets` is not the pattern** for this
  container. A blanket error target would swap raw `badRequest(...)` text and
  framework error pages into the region. Handled errors return `200` with an
  OOB alert; if honest status codes become a requirement, revisit via htmx
  `responseHandling` config with the same OOB fragment.
- **Custom flash keys are not auto-rendered.** Only `error`, `warning`, and
  `success` map automatically; anything else (e.g. `CONCURRENT_UPDATE`) must
  be read by the controller and threaded through the ViewModel.
