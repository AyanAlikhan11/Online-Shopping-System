# Online Shopping MVC Application — Project Presentation & Viva Guide

---

## 1. Executive Summary & Project Introduction

### Project Title
**Online Shopping Management System (Spring Boot MVC Architecture with MySQL)**

### Objective
To build a secure, full-stack, enterprise-grade e-commerce web platform developed using the **Model-View-Controller (MVC)** architectural paradigm in Java. The application enables customers to browse catalog items, manage shopping carts, add favorites, place orders with simulated or Razorpay payments, while providing an administrative dashboard for inventory, sales, customer management, and order fulfillment.

---

## 2. Database Architecture (MySQL)

### Which Database is Used?

Your project uses **MySQL Database**:
* **Database Engine:** MySQL Server (RDBMS)
* **Default Database Name:** `shopdb`
* **Driver:** `com.mysql.cj.jdbc.Driver` (`mysql-connector-j`)
* **JDBC URL:**
  ```properties
  spring.datasource.url=jdbc:mysql://localhost:3306/shopdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
  spring.datasource.username=root
  spring.datasource.password=root
  ```
* **Auto Schema Generation & Demo Data Seeding:**
  * `spring.jpa.hibernate.ddl-auto=update`: Hibernate automatically inspects the entity models (`Product`, `Customer`, `Purchase`, etc.) and creates or updates all MySQL tables and relations upon startup.
  * `createDatabaseIfNotExist=true`: If the `shopdb` database doesn't exist yet in MySQL, JDBC creates it automatically.
  * **Demo Data Integrity:** [`SeedData.java`](file:///c:/Development/Java%20Micro%20Project/online-shoping-mvc/online-shopping-mvc/src/main/java/com/ayan/shop/config/SeedData.java) runs automatically at startup. If the database tables are empty, it automatically populates the default administrator (`admin@shop.local` / `ChangeMe123!`) and demo products (Wireless Headphones, Classic Sneakers, Minimal Watch) with stock, pricing, and images.

---

## 3. Strict MVC Architecture & Package Structure

The codebase is organized into dedicated packages strictly following **Model-View-Controller (MVC)**:

```
src/main/java/com/ayan/shop/
│
├── ShopApplication.java            # Main Spring Boot Entry Point
│
├── model/                          # [MODEL LAYER] - Domain Entities & Database Tables
│   ├── Product.java                # Catalog items, pricing, inventory stock
│   ├── Customer.java               # Customer profiles, roles (CUSTOMER/ADMIN)
│   ├── Purchase.java               # Orders, totals, addresses, payment status
│   ├── PurchaseItem.java           # Order line items and quantities
│   ├── Favorite.java               # Customer wishlist mapping
│   └── OrderStatusHistory.java     # Audit history for order status changes
│
├── repository/                     # [DATA ACCESS LAYER] - Spring Data JPA Repositories
│   ├── Products.java               # Product queries & pessimistic stock locking
│   ├── Customers.java              # Customer queries by email
│   ├── Purchases.java              # Order history & status queries
│   ├── Favorites.java              # Wishlist management queries
│   └── OrderStatusHistoryRepository.java # Order tracking history queries
│
├── service/                        # [BUSINESS LOGIC LAYER]
│   ├── ShopService.java            # Stock verification, checkout, asynchronous emails
│   ├── PaymentService.java         # Dual payment modes (Demo simulation & Razorpay)
│   └── FileStorageService.java     # Image upload validation and file storage
│
├── controller/                     # [CONTROLLER LAYER] - Web Request Handlers
│   ├── ShopController.java         # Customer storefront routes, cart, checkout, login
│   ├── AdminController.java        # Admin dashboard, inventory & order statuses
│   ├── GlobalView.java             # Shared UI attributes (@ControllerAdvice)
│   └── GlobalExceptionHandler.java # Centralized exception & error view handling
│
└── config/                         # [CONFIGURATION LAYER]
    ├── SecurityConfig.java         # Spring Security 6 & BCrypt password hashing
    ├── WebConfig.java              # Uploads directory static resource routing
    ├── AsyncConfig.java            # Thread pool executor for async email tasks
    ├── SeedData.java               # Initial demo products and admin user seeder
    └── ProductionConfig.java       # Production environment profile validation
```

---

## 4. View Layer (Thymeleaf Templates)

Dynamic HTML views populated by the Controllers are located in `src/main/resources/templates/`:

* **Storefront:**
  * `home.html` – Catalog grid with search, category filtering, and sorting
  * `product.html` – Single product details and image galleries
  * `cart.html` – Interactive shopping cart with quantity adjustment
  * `customer-form.html` – Delivery address entry & order review
  * `pay.html` – Payment gateway checkout (Demo simulation / Razorpay)
  * `order.html` & `orders.html` – Order confirmation & history
  * `favorites.html` – Wishlist page
* **Authentication:**
  * `login.html` – Customer / Admin login form
  * `register.html` – User account registration
* **Administration:**
  * `admin.html` – Inventory overview, customer status toggles, order fulfillments
  * `product-form.html` – Create / edit product details with file upload
* **Navigation & Error Templates:**
  * `nav.html` – Dynamic navbar fragment showing live cart item counter
  * `error/400.html`, `error/403.html`, `error/404.html`, `error/500.html`

---

## 5. Architectural Flow Diagram

```mermaid
graph LR
    Browser[Client Browser] <-->|HTTP Request / Response| Controller[Controllers\n(ShopController, AdminController)]
    Controller <-->|Calls Business Logic| Service[Service Layer\n(ShopService, PaymentService)]
    Service <-->|CRUD & Queries| Repository[Repositories\n(Spring Data JPA)]
    Repository <-->|ORM Mapping| Model[Model Layer\n(Entity Classes)]
    Repository <-->|SQL / JDBC| DB[(MySQL Database\nshopdb)]
    Controller -->|Binds Data to Model Map| View[View Layer\n(Thymeleaf Templates)]
    View -->|Rendered HTML| Browser
```

---

## 6. Faculty Viva Q&A Guide

**Q1: What is the architectural pattern followed in this project?**
> **Answer:** The project follows the **Model-View-Controller (MVC)** architectural pattern using **Spring Boot MVC** and **Thymeleaf**. The *Model* layer (`com.ayan.shop.model`) represents domain data and database tables; the *View* layer (`src/main/resources/templates`) provides server-side rendered HTML; the *Controller* layer (`com.ayan.shop.controller`) processes incoming HTTP requests, invokes service logic, and binds data to views.

**Q2: Which database is used and how does it connect?**
> **Answer:** The application connects to **MySQL** via JDBC using `mysql-connector-j`. Spring Data JPA and Hibernate automatically manage the schema (`ddl-auto=update`) and connection pooling. The database connection string is parameterized in `application.properties` with fallback to `jdbc:mysql://localhost:3306/shopdb`.

**Q3: How is demo data preserved and loaded?**
> **Answer:** The [`SeedData.java`](file:///c:/Development/Java%20Micro%20Project/online-shoping-mvc/online-shopping-mvc/src/main/java/com/ayan/shop/config/SeedData.java) component implements Spring Boot's `CommandLineRunner`. During application startup, it checks whether products or the admin account exist in MySQL. If the tables are clean, it seeds the administrator user and demo items automatically without duplicating existing records.

**Q4: How does Spring Security handle authorization in this MVC setup?**
> **Answer:** In [`SecurityConfig.java`](file:///c:/Development/Java%20Micro%20Project/online-shoping-mvc/online-shopping-mvc/src/main/java/com/ayan/shop/config/SecurityConfig.java), paths under `/admin/**` require the `ROLE_ADMIN` authority, while customer routes like checkout, order history, and favorites require authenticated sessions. Passwords are encrypted using `BCryptPasswordEncoder`.

**Q5: How does concurrent stock management prevent race conditions?**
> **Answer:** In `Products.java` and `Purchases.java`, queries use pessimistic write locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`). When a customer checks out, the product row is locked at the database level during the `@Transactional` checkout method in `ShopService.java`, preventing multiple users from buying out-of-stock items simultaneously.
