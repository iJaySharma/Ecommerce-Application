# Product Management & Order Processing System

A Spring Boot backend with role-based access control (USER / ADMIN / SUPER_ADMIN) and
inventory-validated checkout. Built with Spring Boot 3, Spring Data JPA / Hibernate,
Spring Security (JWT), and MySQL/PostgreSQL (H2 for zero-setup local runs).

> Not intended to be production-ready — built to demonstrate correctness and core backend flows per the assignment brief.

---

## 1. Tech Stack

| Layer          | Technology                                |
|-----------------|--------------------------------------------|
| Language        | Java 17                                    |
| Framework       | Spring Boot 3.3.4                          |
| Persistence     | Spring Data JPA + Hibernate                |
| Database        | MySQL / PostgreSQL (H2 in-memory by default) |
| Security        | Spring Security + JWT (jjwt 0.12)          |
| Build Tool      | Maven                                      |
| Validation      | Jakarta Bean Validation                    |

## 2. Architecture

Layered architecture, one responsibility per layer:

```
controller/    -> REST endpoints, request/response mapping only
service/       -> interfaces (business contracts)
service/impl/  -> business logic, transactions, inventory rules
repository/    -> Spring Data JPA interfaces
entity/        -> JPA entities (DB model)
dto/request/   -> validated inbound payloads
dto/response/  -> outbound payloads (never expose entities directly)
security/      -> JWT filter, token utility, UserDetails
exception/     -> custom exceptions + @RestControllerAdvice global handler
config/        -> SecurityConfig, DataSeeder
enums/         -> RoleName, OrderStatus
```

Controllers never touch repositories or entities directly — only DTOs cross the controller boundary, and only services orchestrate repositories.

## 3. Roles & Permissions

| Action                                      | USER | ADMIN | SUPER_ADMIN |
|----------------------------------------------|:----:|:-----:|:-----------:|
| View products / categories                   | ✅   | ✅    | ✅           |
| Cart (add/update/remove), Address, Checkout  | ✅   | ✅    | ✅           |
| Add/update product, price, inventory, enable/disable, assign category | ❌ | ✅ | ✅ |
| Create/update/delete categories               | ❌   | ❌    | ✅           |
| Manage users & roles                          | ❌   | ❌    | ✅           |
| View **all** orders                           | ❌   | ❌    | ✅           |

Enforced via `SecurityConfig` (`hasRole` / `hasAnyRole` matchers) — see that file for the exact matcher list.

## 4. Inventory Rules (Checkout Flow)

Implemented in `OrderServiceImpl.checkout()`:

1. Cart must not be empty (mandatory cart validation before payment) → `EmptyCartException` (400) otherwise.
2. Chosen address must belong to the authenticated user.
3. For **every** cart line, the inventory row is fetched with a **pessimistic write lock** (`SELECT ... FOR UPDATE`) and checked for sufficient stock — this runs *before* any inventory is mutated, so a failure on any single item aborts the whole checkout (all-or-nothing).
4. Insufficient stock on any item → `InsufficientInventoryException` (409), transaction rolls back, nothing is deducted.
5. On success: inventory is decremented, item prices are snapshotted onto `OrderItem.priceAtPurchase` (so later price changes don't retroactively alter past orders), the `Order` + `OrderItem`s are persisted, and the cart is cleared — all inside one `@Transactional` boundary.

The pessimistic lock protects against two users racing to buy the last unit of a product concurrently.

## 5. Database Schema

Tables (matches the assignment's required table list):

```
users            (id, username, email, password, enabled, created_at)
roles            (id, name)                         -- USER / ADMIN / SUPER_ADMIN
user_roles       (user_id, role_id)                 -- join table, many-to-many
categories       (id, name, description)
products         (id, name, description, price, enabled, category_id)
inventory        (id, product_id, quantity)          -- one-to-one with products
addresses        (id, user_id, line1, line2, city, state, zip_code, country)
cart             (id, user_id)                        -- one-to-one with users
cart_items       (id, cart_id, product_id, quantity)
orders           (id, user_id, address_id, total_amount, status, created_at)
order_items      (id, order_id, product_id, quantity, price_at_purchase)
```

Relationships:
- `User 1---1 Cart 1---* CartItem *---1 Product`
- `User 1---* Address`
- `User 1---* Order 1---* OrderItem *---1 Product`
- `Product 1---1 Inventory`
- `Product *---1 Category`
- `User *---* Role` (via `user_roles`)

`ddl-auto=update` lets Hibernate create/update this schema automatically on startup — no manual DDL needed for local runs.

## 6. Setup & Run

### Prerequisites
- JDK 17+
- Maven 3.8+
- (Optional) MySQL 8 or PostgreSQL 14+ if you don't want to use the default H2 in-memory DB

### Quick start (H2, zero config)
```bash
git clone <your-repo-url>
cd pms-backend
mvn spring-boot:run
```
The app starts on `http://localhost:8080`. H2 console (if needed for inspection) is at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:pmsdb`, user `sa`, no password).

### Switching to MySQL or PostgreSQL
Edit `src/main/resources/application.properties`:
- Comment out the H2 block.
- Uncomment the MySQL or PostgreSQL block and adjust credentials/DB name.

Example (MySQL):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pmsdb?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=root
```

### Default seeded account
On first startup, `DataSeeder` creates the three roles and one default SUPER_ADMIN:
```
username: superadmin
password: Admin@123
```
Use this to log in and start creating categories, products, and other admins.

### Build a jar
```bash
mvn clean package
java -jar target/product-order-management-1.0.0.jar
```

## 7. API Reference

Base URL: `http://localhost:8080`
Authenticated requests need header: `Authorization: Bearer <token>`

### Auth (public)
| Method | Endpoint             | Description         |
|--------|-----------------------|----------------------|
| POST   | `/api/auth/register`  | Register a new USER  |
| POST   | `/api/auth/login`     | Login, returns JWT   |

### Categories
| Method | Endpoint                  | Role         |
|--------|----------------------------|--------------|
| GET    | `/api/categories`          | Public       |
| GET    | `/api/categories/{id}`     | Public       |
| POST   | `/api/categories`          | SUPER_ADMIN  |
| PUT    | `/api/categories/{id}`     | SUPER_ADMIN  |
| DELETE | `/api/categories/{id}`     | SUPER_ADMIN  |

### Products
| Method | Endpoint                          | Role                |
|--------|-------------------------------------|----------------------|
| GET    | `/api/products`                     | Public               |
| GET    | `/api/products/{id}`                | Public               |
| GET    | `/api/products/category/{catId}`    | Public               |
| POST   | `/api/products`                     | ADMIN, SUPER_ADMIN   |
| PUT    | `/api/products/{id}`                | ADMIN, SUPER_ADMIN   |
| PATCH  | `/api/products/{id}/price`          | ADMIN, SUPER_ADMIN   |
| PATCH  | `/api/products/{id}/inventory`      | ADMIN, SUPER_ADMIN   |
| PATCH  | `/api/products/{id}/enable`         | ADMIN, SUPER_ADMIN   |
| PATCH  | `/api/products/{id}/disable`        | ADMIN, SUPER_ADMIN   |

### Cart (USER and above)
| Method | Endpoint                         | Description               |
|--------|------------------------------------|----------------------------|
| GET    | `/api/cart`                        | View cart                  |
| POST   | `/api/cart/items`                  | Add item to cart           |
| PUT    | `/api/cart/items/{productId}?quantity=` | Update item quantity  |
| DELETE | `/api/cart/items/{productId}`      | Remove item from cart      |
| DELETE | `/api/cart`                        | Clear cart                 |

### Addresses (USER and above)
| Method | Endpoint            | Description       |
|--------|-----------------------|--------------------|
| POST   | `/api/addresses`      | Add address        |
| GET    | `/api/addresses`      | List own addresses |

### Orders
| Method | Endpoint                | Role                | Description                         |
|--------|---------------------------|----------------------|--------------------------------------|
| POST   | `/api/orders/checkout`    | USER and above       | Place order (validates inventory)    |
| GET    | `/api/orders/my`          | USER and above       | View own orders                      |
| GET    | `/api/admin/orders`       | SUPER_ADMIN          | View all orders                      |

### User & Role Management
| Method | Endpoint                          | Role         |
|--------|--------------------------------------|--------------|
| GET    | `/api/admin/users`                   | SUPER_ADMIN  |
| PATCH  | `/api/admin/users/{id}/role`         | SUPER_ADMIN  |
| PATCH  | `/api/admin/users/{id}/enable`       | SUPER_ADMIN  |
| PATCH  | `/api/admin/users/{id}/disable`      | SUPER_ADMIN  |

## 8. Sample API Requests

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","email":"john@example.com","password":"secret123"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"Admin@123"}'
```
Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "username": "superadmin",
  "roles": ["ROLE_SUPER_ADMIN"]
}
```

### Create a category (SUPER_ADMIN)
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Electronics","description":"Gadgets and devices"}'
```

### Create a product with initial stock (ADMIN/SUPER_ADMIN)
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Wireless Mouse","description":"Ergonomic mouse","price":19.99,"categoryId":1,"quantity":50}'
```

### Add item to cart (USER)
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'
```

### Add an address (USER)
```bash
curl -X POST http://localhost:8080/api/addresses \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"line1":"221B Baker Street","city":"London","state":"London","zipCode":"NW16XE","country":"UK"}'
```

### Checkout (USER)
```bash
curl -X POST http://localhost:8080/api/orders/checkout \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"addressId":1}'
```
If inventory is insufficient for any line item, this returns `409 Conflict` and nothing is charged/deducted:
```json
{
  "timestamp": "2026-08-20T10:15:00",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient inventory for product 'Wireless Mouse'. Requested: 5, Available: 2",
  "path": "/api/orders/checkout"
}
```

## 9. Error Handling

All exceptions are caught by `GlobalExceptionHandler` and returned in a consistent shape:
```json
{
  "timestamp": "2026-08-20T10:15:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: '99'",
  "path": "/api/products/99"
}
```
Validation errors additionally include a `validationErrors` map of field → message.

| Exception                        | HTTP Status |
|-----------------------------------|-------------|
| `ResourceNotFoundException`       | 404         |
| `DuplicateResourceException`      | 409         |
| `InsufficientInventoryException`  | 409         |
| `EmptyCartException`              | 400         |
| `UnauthorizedActionException`     | 403         |
| `AccessDeniedException`           | 403         |
| `BadCredentialsException`         | 401         |
| `MethodArgumentNotValidException` | 400         |

## 10. Project Structure
```
pms-backend/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/pms/
    │   ├── PmsApplication.java
    │   ├── config/          (SecurityConfig, DataSeeder)
    │   ├── controller/
    │   ├── dto/{request,response}/
    │   ├── entity/
    │   ├── enums/
    │   ├── exception/
    │   ├── repository/
    │   ├── security/
    │   └── service/{,impl}/
    └── resources/
        └── application.properties
```
"# Ecommerce-Application" 
