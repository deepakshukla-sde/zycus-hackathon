# Zycus Hackathon - Backend

## Prerequisites
- Java 21
- Gradle 8+
- PostgreSQL running on localhost:5432

## Database Setup
```sql
CREATE DATABASE "zycus-hackathon";
CREATE USER admin WITH PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE "zycus-hackathon" TO admin;
```

## Configuration
Set your OpenAI API key (optional):
```bash
export OPENAI_API_KEY=sk-...
```

## Run
```bash
./gradlew bootRun
```

The app starts on http://localhost:8080

## Health Check
```
GET http://localhost:8080/actuator/health
```
