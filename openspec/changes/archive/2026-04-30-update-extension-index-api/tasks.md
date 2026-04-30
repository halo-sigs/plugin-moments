## 0. Prerequisites

- [x] 0.1 Update `sourceCompatibility` from Java 17 to Java 21 in `build.gradle`
- [x] 0.2 Update Halo platform dependency from `2.20.11` to `2.22.0` in `build.gradle`
- [x] 0.3 Update Lombok plugin from `8.0.0-rc2` to `8.6` in `build.gradle`
- [x] 0.4 Update Halo plugin devtools from `0.4.1` to `0.6.2` in `build.gradle`

## 1. Migrate index registration in MomentsPlugin

- [x] 1.1 Replace `IndexAttributeFactory` and `IndexSpec` imports with `IndexSpecs` in `MomentsPlugin.java`
- [x] 1.2 Convert `spec.tags` index to `IndexSpecs.multi("spec.tags", String.class)`
- [x] 1.3 Convert `spec.owner` index to `IndexSpecs.single("spec.owner", String.class)`
- [x] 1.4 Convert `spec.releaseTime` index to `IndexSpecs.single("spec.releaseTime", Instant.class)`
- [x] 1.5 Convert `spec.visible` index to `IndexSpecs.single("spec.visible", String.class)`
- [x] 1.6 Convert `spec.approved` index to `IndexSpecs.single("spec.approved", Boolean.class)`
- [x] 1.7 Convert `requireSyncOnStartup` index to `IndexSpecs.single("requireSyncOnStartup", Boolean.class)`

## 2. Migrate QueryFactory to Queries in query classes

- [x] 2.1 Replace `QueryFactory` imports with `Queries` in `MomentQuery.java`
- [x] 2.2 Replace `QueryFactory` imports with `Queries` in `MomentPublicQuery.java`
- [x] 2.3 Replace `QueryFactory` imports with `Queries` in `DefaultQueryMomentPredicateResolver.java`
- [x] 2.4 Replace `QueryFactory` imports with `Queries` in `MomentFinderImpl.java`
- [x] 2.5 Replace `QueryFactory` imports with `Queries` in `MomentMigration.java`
- [x] 2.6 Replace `QueryFactory` imports with `Queries` in `SubscriptionMigration.java`
- [x] 2.7 Replace `QueryFactory` imports with `Queries` in `MomentReconciler.java`
- [x] 2.8 Replace `QueryFactory` imports with `Queries` in `MomentHaloDocumentsProvider.java`

## 3. Verify and test

- [x] 3.1 Run `./gradlew build` to ensure compilation succeeds
- [ ] 3.2 Run `./gradlew haloServer` and smoke-test moment CRUD operations
- [ ] 3.3 Verify list/filter/tag queries still return correct results
- [ ] 3.4 Check that `MomentMigration` reconciler triggers correctly for unapproved moments on startup
