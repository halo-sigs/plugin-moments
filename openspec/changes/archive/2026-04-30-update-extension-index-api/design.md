## Context

The plugin currently uses the deprecated Halo Extension Index API:

- `IndexSpec` + `IndexAttributeFactory.simpleAttribute()` / `multiValueAttribute()` to register indexes in `MomentsPlugin.start()`
- `QueryFactory.all()`, `and()`, `or()`, `equal()`, `contains()`, `greaterThanOrEqual()`, `lessThanOrEqual()`, `isNull()` to build `FieldSelector` queries across 8 source files

Halo 2.20+ replaced these with `IndexSpecs` (typed index declarations) and `Queries` (field-selector query builders). The platform dependency is already at `2.20.11`, so the new APIs are available.

## Goals / Non-Goals

**Goals:**
- Migrate index registration in `MomentsPlugin` to `IndexSpecs.single()` / `IndexSpecs.multi()` with typed keys
- Migrate all `QueryFactory` usages to `Queries` equivalent methods
- Ensure `FieldSelector` behavior remains identical after migration
- Keep `requireSyncOnStartup` logic functional (used by `MomentReconciler` startup matcher)

**Non-Goals:**
- Adding new indexes or changing index behavior
- Changing public API contracts (endpoints, Finder API, VO shapes)
- Frontend changes

## Decisions

### Use typed keys in IndexSpecs

The new API supports `String`, `Boolean`, `Integer`, `Long`, `Instant` as key types. We will use the most appropriate type for each index:
- `spec.tags` → `IndexSpecs.multi("spec.tags", String.class)`
- `spec.owner` → `IndexSpecs.single("spec.owner", String.class)`
- `spec.releaseTime` → `IndexSpecs.single("spec.releaseTime", Instant.class)`
- `spec.visible` → `IndexSpecs.single("spec.visible", String.class)`
- `spec.approved` → `IndexSpecs.single("spec.approved", Boolean.class)`
- `requireSyncOnStartup` → `IndexSpecs.single("requireSyncOnStartup", Boolean.class)`

**Rationale:** Typed keys are safer and avoid string serialization bugs (e.g., `"true"` vs `true`). `Instant` is directly supported, so we no longer need `.toString()` conversion.

### Keep `requireSyncOnStartup` index logic unchanged

The `requireSyncOnStartup` index is an internal mechanism to trigger reconciliation for moments whose `observedVersion` is stale. We will migrate it to `IndexSpecs.single(..., Boolean.class)` but preserve the same index function logic (return `Boolean.TRUE` when `observedVersion < version`, else `null`).

**Rationale:** The reconciler relies on this for startup migration. Changing the logic would risk missing un-reconciled moments.

### Migrate QueryFactory to Queries one-to-one

`Queries` provides the same static methods as `QueryFactory` (`all()`, `and()`, `or()`, `equal()`, `contains()`, `greaterThanOrEqual()`, `lessThanOrEqual()`, `isNull()`), so each call site can be replaced directly with the new import.

**Rationale:** No behavioral changes are needed; this is a drop-in import replacement.

## Risks / Trade-offs

- **Risk:** `Queries` API signatures may differ slightly from `QueryFactory` (e.g., method overloads, generic types). → **Mitigation:** Verify each call site compiles after migration; the methods are designed to be backward-compatible in behavior.
- **Risk:** `IndexSpecs` with typed keys may change how values are stored/queried internally. → **Mitigation:** Run `./gradlew build` and `./gradlew test` (if tests exist) to verify compilation. Test with `./gradlew haloServer` and exercise moment CRUD + list operations.
- **Risk:** The `FieldSelector.of(query)` and `fieldSelector.andQuery(...)` APIs may have changed alongside `QueryFactory`. → **Mitigation:** Check Halo source or API Javadoc for `FieldSelector` compatibility; if changed, update builder patterns accordingly.
