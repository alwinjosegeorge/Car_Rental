import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/**
 * CarRentalSwingFrontend.java
 * Single-file Swing frontend for a Car Rental and Dealership System.
 * Java 17 compatible.
 *
 * Improvements over original:
 * - Proper date parsing with LocalDate and days calculation
 * - Validations and error handling
 * - UI refresh after actions
 * - Safe ID generation
 * - Status updates for cars when rented/sold/returned
 *
 * Save as CarRentalSwingFrontend.java, then:
 * javac CarRentalSwingFrontend.java
 * java CarRentalSwingFrontend
 */
public class CarRentalSwingFrontend {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    // -------------------- Models --------------------
    public static class Car {
        public String id;
        public String brand;
        public String model;
        public int year;
        public double pricePerDay;
        public String status; // Available, Rented, Sold

        public Car(String id, String brand, String model, int year, double pricePerDay, String status) {
            this.id = id;
            this.brand = brand;
            this.model = model;
            this.year = year;
            this.pricePerDay = pricePerDay;
            this.status = status;
        }

        @Override
        public String toString() {
            return id + " - " + brand + " " + model + " (" + year + ") [" + status + "]";
        }
    }

    public static class User {
        public String id;
        public String name;
        public String email;
        public String role; // customer, dealer, admin

        public User(String id, String name, String email, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }

    public static class Rental {
        public String id;
        public String carId;
        public String userId;
        public LocalDate startDate;
        public LocalDate endDate;
        public double totalPrice;

        public Rental(String id, String carId, String userId, LocalDate startDate, LocalDate endDate, double totalPrice) {
            this.id = id;
            this.carId = carId;
            this.userId = userId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalPrice = totalPrice;
        }
    }

    // -------------------- Services (in-memory placeholders) --------------------
    public static class CarService {
        private static final List<Car> carList = new ArrayList<>();

        static {
            // sample data
            carList.add(new Car("C001", "Toyota", "Camry", 2020, 3500.0, "Available"));
            carList.add(new Car("C002", "Honda", "Civic", 2019, 3200.0, "Available"));
            carList.add(new Car("C003", "Ford", "Mustang", 2021, 7000.0, "Sold"));
            carList.add(new Car("C004", "Hyundai", "i20", 2018, 1800.0, "Rented"));
        }

        public static List<Car> fetchCars() {
            return new ArrayList<>(carList);
        }

        public static void addCar(Car car) {
            carList.add(car);
        }

        public static void updateCar(Car updated) {
            for (int i = 0; i < carList.size(); i++) {
                if (carList.get(i).id.equals(updated.id)) {
                    carList.set(i, updated);
                    return;
                }
            }
        }

        public static void deleteCar(String carId) {
            carList.removeIf(c -> c.id.equals(carId));
        }

        public static Car findById(String id) {
            return carList.stream().filter(c -> c.id.equals(id)).findFirst().orElse(null);
        }

        public static String nextId() {
            // generate next ID like C005
            int max = carList.stream().mapToInt(c -> {
                try {
                    return Integer.parseInt(c.id.replaceAll("\\D+", ""));
                } catch (Exception e) {
                    return 0;
                }
            }).max().orElse(0);
            return String.format("C%03d", max + 1);
        }
    }

    public static class RentalService {
        private static final List<Rental> rentals = new ArrayList<>();

        public static List<Rental> fetchRentals() {
            return new ArrayList<>(rentals);
        }

        public static void bookRental(Rental r) {
            rentals.add(r);
            Car c = CarService.findById(r.carId);
            if (c != null) c.status = "Rented";
        }

        public static void returnRental(String rentalId) {
            Rental found = rentals.stream().filter(r -> r.id.equals(rentalId)).findFirst().orElse(null);
            if (found != null) {
                Car c = CarService.findById(found.carId);
                if (c != null) c.status = "Available";
                rentals.remove(found);
            }
        }

        public static String nextId() {
            int max = rentals.stream().mapToInt(r -> {
                try {
                    return Integer.parseInt(r.id.replaceAll("\\D+", ""));
                } catch (Exception e) {
                    return 0;
                }
            }).max().orElse(0);
            return String.format("R%04d", max + 1);
        }
    }

    public static class UserService {
        private static final List<User> users = new ArrayList<>();

        static {
            users.add(new User("U001", "Alice", "alice@example.com", "customer"));
            users.add(new User("U002", "Bob", "bob@dealer.com", "dealer"));
            users.add(new User("U003", "Admin", "admin@system.com", "admin"));
        }

        public static User authenticate(String email, String password, String role) {
            // Placeholder: real auth later. We do case-insensitive email and role match.
            return users.stream().filter(u -> u.email.equalsIgnoreCase(email) && u.role.equalsIgnoreCase(role)).findFirst().orElse(null);
        }

        public static List<User> fetchAllUsers() {
            return new ArrayList<>(users);
        }
    }

    // -------------------- Main Frame with CardLayout --------------------
    public static class MainFrame extends JFrame {
        private CardLayout cardLayout = new CardLayout();
        private JPanel cards = new JPanel(cardLayout);

        // UI panels
        private LoginUI loginUI;
        private DashboardUI dashboardUI;
        private CarListUI carListUI;
        private RentalUI rentalUI;
        private SalesUI salesUI;
        private CarManagementUI carManagementUI;

        // Currently logged-in user
        private User currentUser = null;

        public MainFrame() {
            setTitle("Car Rental & Dealership System");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1000, 700);
            setLocationRelativeTo(null);

            // init UIs
            loginUI = new LoginUI(this);
            dashboardUI = new DashboardUI(this);
            carListUI = new CarListUI(this);
            rentalUI = new RentalUI(this);
            salesUI = new SalesUI(this);
            carManagementUI = new CarManagementUI(this);

            cards.add(loginUI, "login");
            cards.add(dashboardUI, "dashboard");
            cards.add(carListUI, "carlist");
            cards.add(rentalUI, "rental");
            cards.add(salesUI, "sales");
            cards.add(carManagementUI, "carmgmt");

            add(cards);
            showLogin();
        }

        public void showLogin() {
            cardLayout.show(cards, "login");
        }

        public void showDashboard(User user) {
            this.currentUser = user;
            dashboardUI.setUser(user);
            dashboardUI.refresh();
            cardLayout.show(cards, "dashboard");
        }

        public void showCarList() {
            carListUI.loadTable();
            cardLayout.show(cards, "carlist");
        }

        public void showRental() {
            rentalUI.loadCars();
            rentalUI.refreshRentalsTable();
            cardLayout.show(cards, "rental");
        }

        public void showSales() {
            salesUI.loadCars();
            cardLayout.show(cards, "sales");
        }

        public void showCarManagement() {
            carManagementUI.loadCars();
            cardLayout.show(cards, "carmgmt");
        }

        public User getCurrentUser() {
            return currentUser;
        }
    }

    // -------------------- Login UI --------------------
    public static class LoginUI extends JPanel {
        private MainFrame parent;
        private JTextField emailField;
        private JPasswordField passwordField;
        private JComboBox<String> roleCombo;

        public LoginUI(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(40, 120, 40, 120));

            JLabel title = new JLabel("Car Rental & Dealership - Login", SwingConstants.CENTER);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
            add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            form.add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            emailField = new JTextField(20);
            form.add(emailField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            form.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            passwordField = new JPasswordField(20);
            form.add(passwordField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            form.add(new JLabel("Role:"), gbc);
            gbc.gridx = 1;
            roleCombo = new JComboBox<>(new String[]{"customer", "dealer", "admin"});
            form.add(roleCombo, gbc);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JButton loginBtn = new JButton("Login");
            form.add(loginBtn, gbc);

            add(form, BorderLayout.CENTER);

            loginBtn.addActionListener(e -> doLogin());
        }

        private void doLogin() {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();

            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter email and password.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            User u = UserService.authenticate(email, password, role);
            if (u != null) {
                JOptionPane.showMessageDialog(this, "Welcome, " + u.name);
                parent.showDashboard(u);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials or role. (Placeholder auth)", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // -------------------- Dashboard UI (role-based) --------------------
    public static class DashboardUI extends JPanel {
        private MainFrame parent;
        private User user;
        private JLabel welcomeLabel;
        private JTabbedPane tabs = new JTabbedPane();

        public DashboardUI(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());

            JPanel top = new JPanel(new BorderLayout());
            welcomeLabel = new JLabel("", SwingConstants.LEFT);
            welcomeLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            top.add(welcomeLabel, BorderLayout.WEST);

            JButton logout = new JButton("Logout");
            logout.addActionListener(e -> doLogout());
            top.add(logout, BorderLayout.EAST);

            add(top, BorderLayout.NORTH);
            add(tabs, BorderLayout.CENTER);
        }

        public void setUser(User u) {
            this.user = u;
        }

        public void refresh() {
            tabs.removeAll();
            welcomeLabel.setText("Logged in as: " + user.name + " (" + user.role + ")");

            switch (user.role.toLowerCase()) {
                case "customer":
                    tabs.add("Browse Cars", parent.carListUI);
                    tabs.add("Rent a Car", parent.rentalUI);
                    break;
                case "dealer":
                    tabs.add("Inventory", parent.carListUI);
                    tabs.add("Manage Cars", parent.carManagementUI);
                    tabs.add("Sales", parent.salesUI);
                    break;
                case "admin":
                    tabs.add("All Users", createAdminUsersPanel());
                    tabs.add("All Cars", parent.carListUI);
                    tabs.add("All Rentals", createAdminRentalsPanel());
                    break;
                default:
                    tabs.add("Browse Cars", parent.carListUI);
            }
        }

        private void doLogout() {
            parent.showLogin();
        }

        private JPanel createAdminUsersPanel() {
            JPanel p = new JPanel(new BorderLayout());
            DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Name", "Email", "Role"}, 0);
            JTable table = new JTable(model);
            for (User u : UserService.fetchAllUsers()) {
                model.addRow(new Object[]{u.id, u.name, u.email, u.role});
            }
            p.add(new JScrollPane(table), BorderLayout.CENTER);
            return p;
        }

        private JPanel createAdminRentalsPanel() {
            JPanel p = new JPanel(new BorderLayout());
            DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "CarID", "UserID", "Start", "End", "Total"}, 0);
            JTable table = new JTable(model);
            for (Rental r : RentalService.fetchRentals()) {
                model.addRow(new Object[]{r.id, r.carId, r.userId, r.startDate.toString(), r.endDate.toString(), r.totalPrice});
            }
            p.add(new JScrollPane(table), BorderLayout.CENTER);
            return p;
        }
    }

    // -------------------- Car List UI --------------------
    public static class CarListUI extends JPanel {
        private MainFrame parent;
        private JTable table;
        private DefaultTableModel model;

        public CarListUI(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());

            model = new DefaultTableModel(new Object[]{"ID", "Brand", "Model", "Year", "Price/day", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table = new JTable(model);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            JButton refresh = new JButton("Refresh");
            JButton back = new JButton("Back to Dashboard");
            bottom.add(refresh);
            bottom.add(back);

            refresh.addActionListener(e -> loadTable());
            back.addActionListener(e -> parent.showDashboard(parent.getCurrentUser()));

            add(bottom, BorderLayout.SOUTH);
        }

        public void loadTable() {
            model.setRowCount(0);
            DecimalFormat df = new DecimalFormat("#,###.##");
            for (Car c : CarService.fetchCars()) {
                model.addRow(new Object[]{c.id, c.brand, c.model, c.year, df.format(c.pricePerDay), c.status});
            }
        }
    }

    // -------------------- Rental UI --------------------
    public static class RentalUI extends JPanel {
        private MainFrame parent;
        private JComboBox<Car> carCombo;
        private JTextField startField;
        private JTextField endField;
        private JLabel priceLabel;
        private DefaultTableModel rentModel;
        private JTable rentTable;

        public RentalUI(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            form.add(new JLabel("Select Car:"), gbc);
            gbc.gridx = 1;
            carCombo = new JComboBox<>();
            form.add(carCombo, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            form.add(new JLabel("Start Date (YYYY-MM-DD):"), gbc);
            gbc.gridx = 1;
            startField = new JTextField(15);
            form.add(startField, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            form.add(new JLabel("End Date (YYYY-MM-DD):"), gbc);
            gbc.gridx = 1;
            endField = new JTextField(15);
            form.add(endField, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            form.add(new JLabel("Estimated Price:"), gbc);
            gbc.gridx = 1;
            priceLabel = new JLabel("0.0");
            form.add(priceLabel, gbc);

            gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
            JPanel btns = new JPanel();
            JButton calc = new JButton("Calculate Price");
            JButton book = new JButton("Book Rental");
            JButton back = new JButton("Back");
            btns.add(calc);
            btns.add(book);
            btns.add(back);
            form.add(btns, gbc);

            add(form, BorderLayout.NORTH);

            // Table of current rentals to allow return (simple demo)
            rentModel = new DefaultTableModel(new Object[]{"ID", "CarID", "UserID", "Start", "End", "Total"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            rentTable = new JTable(rentModel);
            add(new JScrollPane(rentTable), BorderLayout.CENTER);

            calc.addActionListener(e -> calculatePrice());
            book.addActionListener(e -> bookRental());
            back.addActionListener(e -> parent.showDashboard(parent.getCurrentUser()));

            // refresh rental table
            JButton refreshRentals = new JButton("Refresh Rentals");
            refreshRentals.addActionListener(e -> refreshRentalsTable());
            JPanel bottom = new JPanel();
            JButton returnSelected = new JButton("Return Selected Rental");
            returnSelected.addActionListener(e -> returnSelectedRental());
            bottom.add(refreshRentals);
            bottom.add(returnSelected);
            add(bottom, BorderLayout.SOUTH);
        }

        public void loadCars() {
            carCombo.removeAllItems();
            for (Car c : CarService.fetchCars()) {
                carCombo.addItem(c);
            }
        }

        private void calculatePrice() {
            Car c = (Car) carCombo.getSelectedItem();
            if (c == null) {
                JOptionPane.showMessageDialog(this, "No car selected.");
                return;
            }
            String start = startField.getText().trim();
            String end = endField.getText().trim();
            if (start.isEmpty() || end.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter both start and end dates to calculate price.");
                return;
            }
            try {
                LocalDate s = LocalDate.parse(start);
                LocalDate e = LocalDate.parse(end);
                if (e.isBefore(s)) {
                    JOptionPane.showMessageDialog(this, "End date cannot be before start date.");
                    return;
                }
                long days = ChronoUnit.DAYS.between(s, e) + 1; // inclusive
                double total = days * c.pricePerDay;
                DecimalFormat df = new DecimalFormat("#,###.##");
                priceLabel.setText(df.format(total) + " (" + days + " days)");
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
            }
        }

        private void bookRental() {
            Car c = (Car) carCombo.getSelectedItem();
            if (c == null) {
                JOptionPane.showMessageDialog(this, "Select a car first.");
                return;
            }
            if (!"Available".equalsIgnoreCase(c.status)) {
                JOptionPane.showMessageDialog(this, "Selected car is not available for rent.");
                return;
            }
            String start = startField.getText().trim();
            String end = endField.getText().trim();
            if (start.isEmpty() || end.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter start and end dates.");
                return;
            }
            LocalDate s, e;
            try {
                s = LocalDate.parse(start);
                e = LocalDate.parse(end);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
                return;
            }
            if (e.isBefore(s)) {
                JOptionPane.showMessageDialog(this, "End date cannot be before start date.");
                return;
            }
            long days = ChronoUnit.DAYS.between(s, e) + 1; // inclusive
            double total = days * c.pricePerDay;

            String rid = RentalService.nextId();
            String uid = parent.getCurrentUser() != null ? parent.getCurrentUser().id : "U000";
            Rental r = new Rental(rid, c.id, uid, s, e, total);
            RentalService.bookRental(r);

            JOptionPane.showMessageDialog(this, "Booked! Rental ID: " + rid + " | Total: " + new DecimalFormat("#,###.##").format(total));
            // Clear fields and refresh
            startField.setText("");
            endField.setText("");
            priceLabel.setText("0.0");
            loadCars(); // will re-populate car combo to show new statuses
            refreshRentalsTable();
            parent.carListUI.loadTable();
            parent.carManagementUI.loadCars();
        }

        public void refreshRentalsTable() {
            rentModel.setRowCount(0);
            DecimalFormat df = new DecimalFormat("#,###.##");
            for (Rental r : RentalService.fetchRentals()) {
                rentModel.addRow(new Object[]{r.id, r.carId, r.userId, r.startDate.toString(), r.endDate.toString(), df.format(r.totalPrice)});
            }
        }

        private void returnSelectedRental() {
            int row = rentTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a rental row to return.");
                return;
            }
            String rid = (String) rentModel.getValueAt(row, 0);
            int choice = JOptionPane.showConfirmDialog(this, "Mark rental " + rid + " as returned?", "Return", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                RentalService.returnRental(rid);
                JOptionPane.showMessageDialog(this, "Rental returned.");
                refreshRentalsTable();
                loadCars();
                parent.carListUI.loadTable();
                parent.carManagementUI.loadCars();
            }
        }
    }

    // -------------------- Sales UI --------------------
    public static class SalesUI extends JPanel {
        private MainFrame parent;
        private JComboBox<Car> carCombo;
        private JTextField buyerNameField;
        private JTextField buyerContactField;

        public SalesUI(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Select Car to Sell:"), gbc);
            gbc.gridx = 1; carCombo = new JComboBox<>(); form.add(carCombo, gbc);

            gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Buyer Name:"), gbc);
            gbc.gridx = 1; buyerNameField = new JTextField(15); form.add(buyerNameField, gbc);

            gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Buyer Contact:"), gbc);
            gbc.gridx = 1; buyerContactField = new JTextField(15); form.add(buyerContactField, gbc);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JPanel btns = new JPanel();
            JButton sellBtn = new JButton("Mark as Sold");
            JButton back = new JButton("Back");
            btns.add(sellBtn); btns.add(back);
            form.add(btns, gbc);

            add(form, BorderLayout.NORTH);

            sellBtn.addActionListener(e -> doSell());
            back.addActionListener(e -> parent.showDashboard(parent.getCurrentUser()));
        }

        public void loadCars() {
            carCombo.removeAllItems();
            for (Car c : CarService.fetchCars()) {
                if (!"Sold".equalsIgnoreCase(c.status)) carCombo.addItem(c);
            }
        }

        private void doSell() {
            Car c = (Car) carCombo.getSelectedItem();
            if (c == null) { JOptionPane.showMessageDialog(this, "No car selected."); return; }
            if ("Sold".equalsIgnoreCase(c.status)) { JOptionPane.showMessageDialog(this, "Car already sold."); return; }
            String buyer = buyerNameField.getText().trim();
            String contact = buyerContactField.getText().trim();
            if (buyer.isEmpty() || contact.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter buyer details.");
                return;
            }
            // Mark car sold
            c.status = "Sold";
            CarService.updateCar(c);
            JOptionPane.showMessageDialog(this, "Car " + c.id + " marked as SOLD to " + buyer + ".");
            buyerNameField.setText("");
            buyerContactField.setText("");
            loadCars();
            parent.carListUI.loadTable();
            parent.carManagementUI.loadCars();
        }
    }

    // -------------------- Car Management UI --------------------
    public static class CarManagementUI extends JPanel {
        private MainFrame parent;
        private JTable table;
        private DefaultTableModel model;

        private JTextField idField, brandField, modelField, yearField, priceField, statusField;

        public CarManagementUI(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());

            model = new DefaultTableModel(new Object[]{"ID", "Brand", "Model", "Year", "Price/day", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            table = new JTable(model);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx=0; gbc.gridy=0; form.add(new JLabel("ID:"), gbc); gbc.gridx=1; idField = new JTextField(10); idField.setEditable(false); form.add(idField, gbc);
            gbc.gridx=0; gbc.gridy=1; form.add(new JLabel("Brand:"), gbc); gbc.gridx=1; brandField = new JTextField(10); form.add(brandField, gbc);
            gbc.gridx=0; gbc.gridy=2; form.add(new JLabel("Model:"), gbc); gbc.gridx=1; modelField = new JTextField(10); form.add(modelField, gbc);
            gbc.gridx=0; gbc.gridy=3; form.add(new JLabel("Year:"), gbc); gbc.gridx=1; yearField = new JTextField(10); form.add(yearField, gbc);
            gbc.gridx=0; gbc.gridy=4; form.add(new JLabel("Price/day:"), gbc); gbc.gridx=1; priceField = new JTextField(10); form.add(priceField, gbc);
            gbc.gridx=0; gbc.gridy=5; form.add(new JLabel("Status:"), gbc); gbc.gridx=1; statusField = new JTextField(10); form.add(statusField, gbc);

            gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=2;
            JPanel btns = new JPanel();
            JButton add = new JButton("Add New");
            JButton save = new JButton("Save Changes");
            JButton edit = new JButton("Edit Selected");
            JButton delete = new JButton("Delete Selected");
            JButton refresh = new JButton("Refresh");
            btns.add(add); btns.add(edit); btns.add(save); btns.add(delete); btns.add(refresh);
            form.add(btns, gbc);

            add(form, BorderLayout.EAST);

            refresh.addActionListener(e -> loadCars());
            add.addActionListener(e -> prepareAddNew());
            edit.addActionListener(e -> editSelected());
            save.addActionListener(e -> saveChanges());
            delete.addActionListener(e -> deleteSelected());
        }

        public void loadCars() {
            model.setRowCount(0);
            DecimalFormat df = new DecimalFormat("#,###.##");
            for (Car c : CarService.fetchCars()) {
                model.addRow(new Object[]{c.id, c.brand, c.model, c.year, df.format(c.pricePerDay), c.status});
            }
        }

        private void prepareAddNew() {
            idField.setText(CarService.nextId());
            brandField.setText("");
            modelField.setText("");
            yearField.setText("");
            priceField.setText("");
            statusField.setText("Available");
        }

        private void saveChanges() {
            String id = idField.getText().trim();
            String brand = brandField.getText().trim();
            String modelS = modelField.getText().trim();
            String yearTxt = yearField.getText().trim();
            String priceTxt = priceField.getText().trim();
            String status = statusField.getText().trim();

            if (id.isEmpty() || brand.isEmpty() || modelS.isEmpty() || yearTxt.isEmpty() || priceTxt.isEmpty() || status.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.");
                return;
            }
            int year;
            double price;
            try {
                year = Integer.parseInt(yearTxt);
                price = Double.parseDouble(priceTxt);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number in year or price.");
                return;
            }
            Car existing = CarService.findById(id);
            if (existing != null) {
                existing.brand = brand;
                existing.model = modelS;
                existing.year = year;
                existing.pricePerDay = price;
                existing.status = status;
                CarService.updateCar(existing);
                JOptionPane.showMessageDialog(this, "Car updated.");
            } else {
                Car c = new Car(id, brand, modelS, year, price, status);
                CarService.addCar(c);
                JOptionPane.showMessageDialog(this, "Car added.");
            }
            loadCars();
            parent.carListUI.loadTable();
        }

        private void editSelected() {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a row to edit."); return; }
            String id = (String) model.getValueAt(row, 0);
            Car c = CarService.findById(id);
            if (c == null) return;
            // populate fields
            idField.setText(c.id);
            brandField.setText(c.brand);
            modelField.setText(c.model);
            yearField.setText(String.valueOf(c.year));
            priceField.setText(String.valueOf(c.pricePerDay));
            statusField.setText(c.status);
        }

        private void deleteSelected() {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a row to delete."); return; }
            String id = (String) model.getValueAt(row, 0);
            int choice = JOptionPane.showConfirmDialog(this, "Delete car " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                CarService.deleteCar(id);
                loadCars();
                parent.carListUI.loadTable();
            }
        }
    }
}
