## 1. Test Foundation

- [x] 1.1 Add Rstest, Vue Test Utils, happy-dom, Vue SFC test configuration, and a real `test:unit` script to the Console workspace
- [x] 1.2 Add a reusable workbench mount harness that stubs Halo editor and attachment UI through their public inputs and events
- [x] 1.3 Add failing black-box tests for awaited persistence, retained drafts while pending, single-flight submission, success completion, and failure retry

## 2. Submission Contracts and Adapters

- [x] 2.1 Define the Moment draft, create/update submission intent, normalized submission result, and single-operation persistence adapter types
- [x] 2.2 Implement and test the Console adapter mappings for create, update, server-managed fields, and published outcomes
- [x] 2.3 Implement and test the User Center adapter mappings for create, update, server-managed fields, and published or pending-review outcomes

## 3. Shared Submission Workbench

- [x] 3.1 Convert the Console Moment editor into the shared workbench with explicit persistence-adapter and tag-query inputs
- [x] 3.2 Limit draft state to editable content and visibility, and derive unique tags from editor content at submission time
- [x] 3.3 Implement awaited single-flight submission with correct pending, success, reset, update-completion, failure, and retry behavior
- [x] 3.4 Implement outcome-specific feedback and best-effort Moment list cache invalidation without changing the persistence success result
- [x] 3.5 Add black-box coverage for published and pending-review outcomes, tag derivation, attachment duplication, the nine-attachment limit, and the existing keyboard shortcut

## 4. Host Migration

- [x] 4.1 Wire the Console host to the shared workbench, Console adapter, Console tag query, and existing host permission gate
- [x] 4.2 Wire the User Center host to the shared workbench, User Center adapter, User Center tag query, and existing host permission gate
- [x] 4.3 Preserve update-mode exit behavior in both Moment item hosts and remove the duplicate User Center editor module
- [x] 4.4 Verify that existing editor layout, visibility control, attachment behavior, and generated API-client files remain unchanged

## 5. Verification

- [x] 5.1 Run the Console unit test suite and confirm every submission scenario passes
- [x] 5.2 Run Console type checking, linting, and the production frontend build
- [x] 5.3 Run `git diff --check` and confirm the change contains no backend contract or generated client edits
