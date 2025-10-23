-- Create database
CREATE DATABASE car_rental_db;
USE car_rental_db;

-- Create users table
CREATE TABLE users (
    id VARCHAR(10) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    role ENUM('admin','user','seller') NOT NULL,
    contact VARCHAR(100)
);

-- Create cars table
CREATE TABLE cars (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(50),
    model VARCHAR(50),
    price_per_day DOUBLE,
    category VARCHAR(50),
    status ENUM('Available','Rented','Sold'),
    image_path VARCHAR(255),
    owner_id VARCHAR(10),
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Create rentals table
CREATE TABLE rentals (
    id VARCHAR(10) PRIMARY KEY,
    car_id VARCHAR(10),
    user_id VARCHAR(10),
    start_date DATE,
    end_date DATE,
    total_amount DOUBLE,
    FOREIGN KEY (car_id) REFERENCES cars(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Insert sample users
INSERT INTO users (id, username, password, role, contact) VALUES
('U001','admin@demo','admin123','admin','Admin Office'),
('U002','user@demo','user123','user','Customer'),
('U003','seller@demo','seller123','seller','+91-9876543210');

-- Insert sample cars
INSERT INTO cars (id, name, model, price_per_day, category, status, image_path, owner_id) VALUES
('C001','Toyota Camry','Camry',3500,'Sedan','Available',NULL,'U003'),
('C002','Honda Civic','Civic',3200,'Sedan','Available',NULL,'U003');
