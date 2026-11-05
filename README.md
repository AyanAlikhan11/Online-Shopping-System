# Online Shopping MVC

A simple online shopping website built with **Spring Boot 3**, **Thymeleaf**, and **Spring Data JPA**.
Customers can browse products, add them to a cart, place orders, pay (demo or Razorpay), and track delivery.
Admins can manage products, customers, and order status from a dashboard.

---

## Features

**For customers**
- Browse and search products
- Filter by category, type, size, color, and price
- Add products to cart and update quantities
- Register, log in, and save favorite products
- Checkout with a delivery address
- Pay using demo mode or Razorpay
- View order history and delivery status

**For admins**
- Dashboard with products, customers, and orders
- Add, edit, delete products (with image upload)
- Update order delivery status and add notes
- Activate/deactivate or delete customers

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Language |
| Spring Boot 3.3.5 | Application framework |
| Spring MVC + Thymeleaf | Web pages |
| Spring Data JPA / Hibernate | Database access |
| Spring Security | Login and role-based access |
| H2 / MySQL / PostgreSQL | Database options |
| Flyway | Database migrations (prod only) |
| Razorpay | Online payments (optional) |

---

## Requirements

- Java 17 or newer
- Maven 3.8+ (or use the Maven wrapper if available)

---

## Quick Start

The project runs with **H2 file database by default**, so no database setup is required.

```bash
mvn spring-boot:run
```

Then open: **http://localhost:8080**

The database is stored in `./data/shopdb`. Sample products and an admin account are created automatically on the first run.

### Default Admin Login

| Field | Value |
|---|---|
| Email | `admin@shop.local` |
| Password | `ChangeMe123!` |

You can change these with the `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables.

---

## Build and Run

```bash
# Build the project
mvn clean package

# Run the generated jar
java -jar target/online-shopping-mvc-1.0.0.jar
```

---

## Running with MySQL

1. Start MySQL on port `3306` (or set your own URL).
2. Run with the `mysql` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Optional environment variables:

| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/shopdb` |
| `SPRING_DATASOURCE_USERNAME` | `root` |
| `SPRING_DATASOURCE_PASSWORD` | `root` |

---

## Running in Production

Use the `prod` profile with PostgreSQL and Flyway migrations:

```bash
java -jar target/online-shopping-mvc-1.0.0.jar --spring.profiles.active=prod
```

Required environment variables:

```text
DATABASE_URL=jdbc:postgresql://host:5432/shopdb
DATABASE_USER=your_user
DATABASE_PASSWORD=your_password
```

In production, payment mode is set to `razorpay`, so also provide:

```text
RAZORPAY_KEY_ID=your_key_id
RAZORPAY_KEY_SECRET=your_key_secret
```

---

## Payment Modes

| Mode | Description |
|---|---|
| `demo` (default) | Simulated payment, no gateway needed |
| `razorpay` | Real Razorpay payment using API keys |

Set the mode with `PAYMENT_MODE=demo` or `PAYMENT_MODE=razorpay`.

---

## Email Configuration (Optional)

Emails use SMTP. Defaults point to `localhost:1025` (useful with tools like MailHog).

| Variable | Default |
|---|---|
| `MAIL_FROM` | `no-reply@shop.local` |
| `SMTP_HOST` | `localhost` |
| `SMTP_PORT` | `1025` |
| `SMTP_USERNAME` | empty |
| `SMTP_PASSWORD` | empty |
| `SMTP_AUTH` | `false` |
| `SMTP_TLS` | `false` |

---

## File Uploads

Uploaded product images are saved to the `uploads` folder by default.
Change this with `UPLOAD_DIR=/path/to/uploads`.
Maximum file size is **5 MB** per file.

---

## Main Pages

| URL | Description |
|---|---|
| `/` | Home / product listing |
| `/products/{id}` | Product details |
| `/cart` | Shopping cart |
| `/login` | Login |
| `/register` | Create account |
| `/orders` | My orders |
| `/favorites` | My favorites |
| `/admin` | Admin dashboard (admin only) |
| `/actuator/health` | Health check |

---

## Project Structure

```text
src/main/java/com/ayan/shop
├── config        # Security, seed data, web configuration
├── controller    # Shop and admin controllers
├── model         # JPA entities
├── repository    # Spring Data repositories
└── service       # Business logic (orders, payments, files)

src/main/resources
├── templates     # Thymeleaf HTML pages
├── static        # CSS and images
├── db/migration  # Flyway SQL migrations (prod)
└── application*.properties
```

---

## License

This project is for learning and educational purposes.
