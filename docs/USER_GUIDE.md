# AGILE SIS - User Guide

## Table of Contents
1. [Getting Started](#getting-started)
2. [Login & Authentication](#login--authentication)
3. [Student Features](#student-features)
4. [Staff/Professor Features](#staffprofessor-features)
5. [Admin Features](#admin-features)
6. [Parent Features](#parent-features)

---

## Getting Started

### System Requirements
- Java 21 or higher
- MongoDB Atlas connection (cloud database)
- Internet connection

### Launching the Application
1. Open the project in your IDE
2. Run `edu.agile.sis.App` as Java Application
3. The login screen will appear

---

## Login & Authentication

### Logging In
1. Enter your **Username** (usually your full name)
2. Enter your **Password**
3. Click **Login**

### User Roles
| Role | Access Level |
|------|--------------|
| Admin | Full system access |
| Professor | Course management, publications, resources |
| TA | Course assistance, assignments |
| Student | View courses, assignments, request resources |
| Parent | View child's progress, messages |

---

## Student Features

### 📚 Courses
- View enrolled courses
- See course details and schedules

### 📝 Assignments
- View assigned homework
- Submit assignments before deadlines

### ❓ Quizzes
- Take available quizzes
- View quiz results and scores

### 📦 Request Resources
- Browse available laptops and software licenses
- Submit resource requests
- View request status (Pending/Approved/Rejected)
- See assigned resources

### 📚 Faculty Research
- Browse published faculty research papers
- Search by author, year, or keyword

### 💬 Messages
- Send and receive messages with instructors

### 📢 Announcements
- View course and university announcements

---

## Staff/Professor Features

### 📚 Courses
- Create and manage courses
- Add students to courses
- Set schedules and rooms

### 📝 Assignments
- Create assignments with due dates
- Grade student submissions

### ❓ Quizzes
- Create quizzes with multiple choice questions
- View student quiz results

### 📄 My Publications
- Add research papers with metadata:
  - Title, Abstract, Publication Type
  - Venue (journal/conference)
  - DOI and URL link
  - Keywords and co-authors
- Publish/Unpublish papers
- Edit or delete publications

### 📚 Faculty Research
- Browse all faculty publications
- Search by author, type, or year

### 📦 Request Resources
- Browse available resources
- Request equipment and software

### 📅 Room Reservations
- Book classrooms and labs
- View reservation calendar

### 💵 Payroll
- View salary information

### 🏖️ Leave Requests
- Submit leave/vacation requests
- View request status

### 🎁 Benefits
- View and manage benefits

---

## Admin Features

### 🎓 Manage Students
- Add/Edit/Delete student records
- Link students to user accounts

### 👥 Manage Staff
- Add/Edit/Delete staff records
- Create linked login accounts
- View staff publications

### 👪 Manage Parents
- Register parent accounts
- Link parents to students

### 📦 Inventory Management
- **All Items Tab:**
  - Add new items (Laptop, License, Equipment)
  - Assign items to users
  - Unassign/deallocate items
  - Track item status
- **Pending Requests Tab:**
  - Review resource requests
  - Approve or reject with notes

### 💵 Payroll
- Manage staff payroll records

### 🏖️ Leave Requests
- Approve/reject staff leave requests

### 🎁 Benefits
- Manage employee benefits

### ⚙️ EAV Admin
- Manage entity-attribute-value configurations

---

## Parent Features

### 🏠 Parent Dashboard
- View child's academic progress
- See grades and attendance

### 📢 Announcements
- View school announcements

### 📨 Messages
- Communicate with teachers

---

## Tips & Shortcuts

| Action | How To |
|--------|--------|
| Refresh data | Click 🔄 Refresh button |
| View details | Double-click a table row |
| Search | Use search fields and filters |
| Logout | Click 🚪 Logout in sidebar |

---

## Getting Help

If you encounter issues:
1. Check your internet connection (MongoDB cloud)
2. Ensure you have the correct login credentials
3. Contact your system administrator

---

*AGILE SIS v1.0 - Student Information System*
