## Why

Halo 2.20+ updated the Extension index API. The old `IndexAttributeFactory` / `IndexSpec` / `QueryFactory` APIs are deprecated and replaced with `IndexSpecs` / `Queries`. Migrating ensures compatibility with newer Halo versions and removes usage of deprecated APIs.

## What Changes

- Update `MomentsPlugin.java` to register `Moment` indexes using the new `IndexSpecs.single(name, keyType)` and `IndexSpecs.multi(name, keyType)` APIs instead of `new IndexSpec()` + `IndexAttributeFactory`.
- Replace all usages of `run.halo.app.extension.index.query.QueryFactory` (and its static methods: `all`, `and`, `or`, `equal`, `contains`, `greaterThanOrEqual`, `lessThanOrEqual`, `isNull`) with the new `run.halo.app.extension.index.query.Queries` API across:
  - `MomentQuery.java`
  - `MomentPublicQuery.java`
  - `DefaultQueryMomentPredicateResolver.java`
  - `MomentFinderImpl.java`
  - `MomentMigration.java`
  - `SubscriptionMigration.java`
  - `MomentReconciler.java`
  - `MomentHaloDocumentsProvider.java`
- Remove the `requireSyncOnStartup` index and the corresponding `onAddMatcher` in `MomentReconciler` if the new index API provides a different mechanism for startup reconciliation, or update the matcher query accordingly.

## Capabilities

### New Capabilities

- None. This is an internal API migration with no new user-facing behavior.

### Modified Capabilities

- None. No spec-level behavior changes; only implementation details are updated.

## Impact

- All field-selector query construction in the backend moves to the new `Queries` API.
- Index registration in `MomentsPlugin` uses `IndexSpecs` with typed keys (`String`, `Boolean`, `Instant`).
- No changes to public APIs, database schema, or frontend code.
