---

🚗 CarRentalMultiRole — Java Swing App

CarRentalMultiRole is a simple multi-role car rental and dealership demo application built with Java Swing (Java 17+).
It allows users to log in as Admin, Seller, or User, with different permissions for each role.
This project is in-memory only — no database is required — and it demonstrates clean UI design, file handling, and basic CRUD logic.


---

📋 Features

👥 Multi-role Login

Admin → Can view all users and manage all cars.

Seller → Can add, update, or delete their cars and upload images.

User → Can browse cars and view car details and images.


🚘 Car Management

Add new cars with price, status, and image.

Update or delete existing cars.

Upload and store images in the ./images/ folder.

Automatically generates new car IDs (C001, C002, ...).


🖼️ Image Handling

Uploaded images are copied to a local folder images/.

Scaled image thumbnails are displayed in tables and previews.


🧠 In-Memory Data

No database required — all data is stored in lists during runtime.

Demo users and sample cars are preloaded automatically.



---
