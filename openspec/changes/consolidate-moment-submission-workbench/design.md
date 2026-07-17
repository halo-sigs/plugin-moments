## Context

The frontend has two Moment editor modules: one for Console and one for User
Center. They duplicate draft initialization, editor rendering, tag derivation,
attachment normalization and validation, visibility controls, submission state,
cache invalidation, and feedback. Their meaningful differences are persistence,
tag lookup, and host permissions.

Both editors currently start asynchronous persistence without awaiting it. As a
result, `saving` can reset immediately, failures escape the surrounding error
handling, and a new draft can be cleared before persistence succeeds.

The domain language is recorded in `CONTEXT.md`: submitting a Moment can produce
either a published or pending-review result, while publication specifically means
that review has passed.

## Goals / Non-Goals

**Goals:**

- Put the complete Moment submission workbench behind one shared Vue module
  interface.
- Keep submission behavior, state, and verification local to that module.
- Isolate Console and User Center persistence differences behind two real
  adapters.
- Model only author-editable data as a Moment draft.
- Make success, failure, and concurrent-submission behavior observable through
  the shared module interface.
- Add black-box behavioral tests and focused adapter mapping tests.

**Non-Goals:**

- Changing backend endpoints, generated API clients, or authorization rules.
- Changing the editor layout, keyboard shortcut, visibility control, attachment
  types, duplicate detection, or nine-attachment limit.
- Changing Moment review, ownership, release-time, or storage semantics.
- Refactoring the Moment list or item modules beyond replacing their editor
  imports and wiring the shared module.

## Decisions

### Use one shared workbench module, not a shared composable under two editors

The existing Console editor becomes the shared workbench implementation and the
duplicate User Center editor is removed. The shared module owns the editor UI,
draft state, tag derivation, attachment rules, visibility changes, submission
state, feedback, and post-submit cache invalidation.

Extracting only a composable was rejected because it would retain two templates
and two submission call sites. The current asynchronous lifecycle defect exists
in those call sites, so that alternative would not provide sufficient locality.

### Inject an explicit persistence adapter instead of a mode flag

The workbench receives a required persistence adapter. The adapter exposes one
submission operation and internally maps create or update intent to the
appropriate generated client calls. Console and User Center each provide an
adapter; tests provide an in-memory adapter.

A `console`/`uc` mode flag was rejected because every new host difference would
add correlated branches to the shared implementation. Global state and
`provide`/`inject` were rejected because this dependency is shallow in the tree
and should remain visible at the seam.

Tag lookup remains a separate explicit input because it is an editor query
dependency, not persistence behavior. Host-specific submission permission
directives remain outside the shared workbench; backend authorization remains
authoritative.

### Submit a draft, not a full Moment

The shared draft contains author-editable content and visibility. Tags are
derived from `content.raw` at submission time. Moment identity is supplied only
for update intent.

Owner, release time, approval state, approval time, and metadata are excluded
from the draft. The persistence adapters preserve or assign those fields using
the existing backend contracts. This prevents generated transport types and
server-owned state from becoming part of the workbench interface.

### Return a normalized submission result

The persistence adapter converts its backend response into a small result that
identifies the Moment and reports `published` or `pending-review`. The shared
workbench uses this result for accurate feedback.

Returning the complete generated Moment response was rejected because the
workbench needs only identity and review outcome.

### Define persistence acknowledgement as the success boundary

The workbench awaits the adapter submission. While it is pending, the workbench
keeps the draft intact, exposes submitting state, and ignores additional button
or keyboard submissions.

After persistence succeeds:

- a new submission resets the draft;
- an update emits completion so its host can leave edit mode;
- feedback distinguishes published from pending review; and
- Moment list cache invalidation runs as a best-effort follow-up.

A cache refresh failure does not turn an already persisted submission into a
failure. On persistence failure, the workbench retains the draft, leaves edit
mode unchanged, shows failure feedback, and permits retry.

### Test through the shared module interface

Add Vitest, Vue Test Utils, and happy-dom. Behavioral tests mount the shared
workbench with an in-memory adapter, drive user-visible interactions, await
asynchronous work, and assert rendered state and emitted outcomes. Tests do not
reach into private composables or rely on full-component snapshots.

Each production adapter gets focused tests for create/update request mapping and
submission-result normalization.

## Risks / Trade-offs

- [Risk] Halo editor and attachment globals make full mounting noisy. → Mitigation:
  stub those external UI modules while preserving their public `v-model` and
  event behavior.
- [Risk] Cache invalidation can reject after persistence succeeds. → Mitigation:
  isolate it as a best-effort follow-up and test that submission outcome remains
  successful.
- [Risk] Console patch and User Center replace operations preserve fields
  differently. → Mitigation: keep that translation inside adapter tests and send
  only draft-owned fields across the shared seam.
- [Trade-off] The workbench remains coupled to Vue Query and Halo UI primitives.
  This is intentional because it is a frontend feature module, not a
  framework-neutral domain library.

## Migration Plan

1. Add the test runner and prove the current asynchronous defect with a
   behavioral test.
2. Introduce draft, submission intent/result, and persistence-adapter types.
3. Implement Console and User Center adapters against the existing generated
   clients.
4. Convert the Console editor into the shared workbench and migrate both hosts.
5. Remove the duplicate User Center editor after both hosts pass the shared test
   suite.
6. Run unit tests, type checking, linting, and the frontend build.

Rollback consists of restoring the two host-specific editors; no persisted data
or backend contract is migrated.

## Open Questions

None. The submission terminology, success boundary, adapter seam, draft
ownership, failure behavior, concurrency rule, test surface, and frontend-only
scope were resolved before this proposal.
