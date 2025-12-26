# AGILE SIS - Administrator Guide

## Table of Contents
1. [System Overview](#system-overview)
2. [Technology Stack](#technology-stack)
3. [Database Setup](#database-setup)
4. [Collection Schema](#collection-schema)
5. [Initial Data Migration](#initial-data-migration)
6. [User Account Management](#user-account-management)
7. [Backup & Recovery](#backup--recovery)
8. [Troubleshooting](#troubleshooting)

---

## System Overview

AGILE SIS is a Student Information System built with:
- **Frontend:** JavaFX (Desktop Application)
- **Backend:** Java 21
- **Database:** MongoDB Atlas (Cloud)
- **Build Tool:** Maven

---

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 21+ |
| UI Framework | JavaFX | 21 |
| Database | MongoDB Atlas | 6.0+ |
| Build Tool | Maven | 3.8+ |
| Driver | MongoDB Java Driver | 4.11+ |

---

## Database Setup

### MongoDB Atlas Configuration

1. **Create Atlas Account**
   - Go to [MongoDB Atlas](https://www.mongodb.com/atlas)
   - Create a free cluster

2. **Create Database User**
   ```
   Username: your_db_user
   Password: your_secure_password
   Role: readWriteAnyDatabase
   ```

3. **Network Access**
   - Add your IP address to whitelist
   - Or use `0.0.0.0/0` for development (not recommended for production)

4. **Get Connection String**
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/agile_sis?retryWrites=true&w=majority
   ```

5. **Configure Application**
   - Update connection string in `DBConnection.java`:
   ```java
   private static final String CONNECTION_STRING = "mongodb+srv://...";
   private static final String DATABASE_NAME = "agile_sis";
   ```

---

## Collection Schema

### users
```json
{
  "_id": ObjectId,
  "username": "John Doe",
  "passwordHash": "hashed_password",
  "roles": ["Professor", "Staff"],
  "linkedEntityId": "S1001",
  "createdAt": Date
}
```

### staff
```json
{
  "_id": ObjectId,
  "staffId": "S1001",
  "name": "Dr. John Doe",
  "email": "john@university.edu",
  "officeHours": "Mon-Wed 10-12",
  "role": "Professor",
  "createdAt": Date
}
```

### students / entities
```json
{
  "_id": ObjectId,
  "entityId": "STU001",
  "type": "Student",
  "core": {
    "name": "Jane Smith",
    "email": "jane@student.edu"
  },
  "createdAt": Date
}
```

### inventory
```json
{
  "_id": ObjectId,
  "name": "Dell Laptop #1",
  "itemType": "Laptop|License|Equipment",
  "status": "Available|Assigned|Under Repair",
  "assignedToUserId": "STU001",
  "assignedToName": "Jane Smith",
  "assignedDate": Date,
  "assignedUsers": [  // For License (multi-user)
    {"userId": "S1001", "userName": "John", "assignedDate": Date}
  ],
  "purchaseDate": Date,
  "notes": "Description",
  "createdAt": Date
}
```

### inventory_requests
```json
{
  "_id": ObjectId,
  "itemId": "ObjectId_string",
  "itemName": "Dell Laptop #1",
  "itemType": "Laptop",
  "requesterId": "STU001",
  "requesterName": "Jane Smith",
  "requesterType": "Student",
  "requestDate": Date,
  "status": "Pending|Approved|Rejected",
  "notes": "Need for coursework",
  "reviewedBy": "admin_id",
  "reviewerName": "Admin User",
  "reviewDate": Date,
  "reviewNotes": "Approved for semester"
}
```

### publications
```json
{
  "_id": ObjectId,
  "authorId": "S1001",
  "authorName": "Dr. John Doe",
  "title": "Machine Learning in Healthcare",
  "publicationType": "Journal Article|Conference Paper|Book Chapter|Thesis",
  "venue": "IEEE Transactions",
  "publicationDate": Date,
  "abstractText": "Paper abstract...",
  "doi": "10.1109/xxx",
  "url": "https://...",
  "keywords": ["ML", "healthcare"],
  "coAuthors": ["Jane Smith"],
  "published": true,
  "createdAt": Date
}
```

### audit_logs
```json
{
  "_id": ObjectId,
  "action": "ALLOCATE|DEALLOCATE|CREATE|DELETE|REQUEST_SUBMIT|REQUEST_APPROVE",
  "itemId": "ObjectId_string",
  "itemName": "Dell Laptop #1",
  "performedBy": "admin_user",
  "targetUser": "Jane Smith",
  "details": "Additional info",
  "timestamp": Date
}
```

### Other Collections
- `courses` - Course information
- `assignments` - Student assignments
- `quizzes` - Quiz data
- `messages` - User messages
- `announcements` - System announcements
- `reservations` - Room bookings
- `payroll` - Staff payroll
- `leave_requests` - Staff leave
- `benefits` - Employee benefits

---

## Initial Data Migration

### Step 1: Create Admin User
Run this in MongoDB shell or Compass:

```javascript
db.users.insertOne({
  username: "admin",
  passwordHash: "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", // "admin" hashed
  roles: ["Admin"],
  linkedEntityId: null,
  createdAt: new Date()
});
```

### Step 2: Create Sample Staff
```javascript
db.staff.insertOne({
  staffId: "PROF001",
  name: "Dr. Ahmed Hassan",
  email: "ahmed@university.edu",
  officeHours: "Sun-Tue 10:00-12:00",
  role: "Professor",
  createdAt: new Date()
});

db.users.insertOne({
  username: "Dr. Ahmed Hassan",
  passwordHash: "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4", // "1234"
  roles: ["Professor"],
  linkedEntityId: "PROF001",
  createdAt: new Date()
});
```

### Step 3: Create Sample Student
```javascript
db.entities.insertOne({
  entityId: "STU001",
  type: "Student",
  core: {
    name: "Sara Mohamed",
    email: "sara@student.edu"
  },
  createdAt: new Date()
});

db.users.insertOne({
  username: "Sara Mohamed",
  passwordHash: "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4",
  roles: ["Student"],
  linkedEntityId: "STU001",
  createdAt: new Date()
});
```

### Step 4: Create Sample Inventory
```javascript
db.inventory.insertMany([
  {
    name: "Dell Laptop #1",
    itemType: "Laptop",
    status: "Available",
    notes: "Dell Latitude 5520, 16GB RAM",
    createdAt: new Date()
  },
  {
    name: "Microsoft Office 365 (50 seats)",
    itemType: "License",
    status: "Available",
    assignedUsers: [],
    notes: "Enterprise license",
    createdAt: new Date()
  },
  {
    name: "Projector - Room 101",
    itemType: "Equipment",
    status: "Available",
    notes: "Epson PowerLite",
    createdAt: new Date()
  }
]);
```

---

## User Account Management

### Creating Users (Programmatic)
Staff and student accounts are created automatically when adding records through the Admin UI:
1. Go to **Manage Staff** or **Manage Students**
2. Click **Add**
3. Fill in details including initial password
4. System creates both the record AND linked user account

### Password Format
Passwords are stored as SHA-256 hashes. Default password is `1234`.

### Role Assignment
Valid roles:
- `Admin` - Full system access
- `Professor` - Faculty access
- `TA` - Teaching assistant
- `Staff` - General staff
- `Student` - Student access
- `Parent` - Parent portal access

---

## Backup & Recovery

### Manual Backup via Atlas
1. Go to MongoDB Atlas dashboard
2. Select your cluster
3. Click **Backup** tab
4. Create snapshot or enable continuous backup

### Export via mongodump
```bash
mongodump --uri="mongodb+srv://user:pass@cluster.mongodb.net/agile_sis" --out=./backup
```

### Restore via mongorestore
```bash
mongorestore --uri="mongodb+srv://user:pass@cluster.mongodb.net/agile_sis" ./backup/agile_sis
```

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| "SLF4J not found" warning | This is a logging warning, can be ignored or add SLF4J dependency |
| Connection timeout | Check MongoDB Atlas IP whitelist and network |
| Login fails | Verify username/password, check `users` collection |
| No data shown | Check if user has correct role permissions |

### Logs Location
Application logs are output to console. For production, configure a logging framework.

### Health Check
Verify database connection:
```java
MongoClient client = DBConnection.getInstance().getClient();
Document ping = client.getDatabase("admin").runCommand(new Document("ping", 1));
// Returns {"ok": 1} if connected
```

---

## Support Contacts

- **Technical Issues:** Contact system administrator
- **Database Issues:** Check MongoDB Atlas status page
- **Application Bugs:** Report via project issue tracker

---

*AGILE SIS Administrator Guide v1.0*
