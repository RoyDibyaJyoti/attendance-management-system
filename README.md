# AMCS — Attendance Management System

Clean start. Build incrementally, one working vertical slice at a time.

## Running locally

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL running locally on port 5432 with database `amcs_db`

### Backend
```bash
mvn spring-boot:run
```

Backend will start on `http://localhost:8080`.

### Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend will start on `http://localhost:5173`.

## Development philosophy

Every feature must work end-to-end before the next one starts:

```
Frontend → REST API → Application logic → Database → Persisted data → Frontend reload
```

No speculative architecture. No future-proof abstractions. Build what is needed now.
