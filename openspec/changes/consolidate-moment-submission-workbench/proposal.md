## Why

Console and User Center currently duplicate the Moment submission workbench, including
draft handling, tag derivation, attachment rules, and asynchronous submission state.
The duplication has already caused fixes to be applied twice and allows submissions to
finish their UI lifecycle before persistence completes.

## What Changes

- Consolidate Console and User Center Moment editing into one shared submission
  workbench.
- Preserve the existing editor UI, attachment rules, visibility controls, and keyboard
  shortcut.
- Isolate Console and User Center persistence behavior behind explicit adapters.
- Standardize submission outcomes as published or pending review.
- Keep drafts intact on failure and prevent concurrent duplicate submissions.
- Add frontend behavioral tests for the shared submission workflow and adapter mappings.

## Capabilities

### New Capabilities

- `moment-submission-workbench`: Defines shared Moment draft, submission, outcome,
  concurrency, and failure behavior across Console and User Center.

### Modified Capabilities

None.

## Impact

- Affects the Console and User Center Moment editor modules and their API-client
  integration.
- Adds frontend unit-test infrastructure and behavioral tests.
- Does not change backend endpoints, generated API clients, Moment storage, permissions,
  editor visuals, or attachment product rules.
