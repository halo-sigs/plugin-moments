## ADDED Requirements

### Requirement: Extension index API migration
The system SHALL use the new Halo Extension Index APIs (`IndexSpecs` and `Queries`) in place of deprecated `IndexSpec` + `IndexAttributeFactory` and `QueryFactory`.

#### Scenario: Plugin starts with new index API
- **WHEN** the plugin starts
- **THEN** `Moment` indexes are registered via `IndexSpecs.single()` and `IndexSpecs.multi()`
- **AND** all field-selector queries are built via `Queries`

#### Scenario: Moment list queries work after migration
- **WHEN** a client queries moments via Console, UC, Public, or Finder APIs
- **THEN** `FieldSelector` queries produce identical results to the pre-migration implementation
