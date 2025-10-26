working on sprint 1 the dev memmbers did the following tasks:


memmber Ahmed Tabbash did the basic foundation which is : 

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