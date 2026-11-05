# Sarathi — local website + database

The Sarathi website and Spring Boot API run together. The API saves accounts, equipment, bookings, and related records in MySQL. phpMyAdmin is a viewer for that same database.

## Start it in VS Code

1. Open `outputs/Sarathi-Spring-Backend` in VS Code.
2. Open **Terminal → New Terminal**.
3. Run `docker compose up --build` and wait until the app reports that it started.
4. Open the website at http://localhost:8080 and phpMyAdmin at http://localhost:8081.

The first start creates a local demo equipment owner, eight sample machines, and an available demo operator. Demo owner sign-in:

- Email: `demo.owner@sarathi.test`
- Password: `SarathiDemo#2026`

Demo operator sign-in (choose Sign in):

- Email: `demo.operator@sarathi.test`
- Password: `SarathiOperator#2026`

The operator profile is available for tractor, harvester, rotavator, sprayer, seed drill, and cultivator bookings in Nashik. To try a booking, create a separate Farmer account on the website, open **Find equipment**, choose a machine, select dates, optionally choose Kiran Patil as operator, and submit. Open **My bookings** on the website to see the saved request. In phpMyAdmin, select server `db`, sign in as `sarathi` with password `sarathi_dev_password`, open the `sarathi` database, then select the `bookings` table. The same record is stored there. User accounts are in `users`; sample and owner listings are in `equipment`.

To see the owner side, sign in as the demo owner and open **My bookings** to accept or decline farmer requests. The **Log out** button appears in the top bar after sign-in; the profile area also has a log out icon.

Stop the app with `Ctrl+C`. To stop and remove containers while keeping saved records, run `docker compose down`. Do not run `docker compose down -v` unless you intend to erase the database.

## Notes

- Equipment rates and payments are sample/offline tracking. No real payment is processed.
- Demo seed data is local-only and controlled by `SARATHI_DEMO_DATA_ENABLED` in `compose.yaml`.
- phpMyAdmin displays MySQL records; it is not a frontend connection. The browser sends requests to Sarathi's API, and the API stores them in MySQL.