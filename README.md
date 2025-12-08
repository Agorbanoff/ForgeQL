# ForgeQL

ForgeQL is a lightweight, expressive, JSON-driven query engine inspired by GraphQL — but intentionally simpler, faster, and easier to integrate into backend systems.

Built with **Spring Boot**, ForgeQL exposes a single `/query` endpoint that accepts structured JSON describing what data the client wants. The backend parses the request, validates it, compiles it into SQL, executes it safely, and returns clean nested JSON.

---

## ✨ Features

- **Single endpoint architecture** (`POST /query`)
- **Custom JSON query language**
- **Dynamic SQL generation (PreparedStatement-safe)**
- **AST-based query parsing**
- **Nested relations (`include`)**
- **Filter operators:** `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `in`, `between`
- **Schema registry (entity → fields → relations)**
- **Extendable modular architecture** (sorting, pagination, aggregations, permissions, caching)

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

SigmaQL processes every request through five stages:

### **1. Validation**
The query is validated against the Schema Registry:
- entity exists  
- fields exist  
- operators are legal  
- relations are valid  

### **2. Parse → AST**
The JSON query is transformed into a structured **Abstract Syntax Tree**:

```
QueryAST
 ├── entity
 ├── fields
 ├── filter
 └── include
```

### **3. SQL Compilation**
The AST is translated into SQL using a custom SQL builder.

Example:

```sql
SELECT id, username, email 
FROM users 
WHERE age > ?;
```

### **4. Execution (Spring JDBC / JPA / Query Builder)**
Queries are executed safely using:
- JDBC PreparedStatement, **or**
- Spring Data JDBC, **or**
- NamedParameterJdbcTemplate

### **5. JSON Response Resolution**
SQL results are transformed into nested JSON matching the structure of the query.

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

# 🚀 Getting Started (Spring Boot)

### **1. Clone the repo**
```bash
git clone https://github.com/Agorbanoff/ForgeQL.git
cd ForgeQL
```

### **2. Build the project**
```bash
mvn clean install
```

### **3. Configure environment variables**
In `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ForgeQL
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver
```

### **4. Run the backend**
```bash
mvn spring-boot:run
```

---

# 🧪 Test an Example Query

```bash
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{"entity": "users", "fields": ["id"]}'
```

---

# 🧱 Project Roadmap

- [ ] Sorting (`orderBy`)
- [ ] Pagination (`limit`, `offset`)
- [ ] Field aliases
- [ ] Aggregations (`count`, `sum`, `avg`, `max`, `min`)
- [ ] Permission and role-based field visibility
- [ ] Caching of repeated queries
- [ ] Join optimizations
- [ ] Relation depth limits
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

Pull requests and ideas are welcome.  
ForgeQL is designed as an educational project demonstrating architecture-level backend design, query parsing, and dynamic SQL compilation.
