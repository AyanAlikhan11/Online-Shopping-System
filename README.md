# Shoplane - Online Shopping System

Shoplane is a complete Spring Boot MVC web application for browsing products, managing a cart, placing orders, tracking delivery, and administering the store.

It runs locally with an embedded H2 database, so you do not need Docker, PostgreSQL, an email account, or a payment account for the normal demo. Optional SMTP email and Razorpay payment test mode are also included.

This is an educational project. It is useful for learning Spring MVC, Thymeleaf, Spring Security, JPA, and payment-flow handling, but it is not ready for real customers without the production hardening described at the end.

## Contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Demo accounts and first run](#demo-accounts-and-first-run)
- [Using the app](#using-the-app)
- [Configuration](#configuration)
- [Payments](#payments)
- [Email](#email-optional)
- [Product images](#product-images)
- [Database](#database)
- [Testing and building](#testing-and-building)
- [Main routes](#main-routes)
- [Project structure](#project-structure)
- [Security notes](#security-notes)
- [Changing the admin account](#changing-the-admin-account)
- [Troubleshooting](#troubleshooting)
- [Known limitations before production](#known-limitations-before-production)

## Features

### Storefront

- Product catalogue with text search across product names and descriptions.
- Filters for category, type, size, color, minimum price, and maximum price.
- Product detail pages with multiple images, image switching, and click-to-enlarge.
- Favorites that can be added or removed.
- Session-based cart with quantity updates, stock validation, and a live cart counter.
- Checkout with delivery address and automatic stock reservation.
- Order history and order detail pages.
- Customer-controlled cancellation of unpaid orders, with stock restored.
- Delivery tracking timeline updated by the administrator.
- Registration and login with BCrypt password hashing.

### Administration

- Admin dashboard showing products, orders, and registered customers.
- Add, edit, and delete products.
- Product images from URLs or direct file uploads.
- Deleting a product also removes it from customer favorites; existing order items keep their purchase-time name and price snapshot.
- Update paid order delivery stages and add optional tracking notes.
- Edit customer name, email, and delivery address.
- Disable or re-enable customer accounts.
- Delete customers only when they have no order history.
- Prices are displayed in INR and Razorpay payments use INR.

### Application behavior

- Custom 400, 403, 404, and 500 error pages.
- Public health endpoint at `/actuator/health`.
- Optional asynchronous order-confirmation email.
- Optional Razorpay Checkout with server-side signature and captured-payment verification.
- Local H2 database for development and PostgreSQL with Flyway migrations for production.

## Tech stack

| Layer | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Web | Spring MVC |
| Views | Thymeleaf |
| Security | Spring Security, BCrypt, CSRF protection |
| Persistence | Spring Data JPA, Hibernate |
| Local database | H2 file database / MySQL (profile) |
| Production database | PostgreSQL |
| Migrations | Flyway |
| Email | Spring Mail / JavaMail |
| Payments | Demo mode, optional Razorpay Checkout |
| Build | Maven 3.9+ |

## Requirements

Install these before starting:

- **JDK 17 or newer**
- **Apache Maven 3.9 or newer** (the project does not include a Maven wrapper)
- A modern browser such as Chrome, Edge, or Firefox

An internet connection is needed for the first Maven dependency download. After dependencies are cached, the core demo works offline, but the sample Unsplash images and Razorpay Checkout still need internet.

Check the installation:

```powershell
java -version
mvn -version
```

If Maven is not installed, you can also import `pom.xml` into IntelliJ IDEA or Eclipse and run the `ShopApplication` class directly.

## Quick start

Open PowerShell in the project folder containing `pom.xml`.

```powershell
mvn test
mvn spring-boot:run
```

Open `http://localhost:8080`.

Leave the PowerShell window open while using the app. Press `Ctrl+C` to stop it.

The first run creates a local H2 database at `data/shopdb.mv.db` and an upload folder at `uploads/`. Both are excluded from Git.

### If port 8080 is already in use

```powershell
$env:SERVER_PORT="8081"
mvn spring-boot:run
```

Then open `http://localhost:8081`.

### macOS and Linux

The same commands work:

```bash
mvn test
mvn spring-boot:run
```

Set environment variables with `export` instead of `$env:`, for example:

```bash
export ADMIN_EMAIL="owner@example.com"
export ADMIN_PASSWORD="LongUniquePassword123!"
mvn spring-boot:run
```

### Run the packaged jar

```powershell
mvn -q -DskipTests package
java -jar target/online-shopping-mvc-1.0.0.jar
```

## Demo accounts and first run

### Admin account

Default local login:

- Email: `admin@shop.local`
- Password: `ChangeMe123!`

The admin account is created only when the configured admin email does not already exist.

You can choose your own admin credentials before the first run:

```powershell
$env:ADMIN_EMAIL="owner@example.com"
$env:ADMIN_PASSWORD="LongUniquePassword123!"
mvn spring-boot:run
```

Changing `ADMIN_EMAIL` or `ADMIN_PASSWORD` after the database has already been created does **not** rename or update the existing admin account. If the new email is not already present, the app can create a second admin account instead. See [Changing the admin account](#changing-the-admin-account).

The production profile rejects the default password and passwords shorter than 12 characters.

### Customer account

Customers create their own account from **Log in > Create an account**. Registration requires:

- A name
- A valid-looking email address
- A password of at least 8 characters

### Sample data

On a fresh database, the app creates:

- One administrator
- Three sample products: Wireless Headphones, Classic Sneakers, and Minimal Watch

Sample products are created only when the product table is empty. Existing data is never overwritten.

### Reset the local demo

Stop the app, then remove the local data and uploads:

```powershell
Remove-Item -Recurse -Force .\data
Remove-Item -Recurse -Force .\uploads
mvn spring-boot:run
```

This deletes all local customers, orders, products, and uploaded images. The next start recreates the demo data.

## Using the app

### Customer walkthrough

Browsing products is public. The cart, favorites, checkout, and orders require a customer account.

1. Open `http://localhost:8080`.
2. Select **Log in > Create an account**, register, and log in.
3. Browse products and use the search box or category, type, size, color, and price filters.
4. Open a product to switch images, enlarge the main image, change quantity, and add it to the cart.
5. Use **Favorites** to save or remove products.
6. Open **Cart** to update quantities or remove items.
7. Enter a delivery address and select **Place demo order**.
8. On the payment page, select **Confirm demo payment**. No real money is charged.
9. Open **Orders** to see the order and its delivery timeline.
10. To cancel an unpaid order, open it from **Orders**, select **Complete payment**, then use **Cancel order** on the payment page. Cancelling restores the reserved stock.

The cart is stored in the HTTP session, so restarting the server or logging out clears the cart.

### Administrator walkthrough

1. Log in with the admin account.
2. Open **Admin** from the navigation bar.
3. Select **Add product** and fill in the product details.
4. Add image URLs separated by commas or new lines, or upload image files.
5. Save the product and verify it appears on the storefront.
6. In **Orders & delivery**, open a paid order and choose a delivery stage: `PLACED`, `PACKED`, `SHIPPED`, `OUT_FOR_DELIVERY`, or `DELIVERED`.
7. Optionally add a delivery note. The customer sees each stage and note in the order timeline.
8. In **Registered customers**, edit customer details, disable an account, re-enable it, or delete a customer with no orders. The customer editor only manages customer accounts; admin accounts are separate.

Delivery stages can only be updated after payment is confirmed. A customer cannot access `/admin`; the app returns the custom 403 page.

Order status values are `AWAITING_PAYMENT`, `PLACED`, `PACKED`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, and `CANCELLED`. Payment status values are `PENDING`, `PAID`, and `CANCELLED`.

### Quick checks

- Public storefront: `http://localhost:8080/`
- Login: `http://localhost:8080/login`
- Registration: `http://localhost:8080/register`
- Health: `http://localhost:8080/actuator/health`
- Missing product (404): `http://localhost:8080/products/999999`
- Access denied (403): log in as a customer and open `http://localhost:8080/admin`

## Configuration

Configuration is supplied through environment variables. Set them in the same PowerShell window before running Maven.

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port |
| `ADMIN_EMAIL` | `admin@shop.local` | Admin email used when the admin is first created |
| `ADMIN_PASSWORD` | `ChangeMe123!` | Admin password used when the admin is first created |
| `PAYMENT_MODE` | `demo` | `demo` or `razorpay` |
| `RAZORPAY_KEY_ID` | empty | Razorpay public key ID |
| `RAZORPAY_KEY_SECRET` | empty | Razorpay secret key; never expose it in the browser |
| `SMTP_HOST` | `localhost` | SMTP server host |
| `SMTP_PORT` | `1025` | SMTP server port |
| `SMTP_USERNAME` | empty | SMTP username |
| `SMTP_PASSWORD` | empty | SMTP password |
| `SMTP_AUTH` | `false` | Enable SMTP authentication |
| `SMTP_TLS` | `false` | Enable STARTTLS |
| `MAIL_FROM` | `no-reply@shop.local` | Sender address for order emails |
| `UPLOAD_DIR` | `uploads` | Directory for uploaded product images |
| `SPRING_PROFILES_ACTIVE` | empty | Set to `prod` for PostgreSQL deployment |
| `DATABASE_URL` | empty | PostgreSQL JDBC URL, production only |
| `DATABASE_USER` | empty | PostgreSQL username, production only |
| `DATABASE_PASSWORD` | empty | PostgreSQL password, production only |

Example for a local session:

```powershell
$env:SERVER_PORT="8081"
$env:ADMIN_EMAIL="owner@example.com"
$env:ADMIN_PASSWORD="LongUniquePassword123!"
$env:PAYMENT_MODE="demo"
mvn spring-boot:run
```

Environment variables set with `$env:` last only for the current PowerShell session. To make them permanent, set them through Windows System Properties or your deployment platform.

## Payments

### Demo mode

Demo mode is the default and requires no payment account.

1. Add products to the cart.
2. Enter a delivery address.
3. Select **Place demo order**.
4. Select **Confirm demo payment** on the payment page.

The order becomes paid, stock stays reserved, and the delivery timeline starts at `PLACED`. No money is charged and no payment-provider request is made.

### Razorpay test mode

To try the real Razorpay Checkout in test mode:

```powershell
$env:PAYMENT_MODE="razorpay"
$env:RAZORPAY_KEY_ID="rzp_test_your_key_id"
$env:RAZORPAY_KEY_SECRET="your_test_key_secret"
mvn spring-boot:run
```

Then complete checkout and select **Pay securely**.

How the payment flow works:

1. The app creates a Razorpay order on the server.
2. The browser opens Razorpay Checkout with the public key and order ID.
3. Razorpay returns the payment ID, order ID, and signature.
4. The app validates the signature on the server.
5. The app fetches the payment from Razorpay and checks that it is `captured`, matches the order, and matches the expected amount and currency.
6. Only then is the order marked `PAID`.

Secret keys stay on the server. Never place `RAZORPAY_KEY_SECRET` in a template, browser script, or committed file.

If an unpaid order is abandoned, the customer can return to it from **Orders**, complete payment later, or open the payment page and cancel it. Cancellation checks Razorpay for received money or payment attempts before releasing stock. If a payment was already received, cancellation is refused.

The production profile forces Razorpay mode and requires both keys before startup.

## Email (optional)

Order confirmation email is optional. Configure SMTP only if you want it:

```powershell
$env:SMTP_HOST="smtp.example.com"
$env:SMTP_PORT="587"
$env:SMTP_USERNAME="smtp-user"
$env:SMTP_PASSWORD="smtp-password"
$env:SMTP_AUTH="true"
$env:SMTP_TLS="true"
$env:MAIL_FROM="verified@example.com"
mvn spring-boot:run
```

Email is sent asynchronously after payment. If SMTP is missing, unavailable, or slow, the paid order still succeeds and a warning is written to the server log. Mail connection, read, and write timeouts are five seconds.

## Product images

The admin product form supports:

- Image URLs separated by commas or new lines.
- Direct uploads of JPEG, PNG, WebP, or GIF images.
- A combination of both.

Upload limits:

- 5 MB per image.
- 25 MB per request.

Uploaded files are stored in `uploads/` by default and served from `/uploads/**`. Change the location with:

```powershell
$env:UPLOAD_DIR="D:\shop-images"
```

For a real deployment, use a persistent volume or object storage. Files on an ephemeral server disk will be lost when the server is replaced.

Deleting a product does not delete its uploaded image files from disk; clean the upload folder periodically if that matters to you.

Sample products use images from Unsplash, so those images need internet access.

## Database

### Local development

- Database: H2 file database
- File: `data/shopdb.mv.db`
- Username: `sa`
- Password: empty
- Schema: automatically updated by Hibernate
- Flyway: disabled locally

To reset the local database, stop the app and delete the `data` folder. This removes all local users, products, and orders.

### MySQL development (Optional)

You can run the application with MySQL instead of H2:

- Driver: `mysql-connector-j`
- Configuration: `src/main/resources/application-mysql.properties`
- Default connection: `jdbc:mysql://localhost:3306/shopdb` (username `root`, password `root`)

Start with the MySQL profile:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### PostgreSQL production

Use a fresh PostgreSQL database and set:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/shoplane"
$env:DATABASE_USER="shoplane"
$env:DATABASE_PASSWORD="your_database_password"
$env:ADMIN_EMAIL="owner@example.com"
$env:ADMIN_PASSWORD="replace_with_a_unique_long_password"
$env:RAZORPAY_KEY_ID="rzp_test_your_key_id"
$env:RAZORPAY_KEY_SECRET="your_test_key_secret"
mvn clean package
java -jar target/online-shopping-mvc-1.0.0.jar
```

The production profile:

- Enables Flyway migrations.
- Sets Hibernate to `validate` instead of updating the schema.
- Requires a non-default admin password of at least 12 characters.
- Requires Razorpay keys.
- Uses a secure session cookie, which requires HTTPS.
- Enables Thymeleaf template caching for performance.

Flyway creates the schema and applies the migrations in `src/main/resources/db/migration/`:

- `V1__init.sql` creates the initial tables.
- `V2__customer_active_and_order_history.sql` adds customer account status and order status history.

Back up an existing production database before applying migrations.

Do not run the `prod` profile over plain `http://localhost`. Put the application behind a reverse proxy or hosting service that terminates HTTPS.

## Testing and building

Run all automated tests:

```powershell
mvn test
```

The test suite uses an in-memory H2 database and does not modify your local `data` folder. It currently contains 13 tests covering:

- Public pages and the health endpoint.
- Admin authentication and customer access denial.
- The custom 403 and 404 pages.
- Registration.
- Admin product creation and product image upload.
- Admin customer editing and account disabling.
- Order tracking page rendering.
- Checkout stock reservation.
- Demo payment history.
- Cancellation stock restoration.

Build a runnable jar without rerunning tests:

```powershell
mvn -q -DskipTests package
```

Run it:

```powershell
java -jar target/online-shopping-mvc-1.0.0.jar
```

## Main routes

| Route | Access | Purpose |
| --- | --- | --- |
| `/` | Public | Storefront and product filters |
| `/products/{id}` | Public | Product details |
| `/login` | Public | Login |
| `/register` | Public | Customer registration |
| `/actuator/health` | Public | Health status |
| `/uploads/**` | Public | Uploaded product images |
| `/cart` | Customer | View and update cart |
| `/checkout` | Customer | Create an order and reserve stock |
| `/favorites` | Customer | Saved products |
| `/orders` | Customer | Own order history |
| `/orders/{id}` | Customer/Admin | Order details and delivery timeline |
| `/pay/{id}` | Customer | Payment page |
| `/pay/{id}/demo` | Customer | Confirm demo payment |
| `/pay/{id}/verify` | Customer | Verify Razorpay payment |
| `/pay/{id}/cancel` | Customer | Cancel an unpaid order |
| `/admin` | Admin | Admin dashboard |
| `/admin/products/new` | Admin | Add a product |
| `/admin/products/{id}/edit` | Admin | Edit a product |
| `/admin/orders/{id}/status` | Admin | Update delivery status |
| `/admin/customers/{id}/edit` | Admin | Edit a customer |
| `/admin/customers/{id}/toggle` | Admin | Disable or enable a customer |
| `/403` | Public | Custom access-denied page |

## Project structure

```text
online-shopping-mvc/
|-- pom.xml
|-- README.md
|-- .gitignore
|-- src/
|   |-- main/
|   |   |-- java/com/ayan/shop/
|   |   |   |-- ShopApplication.java
|   |   |   |-- model/
|   |   |   |   |-- Customer.java
|   |   |   |   |-- Product.java
|   |   |   |   |-- Purchase.java
|   |   |   |   |-- PurchaseItem.java
|   |   |   |   |-- Favorite.java
|   |   |   |   `-- OrderStatusHistory.java
|   |   |   |-- repository/
|   |   |   |   |-- Customers.java
|   |   |   |   |-- Products.java
|   |   |   |   |-- Purchases.java
|   |   |   |   |-- Favorites.java
|   |   |   |   `-- OrderStatusHistoryRepository.java
|   |   |   |-- controller/
|   |   |   |   |-- ShopController.java
|   |   |   |   |-- AdminController.java
|   |   |   |   |-- GlobalView.java
|   |   |   |   `-- GlobalExceptionHandler.java
|   |   |   |-- service/
|   |   |   |   |-- ShopService.java
|   |   |   |   |-- PaymentService.java
|   |   |   |   `-- FileStorageService.java
|   |   |   `-- config/
|   |   |       |-- SecurityConfig.java
|   |   |       |-- WebConfig.java
|   |   |       |-- AsyncConfig.java
|   |   |       |-- SeedData.java
|   |   |       `-- ProductionConfig.java
|   |   `-- resources/
|   |       |-- application.properties
|   |       |-- application-mysql.properties
|   |       |-- application-prod.properties
|   |       |-- templates/
|   |       |   |-- home.html
|   |       |   |-- product.html
|   |       |   |-- cart.html
|   |       |   |-- pay.html
|   |       |   |-- orders.html
|   |       |   |-- order.html
|   |       |   |-- favorites.html
|   |       |   |-- admin.html
|   |       |   |-- product-form.html
|   |       |   |-- customer-form.html
|   |       |   |-- login.html
|   |       |   |-- register.html
|   |       |   |-- nav.html
|   |       |   |-- error.html
|   |       |   `-- error/
|   |       |-- static/
|   |       |   |-- css/style.css
|   |       |   `-- img/product-placeholder.svg
|   |       `-- db/migration/
|   |           |-- V1__init.sql
|   |           `-- V2__customer_active_and_order_history.sql
|   `-- test/
|       |-- java/com/ayan/shop/
|       |   |-- ShopApplicationTests.java
|       |   |-- AdminFlowTests.java
|       |   `-- ShopServiceTests.java
|       `-- resources/application.properties
|-- data/                 (created locally, ignored by Git)
`-- uploads/              (created locally, ignored by Git)
```

## Security notes

- Passwords are hashed with BCrypt.
- All Thymeleaf forms include CSRF protection through Spring Security.
- `/admin/**` requires the `ADMIN` role.
- Customers can only view their own orders and cannot open another customer's payment page.
- Product uploads are restricted by content type and size.
- Uploaded files are served only from the configured upload directory.
- The health endpoint is public, but details are hidden.
- The H2 web console is not enabled by default.
- Mail health is disabled so an unused SMTP server cannot make the whole app unhealthy.
- Secrets and credentials should be supplied through environment variables, never committed to Git.
- Production uses secure session cookies and should always run behind HTTPS.
- The default admin password is for local development only.

## Changing the admin account

There is currently no admin account settings page.

### Fresh local database

The simplest option is to reset the local demo database and recreate the admin:

```powershell
# Stop the app with Ctrl+C first.
Remove-Item -Recurse -Force .\data
Remove-Item -Recurse -Force .\uploads
$env:ADMIN_EMAIL="newadmin@example.com"
$env:ADMIN_PASSWORD="LongUniquePassword123!"
mvn spring-boot:run
```

This deletes all local customers, orders, products, and uploaded images.

### Existing local database

To keep existing data, stop the app first and back up the `data` folder. The H2 database file is locked while the app is running.

Generate a BCrypt hash:

```powershell
$crypto = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\security\spring-security-crypto" -Recurse -Filter "spring-security-crypto-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
$jcl = Get-ChildItem "$env:USERPROFILE\.m2\repository\org\springframework\spring-jcl" -Recurse -Filter "spring-jcl-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
$tmp = "$env:TEMP\AdminHash.java"

@'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class AdminHash {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
'@ | Set-Content -LiteralPath $tmp -Encoding ascii

$hash = java -cp "$crypto;$jcl" $tmp "LongUniquePassword123!"
$hash
```

Update the admin row:

```powershell
$h2 = Get-ChildItem "$env:USERPROFILE\.m2\repository\com\h2database\h2" -Recurse -Filter "h2-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
$db = "jdbc:h2:file:" + ((Join-Path (Get-Location) "data\shopdb") -replace '\\','/')
$sql = "UPDATE customers SET email='newadmin@example.com', password='" + $hash + "' WHERE role='ADMIN'"
java -cp $h2 org.h2.tools.Shell -url $db -user sa -sql $sql
```

Start the app and log in with the new email and password.

For PostgreSQL production, update the same columns with a BCrypt hash using your normal database tools. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` before the first production startup so the initial account is correct.

## Troubleshooting

| Problem | Solution |
| --- | --- |
| `mvn is not recognized` | Install Maven and add its `bin` folder to `Path`; open a new PowerShell window. |
| `java is not recognized` | Install JDK 17+ and add its `bin` folder to `Path`. |
| Port 8080 already in use | Run `$env:SERVER_PORT="8081"; mvn spring-boot:run` and open port 8081. |
| Login fails | Check the email and password. A disabled customer cannot log in until an admin re-enables the account. |
| Admin env changes have no effect | The admin is seeded only once. See [Changing the admin account](#changing-the-admin-account). |
| Product photos are missing | Sample images need internet access. Upload local images or use valid image URLs. |
| Upload rejected | Use JPEG, PNG, WebP, or GIF, at most 5 MB per file and 25 MB total. |
| Email is not received | Configure SMTP. The order still succeeds if email fails; check the server log. |
| Database is locked | Stop the running app before opening the H2 file with another tool. |
| 403 page appears | You are not allowed to access that page. Customers cannot open `/admin`. |
| 404 page appears | The product, order, or customer does not exist. |
| Razorpay Checkout does not open | Check internet access, keys, and whether `PAYMENT_MODE=razorpay`. The secret key must be set on the server. |
| Production startup fails | Check that `ADMIN_PASSWORD` is at least 12 characters and is not the default, Razorpay keys are set, and database variables are valid. |

## Known limitations before production

This project is a functional educational demo. Before accepting real customers, address these items:

- **No payment webhooks or automatic reconciliation.** If Razorpay captures a payment but the browser closes before returning, the order stays pending until an operator reconciles it.
- **No automatic expiry or refund rule for reserved stock.** Abandoned unpaid orders hold stock until cancelled.
- **Email is asynchronous but not queued with retries.** A reliable email service and retry queue are still needed.
- **Uploads use local disk by default.** Use persistent object storage for a real deployment, and add file cleanup for deleted products.
- **No admin account settings page.** Admin credentials must be seeded or changed through the database.
- **No password reset or email verification.**
- **Cart is session-based.** Restarting the server or clearing the session clears the cart.
- **Delivery tracking is manually updated by the administrator.** It is not a live courier feed and there is no courier tracking number.
- **Prices and Razorpay payments use INR only.**
- **Order totals are product subtotals.** There is no tax, shipping, discount, or coupon calculation.
- **Size and color are descriptive product fields.** There are no selectable variants with separate stock.
- **Catalogue filtering is in memory and has no pagination.** Add database search and paging for a large catalogue.
- **No login rate limiting or account lockout.**
- **No browser end-to-end, load, or security testing suite yet.**
- **Production operations are incomplete.** Add backups, monitoring, alerting, secret management, HTTPS, and a domain before launch.

Run the complete sandbox payment flow, including cancellation and reconciliation scenarios, before switching to live Razorpay keys.
