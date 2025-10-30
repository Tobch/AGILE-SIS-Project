## Sprint 1 — Goal: skeleton desktop app + core modules + EAV DB foundation

### 1. Setup project repo, CI, Jira board (2 SP) — **Priority: Highest**

**Description:** Create Git repository structure, Github remote, create Jira project with sprints and issue types.
**Acceptance criteria:**

* Repo created with `main` and `develop` branches.
* Jira project created and Sprint 1 board populated with initial tickets.
  **Subtasks:**
* Create repo `AGILE SIS Project` (or chosen name).
* Add README, license, CONTRIBUTING.
* Setup GitFlow or trunk-based branching policy.
* Add CI config (GitHub Actions or Jenkins).
* Create corresponding Jira project and import backlog tickets.

---

### 2. Desktop app skeleton (JavaFX) + app settings window (5 SP) — **Priority: Highest**

**Description:** JavaFX application skeleton with login screen, main window, and settings (DB connection config).
**Acceptance criteria:**

* App launches, shows login screen and main dashboard stub.
* Settings dialog allows configuring MongoDB URI, test connection button works.
* Unit tests for config reading.
  **Subtasks:**
* Initialize JavaFX app with Maven/Gradle.
* Implement settings UI and persistence.
* Implement simple logger and exception handler.

---

### 3. Authentication & Authorization core (4 SP) — **Priority: Highest**

**Description:** Local user auth with roles: Admin, Professor, TA, Student, Parent, Staff. (If backend used, implement JWT; if direct DB, implement salted password hashing.)
**Acceptance criteria:**

* Users can sign in and sign out.
* Role-based UI gating implemented (Admin sees admin menus).
* Passwords hashed and stored securely.
  **Subtasks:**
* Auth UI + role middleware.
* Password hashing (bcrypt).
* Seed initial admin account.


---

### 4. MongoDB EAV foundation + schema validation (6 SP) — **Priority: Highest**

**Description:** Implement EAV model baseline collections, attribute metadata, JSON Schema validators for collections, and migration scripts. (Detailed DB design below.)
**Acceptance criteria:**

* Collections created: `entities`, `attributes`, `attribute_values` (or hybrid approach).
* Attribute metadata allows type/constraints; validator enforces metadata where possible.
* Example entity (student) with EAV attributes stored and retrievable.
  **Subtasks:**
* Create attribute metadata collection + UI to add attributes (admin).
* Implement server-side validation (or client-side checks).
* Write migration script to create initial attributes.

---

### 5. Facilities Module — Classroom & lab scheduling (MVP) (6 SP) — **Priority: High**

**Description:** Booking system for rooms with availability view and reservation flow.
**Acceptance criteria:**

* Rooms list viewable.
* Create reservation with time, room, owner, purpose.
* Prevent double-booking (conflict detection).
  **Subtasks:**
* Room entity + CRUD.
* Reservation logic + conflict detection.
* Calendar/day view (simple list for MVP).

---

### 6. Administrative Office Automation — Student records CRUD (MVP) (5 SP) — **Priority: High**

**Description:** Manage student basic profiles and transcript stub. Store core fields (non-EAV) + EAV allowed for extensibility.
**Acceptance criteria:**

* Create/read/update/delete student profiles.
* Transcript view lists courses + grades (initial manual entry).
  **Subtasks:**
* Student entity UI + backend storage.
* Transcript data model (embedded in student or separate collection).


---

### 7. Staff Module — Directory (4 SP) — **Priority: Medium**

**Description:** Central directory of professors & TAs with contact and office hours.
**Acceptance criteria:**

* Staff list and profile view.
* Ability to assign staff to courses (basic).
  **Subtasks:**
* Staff entity and UI.
* Assign staff to course endpoint.

---

### 8. Curriculum Module — Course catalog (MVP) (5 SP) — **Priority: Medium**

**Description:** Core + elective course definitions, search & view.
**Acceptance criteria:**

* Admin can create courses with core/elective flags, credits, prerequisites.
* Students can view course catalog.
  **Subtasks:**
* Course entity and CRUD.
* Course catalogue UI.

---

### 9. Community Module — Basic messaging (student->staff) (5 SP) — **Priority: Medium**

**Description:** Simple message thread system: students can message staff; messages stored and searchable.
**Acceptance criteria:**

* Send/receive messages; messages have timestamps and read/unread status.
* Permission checks so students message only staff assigned to course (MVP).
  **Subtasks:**
* Message entity and UI.
* Notification stub.

---

### 10. Backlog: EAV admin & attribute editor (3 SP) — **Priority: Medium**

**Description:** Admin UI to define new attributes for entity types (student, staff, course, room).
**Acceptance criteria:**

* Admin can add attribute metadata (name, type, required, validation regex, allowed values).
  **Subtasks:**
* Attribute metadata UI + validation storage.

---

### 11. Testing & QA sprint tasks (3 SP) — **Priority: High**

**Description:** Unit tests, integration tests, and QA plan for Sprint 1 features.
**Acceptance criteria:**

* Tests added.
* QA checklist completed with signoff.

---

## Sprint 2 — Goal: advanced features, integrations, packaging, Handoff

### 13. Assessment & Grading core (6 SP) — **Priority: High**

**Description:** Professors create assignments/exams, submit grades; students view grades and feedback.
**Acceptance criteria:**

* Create assignment, upload instructions, submission record, grade entry UI.
* Student view of grades and feedback.
  **Subtasks:**
* Assignment entity + CRUD.
* Submission record.
* Grading UI.

---

### 14. LMS integration hook + file attachments (4 SP) — **Priority: Medium**

**Description:** Provide an adapter to integrate with an LMS (e.g., export/import assignments). For MVP create import/export CSV and a placeholder for LMS connector.
**Acceptance criteria:**

* Export course roster and assignments CSV.
* Configurable LMS endpoint placeholder documented for future connector.
  **Subtasks:**
* Export utilities.
* Attachment storage implementation (files in GridFS).

---

### 15. Resource allocation & inventory (6 SP) — **Priority: Medium**

**Description:** Track equipment (laptops, licenses) allocation to departments/faculty/students.
**Acceptance criteria:**

* Inventory CRUD, assign/unassign resources, status tracking.
  **Subtasks:**
* Inventory entity and UI.
* Allocation rules and search.

---

### 16. Performance Tracking & Research Publications (5 SP) — **Priority: Medium**

**Description:** Faculty can add publications, track activities and training.
**Acceptance criteria:**

* Publication CRUD, searchable by author and year.
* Faculty profile shows publications list.
  **Subtasks:**
* Publications collection and UI.

---

### 17. Payroll & HR integration basics (MVP) (5 SP) — **Priority: Low-Medium**

**Description:** Add payroll info fields, leave requests and an export for payroll processing. (No actual payroll engine in MVP.)
**Acceptance criteria:**

* Staff can view payroll stub (sample fields) and submit leave requests.
* Admin can export payroll CSV containing required fields.
  **Subtasks:**
* Payroll data model.
* Leave request workflow.

---

### 18. Parent-to-Teacher portal (MVP) (4 SP) — **Priority: Medium**

**Description:** Parents can view child progress and message teachers. Authentication flow and secure view by parent account.
**Acceptance criteria:**

* Parent account creation and linking to student.
* Parent sees student basic grades and announcements.
  **Subtasks:**
* Parent entity and linking UI.
* Permissions enforcement.

---

### 19. Announcements & Events module (3 SP) — **Priority: Medium**

**Description:** University-wide announcements and event calendar.
**Acceptance criteria:**

* Admin can post announcements; users see them in dashboard.
* Event creation with date/time and RSVP (MVP).
  **Subtasks:**
* Announcement and event entities.

---

### 20. Reporting & export (4 SP) — **Priority: Medium**

**Description:** Generate basic reports (room utilization, student enrolment by course, staff load). CSV exports.
**Acceptance criteria:**

* At least three reports available and exportable to CSV.
  **Subtasks:**
* Implement report endpoints and UI.

---

### 21. Packaging, installer, and deployment notes (4 SP) — **Priority: High**

**Description:** Build desktop installers for Windows/macOS (or cross-platform jar). Include DB migration and README for install.
**Acceptance criteria:**

* Installer or packaged jar produced and tested on at least one OS.
  **Subtasks:**
* Build packaging scripts using jpackage or platform-specific tools.
* Document installer steps.

---

### 22. Final QA, documentation, training materials (3 SP) — **Priority: High**

**Description:** End-to-end testing, user guide, admin guide, and handoff materials.
**Acceptance criteria:**

* User guide completed (PDF/markdown).
* Admin guide with DB and migration steps.

---

### 23. Change request buffer & retrospective actions (2 SP) — **Priority: High**

**Description:** Capacity for late changes discovered during Sprint 2; implement at least one major change request or refactor.

---

# Non-Functional Requirements (NRF)

Include these as global backlog items that apply across sprints (create Jira epic "NRF" and add these as tasks):

1. **Security**

   * Password hashing (bcrypt); role-based authorization.
   * TLS for all DB/API connections; local configs must support TLS.
   * Sensitive data encryption at rest (MongoDB Encryption at Rest recommended).
   * Audit logs for critical operations.

2. **Performance**

   * Response time < 2s for common queries (room availability, staff directory).
   * Use indexes on frequently queried fields (entity id, attribute key).
   * Pagination on large lists.

3. **Scalability & Availability**

   * Support multi-user concurrency (use optimistic locking patterns).
   * DB with replica set recommended.
   * DB connection pooling.

4. **Maintainability**

   * Clean layered architecture (UI / Service / Repository).
   * Unit and integration tests; code style checks.

5. **Extensibility**

   * EAV model to allow adding custom attributes without schema migration.
   * Plugin points for LMS and payroll connectors.

6. **Data integrity**

   * Use MongoDB JSON Schema validation where possible.
   * App-level validation for complex invariants (e.g., no double booking).

7. **Backup & Recovery**

   * Daily DB backups; documented restore steps.
   * Schema migration scripts versioned.

8. **Localization**

   * App should support multiple languages (externalize strings).

9. **Accessibility**

   * Follow basic keyboard navigation and readable UI contrast.

---