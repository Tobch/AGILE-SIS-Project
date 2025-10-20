# Sprint 1 — Foundations & MVP

### Ticket S1-001

**Title:** Setup Git repository, CI pipeline and Jira project
**Issue Type:** Story
**Priority:** Highest
**Story Points:** 2
**Description:** As an Admin I want to create the Git repo,and Jira project so that the team has a shared codebase, automated builds, and a backlog to track work.
**Acceptance Criteria:**

* Repo created on chosen host with `main` and `develop` branches.
* CI configured (GitHub Actions / Jenkins) to run build & unit tests on PR.
* Jira project and Sprint 1 created with initial backlog epics.
  **Subtasks:**
* Create repo and push initial skeleton.
* Add README, LICENSE, CONTRIBUTING.
* Configure CI workflow for build & test.
* Create Jira project, epics, and Sprint 1 board.

---

### Ticket S1-002

**Title:** JavaFX desktop app skeleton with login, main window and settings
**Issue Type:** Story
**Priority:** Highest
**Story Points:** 5
**Description:** As a Developer I want to launch the JavaFX desktop app skeleton (login, main window, settings) so that we have a working client to attach features to and test UI flows.
**Acceptance Criteria:**

* App starts and shows a login screen.
* Main dashboard window opens after successful login (stub content OK).
* Settings dialog present allowing DB connection configuration and "Test connection".
* App logs and global exception handler implemented.
  **Subtasks:**
* Initialize Maven/Gradle JavaFX project.
* Implement login screen and navigation.
* Implement settings dialog for DB config.
* Add basic logging/exception handler.

---

### Ticket S1-003

**Title:** DB connection configuration from desktop settings
**Issue Type:** Story
**Priority:** Highest
**Story Points:** 2
**Description:** As an Admin I want to configure the MongoDB connection from the desktop app settings so that the client can connect to the Compass-backed database for dev and testing.
**Acceptance Criteria:**

* Settings allow entering MongoDB URI, DB name and test connection.
* Successful/failed connection feedback displayed to user.
* Settings persisted locally (encrypted where appropriate).
  **Subtasks:**
* Add settings UI fields.
* Implement connection tester using driver.
* Store settings securely.

---

### Ticket S1-004

**Title:** User authentication and role-based access (local auth)
**Issue Type:** Story
**Priority:** Highest
**Story Points:** 4
**Description:** As a User I want to log in with a username/password and have role-based access (Admin, Professor, TA, Student, Parent, Staff) so that only authorized users can see and perform actions for their role.
**Acceptance Criteria:**

* Users can log in and out.
* Roles are enforced in UI (menu options hide/show).
* Passwords stored hashed (bcrypt) in `users` collection.
* Seed admin account exists for first login.
  **Subtasks:**
* Implement `users` collection schema and seed.
* Implement bcrypt password hashing and verification.
* Implement role-based UI gating.

---

### Ticket S1-005

**Title:** Admin UI to define dynamic attributes (attribute metadata)
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As an Admin I want to define dynamic attributes (attribute metadata) for entity types (student, staff, course, room) so that we can add new fields later without breaking existing data.
**Acceptance Criteria:**

* Admin can create/update attribute metadata: key, dataType, allowedValues, required, version, index flag.
* Metadata stored in `attributes` collection.
* Validation on allowed fields and types.
  **Subtasks:**
* Create attribute metadata UI form.
* Implement backend persistence to `attributes`.
* Add client-side validation.

---

### Ticket S1-006

**Title:** Hybrid EAV storage & retrieval for entities (entities collection)
**Issue Type:** Story
**Priority:** Highest
**Story Points:** 6
**Description:** As a Developer/Admin I want to store and retrieve entities (students, staff, courses, rooms) using a hybrid core+attributes EAV model so that stable core fields are fast and new custom attributes are supported.
**Acceptance Criteria:**

* `entities` collection created with core + attributes array.
* Insert/read/update flows implemented for hybrid EAV docs.
* Example student/staff/course/room documents available in DB.
  **Subtasks:**
* Create JSON Schema validator for `entities`.
* Implement DAO methods for read/write.
* Seed sample entities.

---

### Ticket S1-007

**Title:** Attribute metadata versioning and validation rules
**Issue Type:** Story
**Priority:** High
**Story Points:** 2
**Description:** As an Admin I want to add attribute metadata versioning and validation rules so that attribute changes are tracked and validated before writes.
**Acceptance Criteria:**

* `attributes.version` field enforced.
* Validation metadata (regex/allowedValues) enforced on writes at app layer.
* Attempt to write invalid attribute fails with user-friendly message.
  **Subtasks:**
* Add `version` to attribute model.
* Implement validation logic prior to writes.
* Add error handling/UI messages.

---

### Ticket S1-008

**Title:** Student core profile CRUD (basic record management)
**Issue Type:** Story
**Priority:** High
**Story Points:** 5
**Description:** As a Student/Registrar/Admin I want to create and edit student basic profiles (core fields: ID, name, email, enrollment date) so that student records are digitized and retrievable.
**Acceptance Criteria:**

* Create/Read/Update/Delete student profiles implemented.
* Core fields validated and persisted in `core`.
* Profile view displays both core + dynamic attributes.
  **Subtasks:**
* Implement student form UI.
* Implement backend CRUD operations.
* Add tests for core validations.

---

### Ticket S1-009

**Title:** Course catalog CRUD (core + elective flags)
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 5
**Description:** As a Professor/Dept Admin I want to create and view a course catalog with core/elective flags and prerequisites so that students and staff can see available courses and their type.
**Acceptance Criteria:**

* Admin can create/edit courses with credits, core/elective flag, prerequisites list.
* Students can view course catalog (read-only).
* Course documents stored in `entities` or dedicated `courses` collection.
  **Subtasks:**
* Implement course data model and schema.
* Build UI for course creation and catalog view.
* Implement search/filtering.

---

### Ticket S1-010

**Title:** Room records CRUD (capacity & resources)
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 3
**Description:** As a Facilities Manager I want to create room records (capacity, resources) and view room list so that people can find appropriate rooms for classes/events.
**Acceptance Criteria:**

* CRUD for rooms implemented; required fields: roomId, capacity, resources array.
* Rooms visible in a list with filters (capacity/resource).
  **Subtasks:**
* Room entity UI.
* Backend CRUD.
* Basic list & filtering.

---

### Ticket S1-011

**Title:** Room reservation creation flow (MVP)
**Issue Type:** Story
**Priority:** High
**Story Points:** 6
**Description:** As a Staff/Professor I want to create a room reservation (select room, start/end time, purpose) so that I can reserve a space for a class or event.
**Acceptance Criteria:**

* Reservation form lets user choose room, start and end, purpose.
* Reservation saved to `reservations` collection with createdBy and status.
* Start < end and minimal time validation enforced.
  **Subtasks:**
* Reservation UI form.
* Backend insert logic with validation.
* Add createdBy and timestamps.

---

### Ticket S1-012

**Title:** Reservation conflict detection to prevent double-booking
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As a Facilities Manager I want to prevent double-bookings by detecting conflicts at reservation time so that two events don't occupy the same room/time.
**Acceptance Criteria:**

* Conflict check query implemented and runs before insert.
* Conflicting reservation blocks insert and returns conflict details to user.
* Unit tests for conflict scenarios included.
  **Subtasks:**
* Implement overlap query.
* Connect query to reservation save flow.
* Add tests for conflicts.

---

### Ticket S1-013

**Title:** Staff directory with contact info and office hours
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 4
**Description:** As a Professor/TA I want to view and manage a staff directory with contact info and office hours so that students and colleagues can contact me appropriately.
**Acceptance Criteria:**

* Staff list and profile pages exist with contact, role, office hours.
* Admin can assign staff to courses.
* Profile shows contact and assigned courses.
  **Subtasks:**
* Staff entity model.
* Directory UI.
* Assign-to-course flow.

---

### Ticket S1-014

**Title:** Student-to-staff messaging (basic threads)
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 5
**Description:** As a Student I want to send messages to assigned professors/TAs so that I can ask academic questions and schedule help.
**Acceptance Criteria:**

* Ability to create message thread and post messages.
* Messages store sender, recipient, timestamp, read/unread status.
* Students only allowed to message staff assigned to their courses in MVP.
  **Subtasks:**
* `messages` collection schema.
* Messaging UI (thread view + composer).
* Permission checks.

---

### Ticket S1-015

**Title:** Create DB indexes, JSON schema validators, and seed sample data in Compass
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As a Developer I want to create initial DB indexes, JSON schema validators, and seed sample data in MongoDB Compass so that developers can work against consistent test data and constraints.
**Acceptance Criteria:**

* JSON schema validators created for `entities`, `attributes`, `reservations`, `users`.
* Indexes applied as documented.
* Sample data for core entities inserted.
  **Subtasks:**
* Create validators in Compass.
* Create indexes.
* Insert seed data.

---

### Ticket S1-016

**Title:** Unit & integration tests and QA plan for Sprint 1 features
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As QA I want to run unit & integration tests (CI) for Sprint 1 features so that we reduce regressions and ensure basic quality.
**Acceptance Criteria:**

* Unit tests added covering service/business logic.
* Integration tests for DB read/writes run in CI.
* QA acceptance checklist created and executed for Sprint 1 features.
  **Subtasks:**
* Add unit tests for core modules.
* Add integration tests using TestContainers or local test DB.
* Complete QA checklist & report.

---

# Sprint 2 — Advanced features & polish

### Ticket S2-001

**Title:** Assignments & exams creation, submission and grading flow
**Issue Type:** Story
**Priority:** High
**Story Points:** 6
**Description:** As a Professor I want to create assignments and exams, accept student submissions, and enter grades so that I can assess student performance and communicate results.
**Acceptance Criteria:**

* Professors can create assignments/exams with due dates and instructions.
* Students can upload submissions (files or text).
* Professors can grade submissions and provide feedback.
* Grade recorded and visible in student transcript view.
  **Subtasks:**
* Assignment entity & schema.
* Submission storage (GridFS or attachments).
* Grading UI & persistence.

---

### Ticket S2-002

**Title:** Student grade and feedback view
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As a Student I want to view my grades, feedback, and assignment history so that I know how I am progressing academically.
**Acceptance Criteria:**

* Student can see list of assignments, grades, feedback per course.
* Notifications or indicators for new grades.
  **Subtasks:**
* Student-grade UI.
* Endpoint to fetch grades & feedback.
* Notification hook.

---

### Ticket S2-003

**Title:** CSV export/import & LMS connector placeholder
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 4
**Description:** As a System Integrator/Admin I want to export/import course rosters and assignments (CSV) and provide an LMS connector placeholder so that future LMS integration is easier and data can be shared.
**Acceptance Criteria:**

* Export roster and assignment CSV available.
* Import acceptance for roster CSV at least for test data.
* Placeholder adapter class documented for future LMS endpoints.
  **Subtasks:**
* Implement CSV export for rosters/assignments.
* Implement CSV import with validation.
* Add placeholder LMS connector module and docs.

---

### Ticket S2-004

**Title:** Resource (equipment & license) tracking and allocation
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 6
**Description:** As a User I want to track equipment and resource allocations (laptops, licenses) and assign/unassign them so that departments and staff know who is responsible for resources.
**Acceptance Criteria:**

* Inventory item CRUD exists with status and owner.
* Allocation/unallocation workflow implemented.
* Audit log entries on changes.
  **Subtasks:**
* Inventory collection & schema.
* Allocation UI.
* Audit logging on allocation changes.

---

### Ticket S2-005

**Title:** Research publications management on faculty profiles
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 5
**Description:** As a Professor/Researcher I want to add and publish research publications on my profile so that faculty activity and output are visible and searchable.
**Acceptance Criteria:**

* Faculty can add publications with authors, year, DOI/link, abstract.
* Publications searchable by author, year, keyword.
* Publications appear on faculty profile page.
  **Subtasks:**
* Publications schema.
* Profile integration UI.
* Search endpoint.

---

### Ticket S2-006

**Title:** Payroll stub view & leave request workflow (MVP)
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 5
**Description:** As HR/Staff I want to view payroll stub information and submit leave requests so that staff can manage HR-related needs and admins can process payroll exports.
**Acceptance Criteria:**

* Staff can view sample payroll fields (gross, net, deductions).
* Staff can submit leave requests that go to admin for approval.
* Admin can export payroll CSV for processing.
  **Subtasks:**
* Payroll fields model & UI.
* Leave request CRUD and approval flow.
* Payroll CSV export.

---

### Ticket S2-007

**Title:** Parent account creation & secure child linking (parent portal MVP)
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 4
**Description:** As a Parent I want to create a parent account linked to my child and securely view the child's basic grades and announcements so that I can stay informed about my child's academic progress.
**Acceptance Criteria:**

* Parent signup flow and linking to student records (via code or admin approval).
* Parents can view student's basic grades and announcements only.
* Security checks ensure parent cannot view other students.
  **Subtasks:**
* Parent entity & linking UI.
* Authorization checks.
* Parent dashboard.

---

### Ticket S2-008

**Title:** Announcements & events hub with RSVP
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 3
**Description:** As a Student/Staff I want to see university-wide announcements and events with RSVP capability so that I don't miss important dates and can register for events.
**Acceptance Criteria:**

* Admin can post announcements and events.
* Users see announcements on dashboard.
* RSVP persists and admin sees counts.
  **Subtasks:**
* Announcement/event schema.
* Event RSVP UI.
* Dashboard display.

---

### Ticket S2-009

**Title:** Reporting (room utilization, enrollment, staff load) and CSV export
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 4
**Description:** As an Admin I want to generate reports (room utilization, course enrollment, staff load) and export CSVs so that leadership can make data-driven decisions.
**Acceptance Criteria:**

* At least three reports implemented and exportable.
* Reports compute correct aggregates using recent data.
* UI to select report parameters and export.
  **Subtasks:**
* Implement report queries/aggregations.
* Build UI for report selection.
* CSV export feature.

---

### Ticket S2-010

**Title:** Desktop app packaging and installer documentation
**Issue Type:** Story
**Priority:** High
**Story Points:** 4
**Description:** As a Developer/DevOps I want to package the desktop app (installer or cross-platform jar) and document install steps so that end users can install and run the application on supported OSes.
**Acceptance Criteria:**

* Build packaging script (jpackage or platform-specific).
* Installer or runnable package produced and tested on at least one OS.
* Install and run instructions documented in README.
  **Subtasks:**
* Configure jpackage build.
* Produce installer and smoke-test.
* Add install docs.

---

### Ticket S2-011

**Title:** Audit logs for critical operations and soft-delete support
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As an Admin/Developer I want to implement audit logs for critical operations and support soft-deletes with rollback guidance so that we have change traceability and safer data removal.
**Acceptance Criteria:**

* `audit_logs` collection created and operations logged: create/update/delete for key collections.
* Soft-delete implemented with `deleted`, `deletedAt`, `deletedBy`.
* Admin UI to view audit logs for a document.
  **Subtasks:**
* Implement audit logging logic.
* Add soft-delete behavior to DAOs.
* Admin audit UI.

---

### Ticket S2-012

**Title:** Migration scripts versioning & migration runner
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As an Admin/DBA I want to run and version migration scripts for attribute changes and schema updates so that data transformations are repeatable and reversible.
**Acceptance Criteria:**

* `/db/migrations` folder with versioned migration files.
* Migration runner script to apply migrations and store applied versions.
* Rollback instructions included for critical migrations.
  **Subtasks:**
* Implement migration runner scaffolding.
* Add example migration file.
* Document migration process.

---

### Ticket S2-013

**Title:** File attachments support using GridFS or equivalent
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 3
**Description:** As a Developer I want to store file attachments (assignment files, documents) using GridFS or equivalent so that users can upload and retrieve large files reliably.
**Acceptance Criteria:**

* Files can be uploaded and associated with assignments/messages.
* Files retrievable by authorized users.
* Tests for upload/download flows.
  **Subtasks:**
* Integrate GridFS storage.
* Attachment UI for upload/download.
* Authorization checks.

---

### Ticket S2-014

**Title:** Deprecation workflow for attributes and data migration triggers
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 2
**Description:** As an Admin I want to mark attributes as deprecated and trigger data migration workflows so that we can evolve the data model without breaking clients.
**Acceptance Criteria:**

* `attributes.deprecated` field supported and used by admin UI.
* When attribute deprecated, migration ticket template auto-generated.
* UI shows deprecation warnings when attribute is used.
  **Subtasks:**
* Add deprecated flag to attribute metadata.
* Show warnings in attribute editor.
* Add migration ticket template.

---

### Ticket S2-015

**Title:** Scheduled backups & restore documentation
**Issue Type:** Story
**Priority:** High
**Story Points:** 2
**Description:** As a System I want to perform scheduled backups and document restore steps so that we can recover from failures and data loss.
**Acceptance Criteria:**

* Backup command documented (`mongodump` example).
* Restore steps documented and tested.
* Schedule recommendation added to ops docs.
  **Subtasks:**
* Add `/db/backup-restore.md`.
* Test backup & restore on sample DB.
* Add recommended backup schedule.

---

### Ticket S2-016

**Title:** Final QA acceptance and user guides
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As a Product Owner I want to review and accept final QA test results and user guides so that we can release a production-ready MVP to stakeholders.
**Acceptance Criteria:**

* User guide and admin guide completed.
* QA acceptance tests passed and signed off.
* Release notes drafted.
  **Subtasks:**
* Complete user & admin guides.
* Run final QA checklist.
* Draft release notes.

---

### Ticket S2-017

**Title:** Performance indexing & selective denormalization plan
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 3
**Description:** As a Developer I want to add performance-oriented indexes and denormalize frequently queried attributes when necessary so that the app meets performance goals for common queries.
**Acceptance Criteria:**

* Index list reviewed and additional indexes created where necessary.
* At least one attribute denormalized into `core` for performance.
* Performance test results documented.
  **Subtasks:**
* Profile common queries.
* Create indexes and denormalize selected attribute.
* Run performance smoke tests.

---

### Ticket S2-018

**Title:** Feature flags to toggle incomplete features off
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 2
**Description:** As an Admin I want to toggle incomplete features off (feature flags) so that we can deploy incremental work safely and enable features later without additional deployments.
**Acceptance Criteria:**

* Simple feature flag mechanism implemented (config file or DB toggles).
* Flags respected by UI to show/hide features.
* Admin UI to toggle flags for the environment.
  **Subtasks:**
* Implement feature flag config store.
* Add admin UI to toggle flags.
* Use flags in one example feature.

---

### Ticket S2-019

**Title:** Change request workflow documented in Jira for post-release changes
**Issue Type:** Story
**Priority:** High
**Story Points:** 2
**Description:** As a Product Owner I want a documented change request workflow in Jira for post-release changes so that requested changes are analyzed, prioritized and scheduled properly.
**Acceptance Criteria:**

* Change request template added to Jira.
* Workflow steps documented (impact analysis, estimate, approve).
* At least one example change request created.
  **Subtasks:**
* Create Jira change request issue template.
* Document workflow in repo.
* Create example change request.

---

# Change-Anticipation user stories (cross-cutting)

### Ticket CA-001

**Title:** Admin UI to add attribute metadata & backfill defaults via migration
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As an Admin/Dev I want to define new attribute metadata in the admin UI and backfill default values via migration scripts so that new business requirements can be added without downtime.
**Acceptance Criteria:**

* Admin can create new attribute metadata entries.
* Migration script template created to backfill default values.
* Backfill script documented and tested on sample data.
  **Subtasks:**
* Add create attribute metadata UI.
* Add migration template file and example.
* Test backfill on dev DB.

---

### Ticket CA-002

**Title:** Attribute metadata version bump & automated migration support
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As a Developer I want to increment attribute metadata versions and run automated migration scripts so that changes in attribute type/validation are tracked and data is migrated safely.
**Acceptance Criteria:**

* `attributes.version` incremented on metadata change.
* Migration script created to update existing attribute values.
* Migration runner logs applied migrations.
  **Subtasks:**
* Implement migration runner hooks.
* Add example migration that changes data type.
* Update attribute metadata UI to show version.

---

### Ticket CA-003

**Title:** Reserve change buffer in each sprint policy & backlog item
**Issue Type:** Story
**Priority:** High
**Story Points:** 1
**Description:** As a Product Owner I want to reserve a change buffer in each sprint so that urgent changes or required spikes can be handled without derailing planned work.
**Acceptance Criteria:**

* Sprint plan contains a reserved buffer ticket (10-15% capacity).
* Team agrees on buffer policy and it’s documented.
  **Subtasks:**
* Add buffer ticket to each sprint.
* Document buffer policy.

---

### Ticket CA-004

**Title:** Index-on-demand: mark attribute as indexed and create DB index
**Issue Type:** Story
**Priority:** Medium
**Story Points:** 2
**Description:** As a Dev I want to mark attributes as indexed in metadata and create corresponding DB indexes so that new query requirements can be supported with planned indexing.
**Acceptance Criteria:**

* Admin can mark `attributes.index = true`.
* Index creation script runs and index appears in DB.
* Index creation documented with rollback notes.
  **Subtasks:**
* Add index flag to attribute metadata UI.
* Implement index creation script using attribute metadata.
* Document index creation & rollback.

---

### Ticket CA-005

**Title:** Use attribute metadata as source of truth for validation rules
**Issue Type:** Story
**Priority:** High
**Story Points:** 3
**Description:** As a Developer I want to use the attribute metadata as the source of truth for validation rules so that clients and server apply consistent checks before writes.
**Acceptance Criteria:**

* App fetches attribute metadata prior to writing dynamic attributes.
* Validation enforced on client and server layers.
* Tests demonstrating enforcement exist.
  **Subtasks:**
* Implement metadata fetch & cache.
* Implement unified validation module.
* Add tests for metadata-driven validation.
---
