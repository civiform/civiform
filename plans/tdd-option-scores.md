# Technical Design: Optional Score on Multi-Option Question Options

Implements [prd-option-scores.md](prd-option-scores.md). Seven phases, each an independently
shippable PR that keeps the feature inert behind the flag. Phase 2 depends on phase 1's additive
`QuestionOption.score` model for import validation; phase 3 depends on both; phases 4–6 depend on 3
and are independent of each other.

Per repo convention: write JUnit tests in every phase, do not run them locally; browser tests
are out of scope for the server PRs (phase 7 notes the follow-up).

---

## Cross-cutting design decisions

### D0. Additive-only storage and model compatibility

Scoring must not change the meaning, type, or location of any existing database column, JSON
property, answer scalar, or model property. The implementation is additive only:

- Add one `programs.uses_scoring` column with `DEFAULT FALSE NOT NULL`; do not alter or repurpose
  an existing program column.
- Add an optional `score` property to each serialized `QuestionOption`; old option JSON without the
  property continues to deserialize unchanged, and all existing factories and constructor
  overloads remain available.
- Add score data to a submitted application's private JSON document only as additive sibling
  properties: a `score`/`scores` key inside an answered question's own JSON object, next to the
  existing `selection`/`selections` scalar (exactly where the `updated_at` /
  `program_updated_in` metadata scalars already sit), plus a root-level `total_score` next to
  the `applicant` root. Do not rewrite, reorder, canonicalize, or change the representation of
  any existing answer value, including checkbox `selections`.
- Add model accessors, builder methods, and constructor overloads without removing or changing
  existing signatures. Existing callers default to scoring off.
- No migration or backfill rewrites existing question JSON, program-definition JSON, applicant
  data, or application snapshots. Pre-feature rows remain valid and are interpreted as unscored.
- With the feature flag off, application/API/PDF/CSV output shapes remain unchanged. Program
  migration JSON may contain the new additive configuration properties because import and export
  deliberately remain ungated.

This storage contract refines the PRD's checkbox implementation detail: canonical ordering remains
an export presentation rule, but submission does not rewrite the persisted `selections` answer.
This preserves the existing stored answer model while still emitting aligned, canonical API output.

### D1. Feature flag

`ANSWER_OPTION_SCORING_ENABLED`, boolean, `mode: ADMIN_WRITEABLE`, in the **Experimental**
group of `server/conf/env-var-docs.json` (description prefixed `(NOT FOR PRODUCTION USE) `),
plus `answer_option_scoring_enabled = false` with env override in
`server/conf/helper/feature-flags.conf`. Regenerate `SettingsManifest` via
`bin/generate-settings-manifest` → request-aware getter
`getAnswerOptionScoringEnabled(RequestHeader)`.

Threading policy (PRD): the flag is resolved **only at controller boundaries** and passed as an
explicit `boolean` into views, form processing, submission, exporters, samplers, and schema
generation. No service or exporter injects `SettingsManifest` for this flag. Import and
deserialization paths never consult the flag (stored configuration is preserved while off).

### D2. Supported-type set

`QuestionType.isMultiOptionType()` returns true for `YES_NO`
(`QuestionType.java:70-73`), which must never score. Add to `QuestionType`, following the
`QUESTION_TYPES_SUPPORTING_SETTINGS` precedent at `QuestionType.java:48-64`:

```java
private static final ImmutableSet<QuestionType> QUESTION_TYPES_SUPPORTING_OPTION_SCORES =
    ImmutableSet.of(QuestionType.CHECKBOX, QuestionType.DROPDOWN, QuestionType.RADIO_BUTTON);
public static boolean supportsOptionScores(QuestionType type) { ... }
```

Every scoring code path gates on this; none uses `isMultiOptionType()`.

### D3. Value types

- Option score: `Optional<Integer>` on `QuestionOption` (signed 32-bit; negatives allowed;
  absent ≠ 0).
- Checkbox per-question aggregate and application `total_score`: `long` / `Long` (signed
  64-bit) everywhere they are computed, persisted, or exported. Max 32-bit option scores
  cannot overflow a 64-bit sum.
- In the application JSON snapshot and JSON export, numbers are written as `Long`:
  `CfJsonDocumentContext` has `putLong` but no `putInt`, and
  `JsonExporterService.exportApplicationEntriesToJsonApplication`
  (`JsonExporterService.java:271-293`) **silently drops** any value that isn't
  `String`/`Long`/`Double`/`ImmutableList<String>`. All score values crossing that dispatch
  must be boxed `Long` or the new nullable-array wrapper (D5).

### D4. Score keys are stored as siblings of the answer

Persisted keys are added to the submitted application's private JSON snapshot next to the answer
they score. Per-question keys live inside the question's own JSON object, as siblings of the
`selection`/`selections` scalar — the same additive-sibling shape the `updated_at` /
`program_updated_in` metadata scalars already use. The application-level total lives at the
document root, as a sibling of the `applicant` object.

| Key | Path | Type |
|---|---|---|
| `score` | `applicant.<contextualized-question-path>.score` (sibling of `selection`) | nullable long (absent when option unscored) |
| `scores` | `applicant.<contextualized-question-path>.scores` (sibling of `selections`) | array of nullable longs, parallel to the existing stored `selections` array |
| `total_score` | `total_score` (document root, sibling of `applicant`) | long; **presence = scoring was applied** |

Add a small `ApplicationScoreMetadata` helper in `services/applicant/` that owns these three literal
key names and derives the paths: `scorePath(contextualizedPath)` / `scoresPath(contextualizedPath)`
join the key onto the question's contextualized path, and `totalScorePath()` is the fixed root
key. Do **not** add `SCORE`, `SCORES`, or `TOTAL_SCORE` to `Scalar`, and do not add a new
`ScalarType`. Everything that enumerates question content — predicates, API bridges, CSV metadata
columns, applicant update binding, scalar-driven schema generation — is driven by
`Scalar.getScalars(type)`, so keys that are not scalars stay invisible to those surfaces by
construction.

Collision analysis (why sibling keys are safe without reserving question admin names):

- A question's JSON object contains only scalar keys; question admin names appear one level
  higher (directly under `applicant`, or inside a repeated entity's object). No question type has
  a scalar named `score` or `scores`, so the new keys cannot collide with any existing key, and a
  question admin-named `score`, `scores`, or `total_score` produces a question *object* at a
  different depth — never a key inside another question's object.
- `total_score` sits outside the `applicant` tree entirely, so it cannot collide with any
  question admin name.

Applicant update requests must reject the three score keys as reserved scalar key names: extend
the existing reserved-key precheck that already rejects `updated_at`/`program_updated_in` as an
update path's final segment (`ApplicantService.java:293-302`), rather than adding any
namespace-root check.

> **Note on the earlier attempt:** a prior in-development iteration stored this data under an
> isolated `application_metadata.option_scoring` root. That was wrong — score data belongs with
> the answer it scores. The feature is unreleased, so the storage is reworked in place with no
> migration, dual-read, or other backwards compatibility for `application_metadata` snapshots.

### D5. Nullable numeric arrays

Guava `ImmutableList` rejects nulls: a `scores` array with null holes deserialized through
`readLongList` throws inside Jackson's `GuavaModule` and surfaces as `Optional.empty()` — the
list silently vanishes. Therefore:

- Write: `putArray(Path, List)` already accepts a plain `java.util.List` with nulls — use an
  `ArrayList<Long>`.
- Read: new `CfJsonDocumentContext.readNullableLongList(Path)` returning
  `Optional<List<Long>>` via `TypeRef<ArrayList<Long>>` (no Guava).
- Export: new wrapper type (e.g. `record NullableLongArray(List<Long> values)`) with an
  explicit branch in `exportApplicationEntriesToJsonApplication` and a
  `putNullableLongArray` on the export JSON writer, preserving null positions.

### D6. Applicant-invisibility invariants

Enforcement points, testable per phase:

1. `LocalizedQuestionOption` (the applicant-facing localized option object) never carries the
   score; it stays only on `QuestionOption`, resolved by option id (phase 1).
2. No applicant-facing fragment/template is touched; applicant HTML never renders scores in
   any form (phase 1, 7 audit).
3. Crafted applicant updates whose path ends in a reserved score key (`score`, `scores`,
   `total_score`) are rejected before staging (phase 3).
4. Score data is written only to the application's private snapshot copy, never to the
   applicant's shared `ApplicantData` row (phase 3).
5. PDF score rendering requires `isAdmin && includeScores && total_score present`; the
   applicant download path (`UpsellController`) hard-codes `includeScores=false` (phase 4).
6. CSV/JSON/API score surfaces exist only on admin/API-authorized endpoints (phases 5–6).

---

## Phase 1 — Feature flag + score in question configuration

**Goal:** Admins can enter/edit an optional integer score per option on radio, checkbox, and
dropdown questions in the legacy j2html pages; the score persists in the question row's
options JSON. Nothing reads it yet. Everything is gated by the flag; with the flag off, edits
preserve stored scores and crafted posts are ignored.

### 1.1 Flag plumbing

- `conf/env-var-docs.json` + `conf/helper/feature-flags.conf` + regenerated
  `SettingsManifest` per D1.

### 1.2 `QuestionType`

- Add `QUESTION_TYPES_SUPPORTING_OPTION_SCORES` + `supportsOptionScores` per D2.

### 1.3 `QuestionOption` (`services/question/QuestionOption.java`)

- New property `@JsonProperty("score") public abstract Optional<Integer> score();` plus
  builder setter `setScore` (both `Optional<Integer>` and unwrapped `Integer` overloads are
  fine; the `@JsonProperty` goes on the builder setter as with `displayInAnswerOptions`).
- `jsonCreator` (L46-63): add `@JsonProperty("score") Optional<Integer> score` and thread it
  through **both** branches — including the legacy `localizedOptionText == null` branch,
  which today silently drops `displayInAnswerOptions`; use `.toBuilder().setScore(score)` so
  legacy-shaped rows still preserve a score if one exists.
- New factory overload
  `create(id, displayOrder, adminName, optionText, displayInAnswerOptions, score)`; existing
  overloads remain unchanged and keep AutoValue's empty-Optional default. Do not replace or
  reorder parameters in an existing overload.
- Serialization: `QuestionOption` has no `@JsonInclude(NON_ABSENT)`, so empty serializes as
  explicit `null`. That is fine (`FAIL_ON_UNKNOWN_PROPERTIES` is off and the creator maps
  null → empty); do not change the include policy — it would churn every stored row's shape.
- **Not** added to `LocalizedQuestionOption` (positional AutoValue, applicant-facing; D6.1).
  Admin form and scoring code resolve scores from `QuestionOption` by option id.
- No changes needed in `QuestionModel` — `@DbJsonB ImmutableList<QuestionOption>` picks the
  field up automatically.

### 1.4 `MultiOptionQuestionForm` (`forms/questions/MultiOptionQuestionForm.java`)

- Two new mutable parallel lists, mirroring the existing split (L26-44):
  `List<String> optionScores` (existing options) and `List<String> newOptionScores`.
  Initialize in both constructors; the definition-based constructor populates
  `optionScores` in the same sorted order as `options`, mapping each option's score to its
  decimal string or `""`.
- String binding preserves blank vs `0` vs invalid, per the `setMinChoicesRequired(String)`
  precedent (L173-195).
- Parsing/validation: private `parseScore(String)` → trim; blank → `Optional.empty()`; else
  strict `Integer.parseInt` in try/catch. `NumberFormatException` covers decimals, exponents,
  overflow, and malformed input in one place (the HTML input is `type="number" step="1"`, so
  this is a server-side backstop against crafted posts).
- Cardinality: before building options, verify `optionScores.size() == options.size()` and
  `newOptionScores.size() == newOptions.size()`. Unlike the existing
  `Preconditions.checkArgument` cardinality checks (L218-223), score problems must surface as
  **form validation errors, not exceptions** (PRD): expose
  `getOptionScoreErrors(): ImmutableSet<CiviFormError>` computed from cardinality + parse
  failures. `AdminQuestionController.create`/`update` call it after binding and merge the
  errors into the same error rendering used for `QuestionService` validation errors,
  re-rendering the edit view instead of saving.
- `getBuilder()` (L203-265): accept a `boolean scoringEnabled` (threaded from the
  controller). In both option-building loops set the score on `QuestionOption.create(...)`
  only when `scoringEnabled && QuestionType.supportsOptionScores(getQuestionType())`;
  otherwise build with empty score. This also inherently excludes `YesNoQuestionForm`.

### 1.5 Controller preservation semantics (`controllers/admin/AdminQuestionController.java`)

Resolve the flag once per request in `create` and `update`, pass it into
`getBuilder(maybeExisting, questionForm)` (L424-433) and onward:

- `updateDefaultLocalizationForOptions` (L515-561), which merges submitted options into the
  existing definition on every edit:
  - Flag **on**: branches 1 and 2 copy `setScore(updatedOption.score())` exactly like
    `setDisplayInAnswerOptions` (L541, L551); branch 3 (new options) carries the submitted
    score verbatim.
  - Flag **off**: branches 1 and 2 copy `setScore(currentOption.score())` — the stored score
    survives even though the input wasn't rendered and any crafted score field is discarded;
    branch 3 strips scores.
- Create path (no existing definition), flag off: strip all scores from the built options —
  new questions cannot acquire scores while the flag is off.
- Translation-only edits need no change: `MultiOptionQuestionTranslationForm.builderWithUpdates`
  uses `current.toBuilder().setOptionText(...)` (L33-51), so scores survive automatically.
  Add a regression test.

### 1.6 Admin UI (`views/admin/questions/QuestionConfig.java`, `QuestionEditView.java`)

- `multiOptionQuestionField(...)` (`QuestionConfig.java:323-422`): new number input —
  name `optionScores[]` / `newOptionScores[]` (matching the existing / new split at L327,
  L376), `type="number"`, `step="1"`, no min/max, label "Score", never read-only (unlike the
  admin ID at L337), reference class `cf-multi-option-score-input`. The row container is
  `grid grid-cols-8 grid-rows-4`; extend to `grid-rows-6` with the score input occupying
  rows 5-6, cols 1-5 (under Option Text) when scoring UI is enabled — verify visually.
- Value population: `addMultiOptionQuestionFields` (L424-478) renders existing rows from the
  form's parallel lists; pass `Optional<String> scoreValue` (from
  `form.getOptionScores().get(i)`) into `multiOptionQuestionField` as a new parameter — no
  `LocalizedQuestionOption` change.
- Gating: thread a `boolean showScores` (flag AND `supportsOptionScores(questionType)`) from
  `QuestionEditView` (which already reads `settingsManifest` request-aware, see L867) into
  `buildQuestionConfig`, `multiOptionQuestionFieldTemplate` (L315-317), and the
  `<template id="multi-option-question-answer-template">` wrapper
  (`QuestionEditView.java:325-341`). The Yes/No renderer (`yesNoOptionQuestionField`,
  L557-650) is a separate code path and is untouched.
- TypeScript: **no changes.** `multi_option_question.ts` clones the whole template row, so
  cloned rows get the input for free. `admin_validation.ts` selectors are name-specific and
  unaffected. Verify `preview.ts` (reads only `[name="options[]"]` at L193; its
  MutationObserver at L199-210 watches row additions, not inputs) does not pick up the score
  input — add a note in the PR, no code change expected.

### 1.7 Tests

- `QuestionOptionTest`: serde round-trip for absent / explicit null / value / negative;
  legacy-branch creator preserves score; existing stored JSON without the field
  deserializes to empty.
- `MultiOptionQuestionFormTest`: definition→form population; blank vs `0` vs invalid
  (decimal, exponent, `2^31`, junk); cardinality mismatch → errors not exceptions;
  `scoringEnabled=false` builds scoreless options; Yes/No form never builds scores. Bind at
  least one case through a real `formFactory.form(...).bind(...)` to prove Play's binding of
  the parallel string lists (including a trailing-blank row).
- `QuestionConfigTest`: input rendered with value when enabled; absent when flag off or type
  unsupported (incl. Yes/No).
- `AdminQuestionControllerTest`: create/update with scores (flag on); flag off — update
  preserves stored scores, crafted `optionScores[]` ignored, create yields no scores;
  invalid score re-renders with error; translation-form edit preserves scores.

**Exit criteria:** scores round-trip through create → edit → publish; flag off is a strict
no-op for existing behavior; nothing outside question configuration reads scores.

---

## Phase 2 — Program-level scoring setting + program migration

**Goal:** Programs carry a `usesScoring` boolean (default false), editable as a yes/no radio
pair on the program create/edit form behind the flag, exported/imported with the program, and
preserved on edit while the flag is off. Question-score import validation lands here too.

### 2.1 Evolution

`conf/evolutions/default/97.sql`, copying the `93.sql` (login_only) shape:

```sql
# --- Whether the program applies answer-option scores to submitted applications
# --- !Ups
ALTER TABLE IF EXISTS programs
ADD COLUMN IF NOT EXISTS uses_scoring boolean DEFAULT FALSE NOT NULL;

# --- !Downs
ALTER TABLE IF EXISTS programs
DROP COLUMN IF EXISTS uses_scoring;
```

Postgres backfills existing rows from the constant default; no separate UPDATE needed.
This is the only relational-schema change for scoring. Do not modify an existing column or add a
score column to questions, applicants, or applications; option scores and submitted score keys
remain additive JSON properties.

### 2.2 `ProgramModel` (`models/ProgramModel.java`)

Mirror `eligibilityIsGating`, all four persistence touch points: field
`@Constraints.Required private Boolean usesScoring;` (~L103); the
`ProgramModel(ProgramDefinition, Optional<VersionModel>)` ctor used by import (~L197); the
public positional ctor (~L234/254); `@PreUpdate persistChangesToProgramDefinition` (~L281);
`@PostLoad loadProgramDefinition` builder (~L319).

Keep the existing public positional constructor signature and have it delegate to a new overload
with `usesScoring=false`. Update `test/support/ProgramBuilder.java`, `DevDatabaseSeedTask`, and
scoring-aware callers to use the new overload or service parameter. This avoids a breaking source
change for existing tests, seeders, and external construction code.

### 2.3 `ProgramDefinition` — deserialization-safe default

`loginOnly`/`eligibilityIsGating` are required primitives with no default; a program export
missing them fails AutoValue's `build()`. Since pre-feature exports **must** import (PRD),
`usesScoring` cannot be a bare required primitive. Use the Optional-internal pattern:

```java
@JsonProperty("usesScoring")
abstract Optional<Boolean> usesScoringInternal();

@JsonIgnore
public final boolean usesScoring() { return usesScoringInternal().orElse(false); }

// Builder:
@JsonProperty("usesScoring")
abstract Builder setUsesScoringInternal(Boolean usesScoring);

public final Builder setUsesScoring(boolean usesScoring) {
  return setUsesScoringInternal(usesScoring);
}
```

AutoValue defaults Optional properties to empty, so a missing JSON field builds cleanly and
reads as `false`. Verify with a fixture test deserializing a pre-feature export JSON (copy
one of the `AdminImportControllerTest` program JSON blocks with the field removed). Do not inject
the field into the raw JSON tree as a migration workaround; backward compatibility belongs in the
model default so every deserialization path behaves consistently.

Keep the existing JSON property names and types unchanged. A new export may add
`"usesScoring": false`; an old export with no property must continue to import without requiring a
migration step.

### 2.4 Form, view, controller, service

- `ProgramForm`: `private Boolean usesScoring = false;` (~L28).
- `ProgramFormBuilder`: constant `SCORING_FIELD_NAME = "usesScoring"`; a
  `fieldset`/`legend`("Application scoring") with two `buildUSWDSRadioOption`s modeled on
  the eligibility pair (L225-247): ids `program-uses-scoring` / `program-no-scoring`,
  values `true`/`false`, labels "Yes — apply answer option scores to submitted applications"
  / "No". Rendered only when scoring is enabled; the flag is `ADMIN_WRITEABLE`
  (request-aware), and `ProgramFormBuilder` reads flags without a request today (L170), so
  thread `boolean scoringFlagEnabled` from the two view render paths
  (`ProgramNewOneView`, `ProgramMetaDataEditView`, which have the request) through both
  `buildProgramForm` overloads (L92-113, L118-147) into the private canonical builder
  (L150-176).
- `AdminProgramController`: there is **no existing hidden-input preservation pattern**, and a
  hidden input would not stop crafted posts anyway, so preservation is controller-side:
  - `create` (~L165-182): pass `flag ? programData.getUsesScoring() : false`.
  - `update` (~L313-330): pass
    `flag ? programData.getUsesScoring() : existingProgramDefinition.usesScoring()` (the
    definition is already loaded in the update path).
- `ProgramService.createProgramDefinition` (~L404) / `updateProgramDefinition` (~L588): new
  `boolean usesScoring` parameter; `.setUsesScoring(...)` in the update builder chain
  (~L653); into the `ProgramModel` ctor at ~L464. No validation needed (booleans aren't
  validated, matching `eligibilityIsGating`).

### 2.5 Program export/import

- The `@JsonProperty` on `usesScoringInternal` makes the setting ride the export
  automatically. `prepForExport` (`ProgramMigrationService.java:316-327`) must **not** strip
  it — add a regression test asserting it survives, alongside the fields that are stripped.
  `prepForImport` needs no change (absent → false via 2.3).
- Question option scores ride the question definitions' option lists via phase 1's
  `QuestionOption` serde; exports without the field deserialize to empty.
- **Import validation of scores** — two layers:
  1. Raw-tree numeric validation in `ProgramMigrationService.deserialize` (before Jackson
     binding, which would coerce): walk `questions[*].questionOptions[*].score` nodes; each
     must be absent, JSON null, or an integral JSON number within signed 32-bit range.
     Reject floats, strings, and out-of-range values with a validation error surfaced
     through the existing "Error processing JSON" card path
     (`AdminImportController.hxImportProgram` L103). This is deliberately scoped to score
     nodes rather than reconfiguring the migration mapper's coercion rules, which other
     fields may depend on.
  2. `QuestionValidationUtils.validateOptionScores(questions)` (new, registered in
     `ProgramMigrationService.validateQuestions` L166-177): reject any score present on a
     `YES_NO` question's options; defensively re-check range.
- Duplicate-question handling needs **no code change** — scores follow option data through
  the existing `OVERWRITE_EXISTING` / `CREATE_DUPLICATE` / `USE_EXISTING` paths
  (`ProgramMigrationService.saveImportedProgram` L351-380). Tests pin the semantics:
  overwrite adopts imported scores; create-duplicate carries them onto the new question;
  reuse keeps the target environment's question and scores untouched.
- Importing while the flag is off preserves scores and the setting as inert stored
  configuration (import never consults the flag; D1).

### 2.6 Tests

`ProgramDefinitionTest` (serde incl. missing-field fixture), `ProgramModelTest`,
`ProgramServiceTest`, `AdminProgramControllerTest` (flag off: edit preserves stored `true`,
crafted post can't enable, create forces false; flag on: round-trip),
`ProgramMigrationServiceTest` (prepForExport keeps setting; score validation matrix: float /
string / 2^31 / null / absent / yes-no score; duplicate-handling matrix),
`AdminImportControllerTest`, `AdminExportControllerTest`.

**Exit criteria:** the setting round-trips create/edit/export/import; old exports import as
"no"; invalid imported scores are rejected with form-level errors; flag off preserves stored
values end to end.

---

## Phase 3 — Submit-time score snapshot

**Goal:** When the flag is on and the submitted program version has `usesScoring`, submission
adds per-question scores and `total_score` to the application's private JSON snapshot atomically
with activation. Per-question scores are written as siblings of the `selection`/`selections`
answer inside the question's own JSON object; every pre-existing answer value, including checkbox
selection values and ordering, is copied without modification. Duplicate detection ignores the
added score keys.

### 3.1 Score paths + JSON helpers

- Add `ApplicationScoreMetadata` per D4. It owns the `score`/`scores`/`total_score` literals, derives
  the per-question sibling paths from a contextualized question path, exposes the fixed root
  `total_score` path, and provides the reserved-key predicate used by the update precheck.
- Do not change `Scalar`, `ScalarType`, `Scalar.getScalars`, `METADATA_SCALARS`, or question-name
  validation. Existing scalar consumers and persisted scalar names remain unchanged.
- Extend the applicant-update reserved-key precheck (`ApplicantService.java:293-302`, which
  already rejects `updated_at`/`program_updated_in` as a path's final segment) to also reject
  `score`, `scores`, and `total_score`. This reserves scalar-position key names, not question
  admin names: a question admin-named `score` stays legal because it occupies a different path
  depth (D4). Predicates and API bridges cannot select score keys because the keys are not
  scalars.
- `CfJsonDocumentContext`: `readNullableLongList(Path)` per D5 (write side uses the existing
  `putArray(Path, List)` with an `ArrayList<Long>`; add a Javadoc note that this is the
  supported way to persist null-holed numeric arrays).

### 3.2 Score calculator

New `final class ApplicationScoreCalculator` (`services/applicant/`). It receives the dependency
needed to construct a `ReadOnlyApplicantProgramService` over the private snapshot so repeated and
nested-repeated questions expand to concrete `ApplicantQuestion` instances.

`void enrich(ApplicantModel applicant, ApplicantData snapshot, ProgramDefinition submittedVersion)`:

- Traverse `getAllQuestionsIncludingHidden()` on the snapshot-backed read-only service; this
  includes every concrete repeated and nested-repeated instance. Skip types failing
  `QuestionType.supportsOptionScores`. Deduplicate by contextualized question path before
  calculating so a malformed program containing the same path more than once cannot double-count
  a score; follow the exporters' existing `buildKeepingLast` behavior for known issue #9212.
- **Single-select (radio/dropdown):** read the option id (`readLong` at
  `path.join(Scalar.SELECTION)` — the stored value is the option id,
  `SingleSelectQuestion.java:80-82`); look up `QuestionOption` by id in the submitted
  version's definition; if a score is present, write it to
  `ApplicationScoreMetadata.scorePath(path)` — a `score` key inside the question's object, next to
  `selection` — and add it to the running total. No key is written for unscored or unanswered
  questions; export layers emit null from absence.
- **Checkbox:** read the existing selections without modifying them. If the selections path is
  absent, write no per-question key. Otherwise write a same-length nullable score array to
  `ApplicationScoreMetadata.scoresPath(path)` — a `scores` key next to `selections` — preserving the
  stored selection order. Unknown IDs, unscored options, and occurrences after the first
  duplicate ID get null; only the first valid occurrence contributes to the total. This preserves
  the existing answer bytes while ensuring an option contributes at most once.
- Always finish with `putLong(ApplicationScoreMetadata.totalScorePath(), total)` at the document
  root. An enriched application with zero contributions persists `0`; presence of this additive
  root value is the authoritative "scoring was applied" marker.
- Invariant: enrichment only *adds* `score`/`scores` keys to supported-type question objects that
  already exist (an answered question always has its answer and metadata scalars) and the one
  root `total_score` key. It never creates question objects, and never modifies, reorders, or
  removes any existing property.

### 3.3 Submission wiring

`ApplicantProgramReviewController` resolves the request-aware flag and passes `scoringEnabled`
through `ApplicantService.submitApplication`. The service already loads the full
`ProgramDefinition` (L482-531); pass the boolean **and the loaded `ProgramDefinition`** through the
package-private overload (L647-660) into
`ApplicationRepository.submitApplication → submitApplicationInternal` (L86-176). Inside the
existing serializable transaction (`transactionManager.execute`, L91):

1. **Reconcile stale drafts (PRD):** the controller's fast-forward
   (`ApplicantProgramReviewController.java:255`) normally guarantees the draft points at the
   version being submitted, but the repository must not depend on it: if
   `application.getProgram().id != programDefinition.id()`, repoint via
   `application.setProgram(program)` (the `ProgramModel` for `programId` is already loaded
   by `perform`, L182-210). Scores then resolve from the exact version the application is
   associated with.
2. **Private copy:** replace the direct
   `application.setApplicantData(applicant.getApplicantData())` (L168) with
   a new `ApplicantData` built from both `applicant.getApplicantData().asJsonString()` and the
   source's optional preferred locale. Do not use the JSON-only constructor, which would clear the
   submitted application's locale.
   Never mutate `applicant.getApplicantData()` — `ApplicantModel` memoizes it and marks the
   row dirty (`ApplicantModel.java:105-131`), so enriching it in place would leak score
   metadata into the applicant's shared row when `savePrimaryApplicantInfoAnswers` (or any
   later save) runs. This satisfies D6.4.
3. If `scoringEnabled && programDefinition.usesScoring()`:
   `calculator.enrich(applicant, snapshot, programDefinition)`. The calculator only adds sibling
   score keys and the root `total_score` per the 3.2 invariant; it never modifies an existing
   answer value. When scoring is not applied, the snapshot is byte-identical to today's behavior.
4. `application.setApplicantData(snapshot)` and proceed with the existing
   activate/obsolete/save flow. Enrichment, activation, and save share the one transaction —
   the application cannot go active without its complete score snapshot.

Enrichment happens only at submit, never at per-block save, so mid-draft score edits can't
go stale (PRD).

### 3.4 Duplicate detection (`ApplicantData.java:103-134`)

The comparison is live-applicant JSON vs previous snapshot, string equality after scrubbing
`$..updated_at`. Extend `clearFieldsNotRequiredForComparison`:

- Delete score keys only from objects that are actually scored answers, via filtered JsonPath:
  `$..[?(@.selection && @.score)].score` and `$..[?(@.selections && @.scores)].scores`. An object
  holding a `selection`/`selections` key is a single/multi-select question object, and a
  `score`/`scores` key inside such an object can only be ours (D4). Do **not** use a bare
  recursive `$..score` delete: it would also remove a question subtree admin-named `score` from
  both sides of the comparison and blind duplicate detection to real changes in that question.
- Delete the exact root `$.total_score`. Tolerate `PathNotFoundException` on all deletes, matching
  the existing `updated_at` handling: a filtered path with zero matches throws it (verified
  against jayway 3.0.0 with the Jackson provider), while a path with matches deletes them all
  cleanly, so the catch never masks partial scrubbing.
- Do not normalize or reorder any `selections` arrays. Since submission no longer rewrites checkbox
  answers, score support preserves today's duplicate-comparison semantics for checkbox and map
  questions.

### 3.5 Tests

- `ApplicationScoreCalculatorTest`: scored/unscored/mixed selections, negatives, zero-sum
  persists `0`, duplicate + out-of-definition checkbox ids represented by null without changing
  selections, stored-order alignment, repeated and nested-repeated questions, unanswered questions,
  duplicate contextualized paths counted once, Yes/No excluded, 64-bit total with extreme 32-bit
  scores.
- `ApplicationRepositoryTest`: flag off / program off → snapshot has no score keys and no
  `total_score`; flag+program on → keys present as siblings of the answers; stale-draft
  reconciliation repoints and scores from the submitted version; duplicate submission of
  identical answers to a scoring program is still detected (previous snapshot has score keys,
  live doesn't); submitted snapshot preserves every pre-existing value below `applicant` and a
  non-English preferred locale.
- `ApplicantDataTest`: scrub removes only sibling `score`/`scores` keys and the root
  `total_score`; question subtrees admin-named `score`, `scores`, or `total_score` below
  `applicant` remain in the comparison; scrub is a no-op on documents with no scored answers.
- `ApplicantServiceTest`: crafted block update whose path ends in `score`, `scores`, or
  `total_score` is rejected before staging.
- `CfJsonDocumentContextTest`: nullable-list round-trip (incl. all-null and empty).

**Exit criteria:** snapshots carry correct score keys exactly when flag+program agree; every
pre-existing value below `applicant` and the preferred locale are unchanged; the applicant row
never carries score keys; duplicate detection is score-blind without changing existing answer
comparison semantics.

---

## Phase 4 — Admin PDF export

**Goal:** Admin application PDFs show a "Total score" line at the top and per-answer scores;
applicant PDFs never do.

- Add `boolean includeScores` to
  `PdfExporterService.generateApplicationPdf(application, isAdmin)`
  (`PdfExporterService.java:30`) and `PdfExporter.exportApplication` / `buildApplicationPdf`
  (`PdfExporter.java:100-135, 143`). Callers:
  `AdminApplicationController.download` (L281, passes
  `settingsManifest.getAnswerOptionScoringEnabled(request)`);
  `UpsellController.download` (L225, passes `false` unconditionally). Consider bundling the
  growing parameter list into a small options value object while touching the signature.
- Gate: `boolean showScores = isAdmin && includeScores && totalScore.isPresent()` where
  `totalScore` is read once from `application.getApplicantData()` (a fresh private copy,
  `ApplicationModel.java:133-140`) at `ApplicationScoreMetadata.totalScorePath()`. This encodes all
  three PRD conditions; a persisted `0` renders "Total score: 0".
- **Total line:** insert a `Paragraph("Total score: " + total)` in the header block between
  `submitTimeInformation` and `Chunk.NEWLINE` (`PdfExporter.java:163-181`).
- **Per answer:** scores are read from the snapshot by `answerData.contextualizedPath()` —
  never added to `AnswerData` or any shared answer-text object (D6, PRD).
  - Single-select: read `ApplicationScoreMetadata.scorePath(contextualizedPath)` and, when present,
    render a small "Score: N" line under the answer text (same style as the existing eligibility
    annotation at L230).
  - Checkbox: read the unchanged stored selection IDs and the sibling `scores` array, validate
    equal lengths, and build an option-id-to-score lookup. Render selected options in the PDF's
    existing definition order as `optionText (Score: N)`, omitting the suffix for null entries.
    Do not assume the raw option-list order equals `displayOrder`. If lengths disagree, log and
    render the answer without scores rather than mispairing.
  - Answers whose questions aren't in the supported set, or with no persisted score keys,
    render exactly as today.
- Tests (`PdfExporterTest`, `PdfExporterServiceTest`, fixtures via `AbstractExporterTest`):
  admin + flag + scoring → total and per-answer lines; zero total renders; admin with flag
  off → no score text; applicant PDF with all conditions otherwise true → no score text;
  application predating scoring (no `total_score`) → no score text; checkbox pairing and
  mismatch fallback.

**Exit criteria:** score text appears only under the triple gate; applicant PDFs are
byte-equivalent to pre-feature output for non-scored content.

---

## Phase 5 — CSV export

**Goal:** One score column per scored question in the program application CSV, adjacent to
the question's existing columns; demographic CSV never has them.

- **Threading:** `AdminApplicationController.downloadAll` (L188) resolves the flag and passes
  `includeScores` into `CsvExporterService.getProgramAllVersionsCsv` (L74);
  `getDemographicsCsv` (L244) hard-codes `false`. Per the PRD this is an explicit argument,
  not an injected flag.
- **Column qualification** (`generateCsvConfig`, `CsvExporterService.java:102-128`): while
  streaming applications to collect `uniqueQuestions`, also build
  `Set<Path> scoredQuestionPaths`: add a question's contextualized path when its type is in
  the supported set, the application's program version has `usesScoring`, and that version
  of the question has ≥1 scored option. This follows the export's application-driven
  behavior — columns come from represented versions only. `buildColumnHeaders` (L183) passes
  `includeScores && scoredQuestionPaths.contains(path)` into
  `CsvColumnFactory.buildColumns`.
- **Column placement/shape** (`CsvColumnFactory`): emit the score column inside each
  question's own column stream, so adjacency is structural:
  - Radio/dropdown: immediately after the `(selection)` column; header via
    `formatHeader(q.getContextualizedPath().join("score"))` → `... (score)`, matching the existing
    suffix format (L416-462). With sibling storage this is the same join
    `ApplicationScoreMetadata.scorePath` uses, so header and stored value derive from one path.
  - Checkbox: once, after all per-option columns
    (`buildColumnsForMultiSelectQuestion`, L253-276); same `(score)` header.
  - This intentionally follows the PRD's adjacency decision rather than the append-at-end
    backwards-compat convention (`CsvExporterService.java:229`) — safe because columns exist
    only when the flag is on and a represented version scores; call it out in the PR.
- **Cell extractors:** read the row-specific persisted snapshot through
  `question.getApplicantQuestion().getApplicantData()` and derive the score path from that
  question's contextualized path. Do not capture the exemplar question used to construct the
  column.
  - Blank when: question unanswered, `ApplicationScoreMetadata.totalScorePath()` absent in that
    application (scoring not applied — covers flag-off-era, program-off, and pre-feature
    applications), single-select option unscored, or checkbox `scores` all-null.
  - Radio/dropdown: the persisted `score` value.
  - Checkbox: 64-bit sum of non-null entries in the persisted `scores` list
    (`readNullableLongList`); explicit `0` and cancel-to-zero sums render `0`. A length mismatch
    with the unchanged selections array is treated as corrupt metadata and renders blank.
  - `NOT_AN_OPTION_AT_PROGRAM_VERSION` needs no special casing — score cells just render
    blank per the rules above.
- No application-level total column (PRD out-of-scope).
- Tests (`CsvExporterServiceTest` on `AbstractExporterTest` fixtures): column presence matrix
  (flag off → never; scoring-off versions only → no column; mixed versions → column exists,
  scoring-off rows blank); adjacency assertions on header order; cell matrix (blank vs `0`,
  negative, mixed scored/unscored checkbox, sum); demographic CSV never gains columns even
  with flag on.

**Exit criteria:** with the flag off the CSV is byte-identical to today; with it on, columns
appear exactly per the qualification rules.

---

## Phase 6 — JSON/API export, samples, OpenAPI

**Goal:** `score`/`scores` per question and top-level `total_score` in the JSON export and
API, mirrored in samples and both OpenAPI schema versions; all omitted when the flag is off.

- **Threading:** `ProgramApplicationsApiController.list` (L82) and
  `AdminApplicationController.downloadAllJson` (L144) resolve the flag →
  `JsonExporterService.export` / `exportPage` gain `boolean includeScores`.
- **Per-question emission — central helper, not per-presenter:** rather than changing all 15
  `QuestionJsonPresenter` implementations' signatures, add a helper in
  `JsonExporterService` that runs after the presenter entries for each question (in both the
  template pass, `JsonExporterService.java:97-123`, and the per-application pass): for
  supported-type questions, when `includeScores`:
  - `application.<q>.score` (single-select): `Optional<Long>` read from
    `ApplicationScoreMetadata.scorePath(contextualizedPath)`; empty (unanswered / unscored option /
    scoring not applied) → JSON null via the existing dispatch. Note the persisted key and the
    emitted API property now share the same sibling shape.
  - `application.<q>.scores` (checkbox): `null` when scoring was not applied to the
    application (`total_score` absent — read once per application); `[]` when scoring
    applied but unanswered. Otherwise pair the unchanged stored selection IDs with the persisted
    sibling scores, then emit scores in the same definition order and duplicate-filtering used by
    the existing `selections` presenter. Emitted through the `NullableLongArray` wrapper + new
    dispatch branch + `putNullableLongArray` (D5) — without this the value would be silently
    dropped (`JsonExporterService.java:271-293`).
  - The template contains questions from all represented versions. After cloning it for an
    application, initialize every checkbox score path per application (`null` without total-score
    metadata, `[]` with it) before overlaying persisted values; a single static template value
    cannot represent both states.
  - For repeated questions, read from the contextualized score path but apply
    `asNestedEntitiesPath()` to the emitted API path, matching the existing presenters.
  - Values come from the persisted snapshot only — never re-resolved from current question
    versions, so exports are stable across later score edits (PRD).
  - When the flag is off the helper is skipped entirely: properties absent, not null.
- **Top-level `total_score`:** `ApplicationExportData` gains `Optional<Long> totalScore()`
  + builder setter (`JsonExporterService.java:330-403`); populated in
  `buildApplicationExportData` (L162) from `ApplicationScoreMetadata.totalScorePath()`; emitted in
  `convertExportDataToJson` (L215-269) as a nullable 64-bit value when `includeScores`,
  omitted when off. Null when the application predates scoring or scoring wasn't applied;
  explicit `0` passes through.
- **Samples:** `ProgramJsonSampler` (L51) must set the new builder field (its `build()`
  throws otherwise) and gains the same `includeScores` threading from
  `ApiDocsService.getSampleJsonPreview` (L70); `QuestionJsonSampler` adds sample
  `score`/`scores` for the three supported types when enabled. Tests exercise non-null,
  null, empty, negative, and zero values (PRD).
- **OpenAPI** (`services/openapi/`): both generators are hand-written and must be updated in
  lockstep (no scalar-driven pickup, since scores are deliberately not normal scalars):
  - Thread the flag from `OpenApiSchemaController.getSchemaByProgramSlug` (L60) — add
    `includeScores` to the `OpenApiSchemaSettings` record.
  - Per question in `buildApplicationDefinitions` (v3 L263-313, v2 L214-262), for supported
    types only: `score` nullable int32 (single-select), `scores` nullable array with
    nullable int32 items (checkbox; v2 nullability via the existing
    `x-nullable` vendor-extension idiom, including on items).
  - Top-level `total_score` nullable int64 in both hardcoded result property lists
    (v3 L70-115, v2 L154-185).
  - Flag off: properties absent from generated schemas.
- Tests: `JsonExporterServiceTest` (`ResultAsserter`): full semantics matrix incl. template
  nulls for never-answered questions, flag-off omission, stability after a post-submission
  score edit; `ProgramJsonSamplerTest` / `QuestionJsonSamplerTest`; both OpenAPI generator
  tests; `ProgramApplicationsApiControllerTest`.

**Exit criteria:** API consumers see the documented nullable semantics; samples, schemas,
and real output agree; flag off leaves responses byte-identical to today.

---

## Phase 7 — Hardening, audit, rollout

- **Applicant-invisibility audit:** grep-level sweep proving no applicant-facing view,
  fragment, TS bundle, or response serializes `score` (D6): `views/questiontypes/**`,
  applicant controllers, `preview.ts` output, API-bridge paths, predicate evaluators.
  Codify the critical ones as tests where practical (e.g. applicant question fragment
  rendering with a scored option contains no score markup).
- **Browser tests:** a separate PR under `browser-test/` (repo convention: not part of
  server changes): admin creates a scored question + scoring program, submits as applicant,
  verifies CSV/JSON downloads and admin PDF, verifies applicant summary/PDF show nothing.
- **Rollout:** flag defaults false everywhere; enable per deployment. Before promoting the
  flag out of Experimental: confirm program-migration round-trips between a flag-on and
  flag-off environment (phase 2 semantics), and that demographic exports remain unchanged.

---

## Risks and verification items

1. **`ProgramDefinition` Optional-internal default (2.3)** — verify AutoValue + Jackson
   builder deserialization with a pre-feature fixture early in phase 2; do not rely on raw-JSON
   preprocessing.
2. **Sibling score paths (D4/3.1/3.4)** — test top-level, repeated, and nested-repeated
   contextualized paths: every per-question score key must land inside that question's own JSON
   object, and no `Scalar` or `ScalarType` change is allowed. The filtered JsonPath deletes in
   duplicate detection (`$..[?(@.selection && @.score)].score`) are verified against jayway 3.0.0
   with the app's Jackson provider config: zero matches throw `PathNotFoundException` (caught,
   like `updated_at`), partial matches delete cleanly. Unit tests pin this.
3. **Answer preservation (D0/3.3)** — compare the complete `applicant` subtree before and after
   enrichment, including checkbox order, unknown IDs, duplicate IDs, and map selections; every
   pre-existing property must be byte-identical, with the only differences being added
   `score`/`scores` keys inside supported-type question objects and the root `total_score`.
4. **Play binding of parallel `List<String>` score fields (1.4)** — blank trailing entries
   are the classic failure; the real-bind test in phase 1 is mandatory, and the TS row
   template must always emit the input (even empty) so cardinality holds.
5. **Grid layout of the option row (1.6)** — `grid-rows-4 → grid-rows-6` needs a visual
   check; browser-test selectors in `browser-test/src/support/admin_questions.ts:105-113`
   key off existing classes and must not break.
6. **CSV adjacency vs append-at-end convention (5)** — deliberate PRD choice; flag-gated, but
   reviewers will ask. The PR description should preempt it.
7. **Backward-compatibility fixtures (D0)** — deserialize pre-feature question options, program
   exports, applicants, and application snapshots without preprocessing; assert no existing row is
   rewritten merely by deploying the feature, and assert application/API/PDF/CSV outputs retain
   their existing shapes with the flag off.
