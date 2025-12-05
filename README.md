# SigmaQL

SigmaQL is a lightweight, expressive, JSON-driven query engine inspired by GraphQL — but intentionally simpler, faster, and easier to integrate into any backend.

SigmaQL exposes a single `/query` endpoint that accepts structured JSON describing what data the client wants. The backend parses the request, validates it, compiles it into SQL, executes it safely, and returns clean nested JSON.

---

## ✨ Features

- **Single endpoint architecture** (`POST /query`)
- **Custom JSON query language**
- **Dynamic SQL generation (safe prepared statements)**
- **Nested relations (`include`)**
- **Filter operators:** `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `in`, `between`
- **Schema registry for validation**
- **Fully extensible design** (sorting, pagination, aggregations, permissions, caching)

---

## 📦 Example Query

```json
{
  "entity": "users",
  "fields": ["id", "username", "email"],
  "filter": {
    "age": { "gt": 18 }
  },
  "include": {
    "posts": {
      "fields": ["title", "created_at"],
      "filter": { "likes": { "gt": 50 } }
    }
  }
}
```

---

## 📤 Example Response

```json
{
  "data": {
    "users": [
      {
        "id": 3,
        "username": "andrew",
        "email": "andrew@example.com",
        "posts": [
          {
            "title": "My First Post",
            "created_at": "2024-01-01"
          }
        ]
      }
    ]
  }
}
```

---

# 🧠 How SigmaQL Works

SigmaQL processes all queries through five stages:

### **1. Validate**
The query is checked against the internal Schema Registry:
- valid entity  
- valid fields  
- valid filters  
- valid relations  

### **2. Parse → AST**
The JSON request becomes an **Abstract Syntax Tree**:

```
QueryAST
 ├── entity
 ├── fields
 ├── filter
 └── include
```

### **3. Compile to SQL**
The AST is translated into a secure SQL query.

Example:

```sql
SELECT id, username, email
FROM users
WHERE age > $1;
```

### **4. Execute**
Prepared SQL is executed through the database driver.

### **5. Resolve → JSON**
Rows are transformed into clean nested JSON according to the query structure.

---

# 📚 Schema Registry Example

```json
{
  "users": {
    "fields": ["id", "username", "email", "age"],
    "relations": {
      "posts": {
        "type": "one-to-many",
        "target": "posts",
        "localKey": "id",
        "foreignKey": "user_id"
      }
    }
  }
}
```

---

# 🚀 Getting Started

### **1. Clone the repo**
```bash
git clone https://github.com/Agorbanoff/SigmaQL.git
cd SigmaQL
```

### **2. Install dependencies**
FastAPI example:
```bash
pip install -r requirements.txt
```

Spring Boot example:
```bash
mvn clean install
```

### **3. Configure environment**
Create `.env`:

```
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASS=password
DB_NAME=sigmaql
```

### **4. Run the server**

FastAPI:
```bash
uvicorn app.main:app --reload
```

Spring Boot:
```bash
mvn spring-boot:run
```

---

# 🧪 Test an Example Query

```bash
curl -X POST http://localhost:8000/query \
  -H "Content-Type: application/json" \
  -d '{"entity": "users", "fields": ["id"]}'
```

---

# 🧱 Roadmap

- [ ] Sorting (`orderBy`)
- [ ] Pagination (`limit`, `offset`)
- [ ] Field aliases
- [ ] Aggregations (`count`, `sum`, `avg`, `max`, `min`)
- [ ] Role-based permissions
- [ ] Cache layer for repeated queries
- [ ] Relation-depth limits
- [ ] Custom operator plugins

---

# ❗ Error Format

```json
{
  "error": {
    "message": "Unknown field: emaail",
    "code": "INVALID_FIELD",
    "path": "fields[2]"
  }
}
```

---

# 🤝 Contributing

PRs and ideas are welcome.  
This project is intended for learning backend architecture and building a custom query language.

---

# 📜 License

MIT License.
