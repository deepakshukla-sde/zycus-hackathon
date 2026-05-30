# Zycus Procurement Platform

An AI-powered procurement order management system built for the Zycus Hackathon. Admins can view agents and orders, trigger AI-based bulk order assignment, and rely on a background scheduler to auto-reassign orders when agents go offline.

---

## Features

- **JWT Authentication** — register, login, secure all APIs
- **Agent Management** — view agents with status, workload, and ratings
- **Order Management** — view all orders with real-time status
- **AI Order Assignment** — LLM assigns all pending orders to available agents intelligently based on workload and rating
- **Rule-Based Fallback** — if LLM is unavailable, orders are assigned using lowest order count + highest rating logic
- **Offline Agent Monitoring** — background scheduler detects offline agents and auto-reassigns their orders every 3 seconds

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Database | PostgreSQL |
| LLM | OpenAI gpt-4o-mini via RestClient |
| Frontend | React 18, Vite 5, Axios, React Router DOM v6 |
| Auth | JWT (HS256), BCrypt |

---

## Project Structure

```
zycus-hackathon-be/    # Spring Boot backend
zycus-hackathon-fe/    # React frontend
```

---

## Quick Start

### Prerequisites
- Java 21
- PostgreSQL 15+
- Node.js 18+

### 1. Database
```sql
CREATE DATABASE "zycus-hackathon";
CREATE USER admin WITH PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE "zycus-hackathon" TO admin;
\c "zycus-hackathon"
GRANT ALL ON SCHEMA public TO admin;
```

### 2. Environment Variables
```cmd
setx OPENAI_API_KEY "sk-your-key-here"
setx LLM_PROVIDER "gpt"
setx LLM_MODEL "gpt-4o-mini"
setx LLM_BASE_URL "https://api.openai.com/v1/chat/completions"
setx POLLING_INTERVAL "3000"
```

### 3. Backend
```bash
cd zycus-hackathon-be
./gradlew bootRun
# Starts on http://localhost:8080
```

### 4. Frontend
```bash
cd zycus-hackathon-fe
npm install
npm run dev
# Opens on http://localhost:5173
```

### 5. Seed Data
```sql
INSERT INTO zycus_hack_agents (id, name, active_order_count, status, rating) VALUES
  ('AGT-001', 'Priya Sharma',  2, 'BUSY',      4.5),
  ('AGT-002', 'Rahul Verma',   0, 'AVAILABLE', 4.8),
  ('AGT-003', 'Ananya Iyer',   1, 'BUSY',      4.2),
  ('AGT-004', 'Kiran Nair',    0, 'AVAILABLE', 4.6),
  ('AGT-005', 'Deepak Mehta',  3, 'BUSY',      3.9);

INSERT INTO zycus_hack_orders (id, description, assigned_agent_id, status, created_at) VALUES
  ('ORD-001', 'Electronics — Koramangala to Indiranagar', 'AGT-001', 'ASSIGNED', NOW()),
  ('ORD-002', 'Groceries — HSR Layout to BTM',            'AGT-001', 'ASSIGNED', NOW()),
  ('ORD-003', 'Pharma — Whitefield to Marathahalli',      'AGT-003', 'ASSIGNED', NOW()),
  ('ORD-004', 'Documents — MG Road to Jayanagar',         'AGT-005', 'ASSIGNED', NOW()),
  ('ORD-005', 'Food — Bellandur to Electronic City',      'AGT-005', 'ASSIGNED', NOW()),
  ('ORD-006', 'Apparel — Malleshwaram to Rajajinagar',    'AGT-005', 'ASSIGNED', NOW()),
  ('ORD-007', 'Books — Banashankari to JP Nagar',         'AGT-003', 'ASSIGNED', NOW()),
  ('ORD-008', 'Hardware — Peenya to Yeshwanthpur',        'AGT-001', 'ASSIGNED', NOW());
```

---

## API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register user |
| POST | `/api/auth/login` | No | Login, get JWT |
| GET | `/api/orders` | Yes | All orders |
| GET | `/api/orders/agents` | Yes | All agents |
| POST | `/api/orders/assign` | Yes | AI bulk assignment |
| GET | `/actuator/health` | No | Health check |

---

## How Assignment Works

```
Admin clicks "Assign Orders via AI"
        ↓
Fetch all PENDING orders + AVAILABLE agents
        ↓
Call OpenAI with orders + agent workload + ratings
        ↓
LLM returns JSON mapping of order → agent with reasoning
        ↓ (if LLM fails)
Rule-based fallback: lowest order count + highest rating
        ↓
Persist assignments, update agent statuses
        ↓
Show result table with per-order assignment details
```

## Offline Agent Reassignment

Every 3 seconds the scheduler checks for OFFLINE agents. If any have assigned orders, those orders are automatically reassigned to available agents using LLM (or fallback). If no agents are available, orders are reset to PENDING and retried on the next poll.

---

## Documentation

See [DEVELOPMENT.md](./DEVELOPMENT.md) for detailed development documentation.
