# PRD: Seed-first test data setup for browser tests

## Problem Statement

The Playwright browser-test suite (100 test files) overwhelmingly builds its test data by clicking through the admin UI. There are ~730 UI-driven creation calls (`adminPrograms.addProgram`: 335, `adminQuestions.add*Question`: ~395) across 79 files, plus ~200 UI-driven publish actions. Each UI-driven program creation costs multiple full page loads (~3–6s); each question creation costs ~2–4s. The suite runs with a single worker and is sharded 10 ways across 2 cloud configurations in CI, so this setup cost is paid on every shard of every PR.

Beyond speed, this couples nearly every test to the admin creation UI: a markup change to the program or question forms can break dozens of tests whose subject has nothing to do with those forms, and the creation flows themselves get redundantly re-verified hundreds of times.

An API-driven `Seeding` helper already exists (hitting the headless dev endpoints `seedQuestionsHeadless`, `seedProgramsHeadless`, `seedApplicationsHeadless`, `clearHeadless`) but only ~22 files use it, mostly for the canned sample data.

## Solution

Make API seeding the default way browser tests obtain programs, questions, and applications, and reserve UI-driven creation for the small set of tests where the creation flow itself is the feature under test.

Three parts:

1. **Adopt the existing canned seeds** wherever a test just needs "some published program with questions" — the seeder already creates 17 sample questions (one per question type) and two sample programs (minimal, and a comprehensive one with 6 blocks, an enumerator, and a visibility predicate).
2. **Close the gaps in the headless seeding API** — most importantly a headless publish (seeded data lands as drafts today, forcing a UI round-trip through "publish all drafts") and a parameterized "seed this program definition" endpoint that accepts the existing program-export JSON format, so tests needing custom structures (specific names, predicates, eligibility) can declare them as fixtures instead of clicking them together.
3. **Designate a small set of critical-path tests that stay fully manual** end-to-end, preserving true user-journey coverage of the creation and publish flows.

## User Stories

1. As a CiviForm developer, I want browser tests to set up their data via API seeding, so that a full test run (and each CI shard) completes minutes faster.
2. As a test author, I want a one-line seeding call that gives me a published program with representative questions, so that I don't write 30 lines of UI-driven setup for a test about something else.
3. As a test author, I want to seed a custom program from a JSON fixture (custom name, blocks, predicates, eligibility), so that tests needing specific structures don't fall back to UI creation.
4. As a test author, I want a headless "publish all drafts" call, so that seeded data can reach the ACTIVE stage without navigating the admin UI.
5. As a maintainer of the admin UI, I want most tests decoupled from the program/question creation forms, so that changing those forms breaks only the tests that actually cover them.
6. As a maintainer, I want a small, named set of fully-manual critical-path tests, so that the real end-to-end journeys (admin builds → publishes → applicant applies → admin reviews) keep genuine UI coverage.
7. As a reviewer, I want a documented convention (seed by default, UI-create only when the flow is under test), so that new tests don't regress to manual setup.
8. As a developer iterating locally, I want individual data-heavy test files to run in a fraction of the current time, so that the edit-run loop is tolerable.

## Critical-Path Tests to Keep Fully Manual

These stay UI-driven end-to-end, deliberately. Everything else is a candidate for seeded setup.

1. **admin_program_creation.test.ts** — the program creation UI is the feature under test; seeding it away would delete the coverage.
2. **admin_question_types.test.ts** + **question_lifecycle.test.ts** — question creation forms and the draft/edit/delete lifecycle are the subject.
3. **admin_publish.test.ts** — the draft → publish flow is the subject; this also backstops the headless publish endpoint against the real UI behavior.
4. **end_to_end_enumerators.test.ts** — the most complex data structure in the system, exercised as a full journey: admin builds the enumerator program via UI, applicant fills repeated screens, admin reviews. Keep as the deep-structure canary.
5. **application_review.test.ts** — one complete happy-path journey (admin creates and publishes via UI, applicant applies, admin reviews the application) retained as the suite's end-to-end smoke test.

Partial case: **admin_predicates.test.ts** / **admin_predicates_expanded.test.ts** (96 + 25 creation calls, 4 tests already marked slow). The predicate _editing_ UI is under test, but the programs and questions it operates on are pure setup — seed those, keep the predicate configuration steps manual. Same pattern applies to translation, statuses, and image tests: seed the nouns, click the feature.

## Implementation Decisions

- **Setup convention**: API seeding is the default for programs, questions, and applications. UI-driven creation helpers remain supported but are reserved for tests where creation/edit flows are the subject. Documented in the browser-test README and enforced in review.
- **Phase 1 — adopt canned seeds (no server changes)**: migrate tests that only need generic data onto `seedQuestions` / `seedProgramsAndCategories`. Assertions tied to bespoke names get rewritten against the sample names ("Sample Address Question", minimal/comprehensive sample program). Prioritize by creation-call count and `test.slow()` markers.
- **Headless publish**: add a dev-only headless endpoint that promotes all drafts to ACTIVE (equivalent of the admin "publish all drafts" action), plus a `Seeding.publishAllDrafts()` wrapper. Removes ~200 UI publish round-trips.
- **Phase 2 — parameterized program seeding**: add a dev-only headless endpoint that accepts the existing program-export JSON (the format the admin program migration/import feature already parses) and creates the program, resolving or creating its questions. Tests store custom programs as JSON fixtures. Reuses the existing migration service rather than inventing a new schema.
- **Applications**: continue using the existing parameterized application seeding for volume cases (pagination, bulk status updates); extend it to accept answer data only if a migrated test requires submitted (not empty) applications.
- **Isolation model unchanged**: keep the per-test database clear in the shared fixture. Seeding calls happen in `beforeEach` after the clear, as today.
- **Access control unchanged**: dev endpoints stay gated by the existing demo-mode setting; no new exposure surface. New endpoints are headless JSON, mirroring the existing `*Headless` pattern.
- **Migration is incremental and file-by-file**: top offenders first (predicates, application_review setup portions, question/program list tests, trusted_intermediary, applicant question tests). No big-bang rewrite; UI helpers are not deleted.

## Feasibility

**Phase 1 is low-risk and needs zero server work.** The endpoints, the `Seeding` fixture, and the per-test `clearDatabase` wiring all exist and are already used by 22 files. The dev routes are enabled in the browser-test environment (demo-mode gating defaults open). The seeder is idempotent and runs in a serializable transaction.

The real costs and risks:

- **Assertion rewrites, not plumbing.** Most migration effort is renaming expectations to the canned sample names and adapting to the comprehensive program's fixed block structure. Mechanical but broad.
- **Canned data is fixed.** The seeder offers exactly 2 programs and 17 questions with hardcoded structure. Tests needing custom names, specific block layouts, eligibility, or multiple programs can't use it — that's the majority of the remaining files, which is why Phase 2 (parameterized seeding via the import-JSON path) is required to move the bulk of the 730 calls. The import format and migration service already exist and are exercised by the program migration tests, so the new endpoint is a thin dev-only wrapper, not new machinery.
- **Draft vs. active.** Seeded data lands in the draft version; applicant-facing tests need ACTIVE. Until the headless publish endpoint exists, seeded tests still pay one UI publish round-trip, which caps Phase 1 savings.
- **Coverage regression risk.** Every UI-created program incidentally re-tests the creation forms. The named critical-path tests are the deliberate replacement for that incidental coverage; the convention only works if they stay healthy and manual.
- **Sample-data drift.** More tests depending on the canned seed means changes to the sample definitions ripple wider. Acceptable: sample data changes rarely and intentionally, unlike admin form markup.

## Estimated Speed Improvements

Ballpark, grounded in call counts (all figures per full suite run, per browser/cloud configuration):

- **Per-call delta**: UI program creation ≈ 3–6s (4+ page navigations); UI question creation ≈ 2–4s; a headless seed POST ≈ 0.1–0.5s, and one call seeds everything at once. UI publish-all-drafts ≈ 3–5s vs. a sub-second headless call.
- **Replaceable volume**: of ~730 creation calls, roughly 550–600 sit in tests where creation is setup, not subject. At ~3s average that's **~28–30 minutes of UI setup**, replaced by seed calls costing well under 2 minutes total. Headless publish removes most of the ~200 publish actions: another **~10–15 minutes**.
- **Net suite effect**: roughly **35–45 minutes saved per full run**, spread across 10 CI shards ≈ **3–4 minutes per shard job** (and that × 20 shard jobs per PR when both clouds run). Against typical data-heavy shard runtimes that's a ballpark **20–35% reduction**; setup-dominated files (predicates, list tests, applicant question tests) should individually run 2–3× faster, which is where local iteration feels it most.
- **Not addressed**: per-test database clears, login flows, and feature-flag toggles are untouched by this PRD and bound the ceiling. Treat all numbers as estimates to be validated by timing 2–3 migrated files against their current versions before committing to the full migration.

## Out of Scope

- Parallelizing the suite (multiple Playwright workers) or changing shard counts.
- Speeding up login, feature-flag toggling, or the per-test database reset.
- Seeding for unit/Java tests or the staging probers.
- Deleting the UI creation helpers or rewriting the kept critical-path tests.
- Exposing seeding endpoints outside dev/test gating.

## Further Notes

- Validate the estimates early: migrate one setup-heavy file (a predicates or list test) in the first PR and compare wall-clock before/after; abort or rescope Phase 2 if the delta disappoints.
- The parameterized endpoint doubles as a developer tool: a library of program JSON fixtures is also useful for manual testing on dev instances.
- Fixture-ordering gotcha already documented in the fixtures file: the seeding fixture must be created after the page fixture or seeded data gets wiped by the per-test clear — new seeding helpers must preserve that ordering.
