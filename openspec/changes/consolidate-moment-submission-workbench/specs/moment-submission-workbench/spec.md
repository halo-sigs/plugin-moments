## ADDED Requirements

### Requirement: Shared Moment submission behavior
The system SHALL provide the same Moment drafting and submission behavior in
Console and User Center while preserving each host's persistence and permission
rules.

#### Scenario: Submit from Console
- **WHEN** an authorized Console user submits a valid Moment draft
- **THEN** the system persists it through Console behavior and reports the returned submission outcome

#### Scenario: Submit from User Center
- **WHEN** an authorized User Center user submits a valid Moment draft
- **THEN** the system persists it through User Center behavior and reports the returned submission outcome

### Requirement: Moment draft ownership
The system SHALL treat only author-editable content, attachments, and visibility
as Moment draft state, and SHALL derive tags from the editor content at
submission time.

#### Scenario: Derive tags during submission
- **WHEN** the submitted editor content contains Moment tag nodes
- **THEN** the persisted submission contains the unique tags derived from those nodes

#### Scenario: Exclude system-managed state
- **WHEN** a Moment draft is submitted
- **THEN** owner, release time, approval state, approval time, and metadata are not accepted as author-editable draft state

### Requirement: Preserve existing editor behavior
The shared workbench SHALL preserve the existing editor layout, visibility
control, keyboard shortcut, supported attachment types, duplicate detection, and
nine-attachment limit.

#### Scenario: Reject a duplicate attachment
- **WHEN** the author selects an attachment whose URL already exists in the draft
- **THEN** the workbench leaves the draft attachments unchanged and shows the existing duplicate warning

#### Scenario: Enforce the attachment limit
- **WHEN** the draft already contains nine attachments
- **THEN** the workbench prevents another attachment from being added and shows the existing limit warning

### Requirement: Single-flight submission
The system SHALL allow at most one Moment submission from a workbench instance
to be in progress at a time.

#### Scenario: Ignore a concurrent submission
- **WHEN** a submission is pending and the author triggers submit again by button or keyboard
- **THEN** the system performs no additional persistence request

#### Scenario: Expose pending submission state
- **WHEN** persistence has not completed
- **THEN** the workbench remains in submitting state and retains the current draft

### Requirement: Submission outcome
The system SHALL distinguish a published submission from a pending-review
submission using the result returned after persistence.

#### Scenario: Published outcome
- **WHEN** persistence returns a published result
- **THEN** the workbench reports that the Moment was published successfully

#### Scenario: Pending-review outcome
- **WHEN** persistence returns a pending-review result
- **THEN** the workbench reports that the Moment was submitted and is awaiting review

### Requirement: Successful submission lifecycle
The system SHALL consider a Moment submission successful when persistence
acknowledges it, and SHALL treat list-cache invalidation as a best-effort
follow-up.

#### Scenario: Complete a new submission
- **WHEN** persistence acknowledges a new Moment submission
- **THEN** the workbench reports the outcome, resets the draft, and requests Moment list cache invalidation

#### Scenario: Complete an update
- **WHEN** persistence acknowledges an existing Moment update
- **THEN** the workbench reports the outcome, emits update completion, and requests Moment list cache invalidation

#### Scenario: Cache invalidation fails after persistence
- **WHEN** persistence succeeds but Moment list cache invalidation fails
- **THEN** the submission remains successful and the workbench does not restore the submitted draft as a failed submission

### Requirement: Failed submission lifecycle
The system SHALL preserve the author's work and permit retry when Moment
persistence fails.

#### Scenario: Persistence fails
- **WHEN** the persistence adapter rejects a submission
- **THEN** the workbench exits submitting state, retains the draft and edit mode, shows failure feedback, and performs no success follow-up

#### Scenario: Retry after failure
- **WHEN** the author submits again after a failed persistence attempt
- **THEN** the workbench performs a new persistence request with the retained draft
