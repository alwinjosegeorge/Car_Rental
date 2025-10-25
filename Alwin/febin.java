/* CarRentalFull.java
    Fully functional single-file Swing Car Rental app (Enhanced)
    - Database: **SUPABASE (PostgreSQL) Integration**
    - Features: Booking History, Booking Approval Flow (Pending/Confirmed)
*/

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.sql.*;

public class CarRentalFull {

    // ---------------- Models ----------------
    public static class Car {
        public String id, name, model, category, status, imagePath, ownerId;
        public double pricePerDay;
        public String fuelType;
        public int seats;
        public String transmission;

        public Car(String id, String name, String model, double pricePerDay, String category, String status,
                   String imagePath, String ownerId, String fuelType, int seats, String transmission) {
            this.id = id; this.name = name; this.model=model; this.pricePerDay=pricePerDay;
            this.category=category; this.status=status; this.imagePath=imagePath; this.ownerId=ownerId;
            this.fuelType=fuelType; this.seats=seats; this.transmission=transmission;
        }
    }

    public static class User {
        public String id, username, password, role, contact;
        public User(String id, String username, String password, String role, String contact) {
            this.id=id; this.username=username; this.password=password; this.role=role; this.contact=contact;
        }
    }

    public static class Booking{
        public String id, carId, userId, pickupPlace, pickupDate, pickupTime;
        public int days; public double totalPrice;
        public String status; // Pending, Confirmed, Rejected
        
        public Booking(String id,String carId,String userId,String place,String date,String time,int days,double total, String status){
            this.id=id; this.carId=carId; this.userId=userId; this.pickupPlace=place; this.pickupDate=date; this.pickupTime=time; this.days=days; this.totalPrice=total;
            this.status = status;
        }
    }

    // ---------------- DataStore (SUPABASE PostgreSQL Integration) ----------------
    public static class DataStore {
        
        // ⚠️ SUPABASE POOLER SETTINGS (NEW PROJECT)
        private static final String DB_HOST = "pooler.pwgmnmoxgblcawhmdpo.supabase.co"; // Pooler Host
        private static final String DB_PORT = "6543"; // Pooler Port
        private static final String DB_NAME = "postgres"; 
        private static final String DB_USER = "postgres"; 
        private static final String DB_PASS = "alwinjava123"; // Assumed simple password
        
        private static final String JDBC_URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?pgbouncer=true&sslmode=require"; 
        private static Connection connection = null; 

        static {
            try {
                // 🛑 CORRECT DRIVER LOAD for PostgreSQL
                Class.forName("org.postgresql.Driver"); 
                connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS); 
                System.out.println("Supabase Database connected successfully.");
                
                createTablesIfNotExists();
                
                if (getUsersCount() == 0) {
                    initializeDefaultData();
                }

            } catch (Exception e) {
                System.err.println("Error connecting to Supabase: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Database Connection Failed! Check Host/Password/Firewall.", "DB Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1); 
            }
        }
        
        // *** Tables Creation (PostgreSQL Version) ***
        private static void createTablesIfNotExists() throws SQLException {
            Statement stmt = connection.createStatement();
            // Note: If tables exist, the "IF NOT EXISTS" prevents errors.
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, contact TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS cars (id TEXT PRIMARY KEY, name TEXT NOT NULL, model TEXT, price_per_day REAL NOT NULL, category TEXT, status TEXT, image_path TEXT, owner_id TEXT, fuel_type TEXT, seats INTEGER, transmission TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS bookings (id TEXT PRIMARY KEY, car_id TEXT, user_id TEXT, pickup_place TEXT, pickup_date TEXT, pickup_time TEXT, days INTEGER, total_price REAL, status TEXT DEFAULT 'Pending')");
            stmt.close();
        }

        // --- Helper Methods ---
        private static int getUsersCount() throws SQLException {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            stmt.close();
            return count;
        }

        private static void initializeDefaultData() {
            System.out.println("Initializing default users and cars...");
            try {
                 addUser(new User("U001", "admin@demo", "admin123", "admin", "Admin Office"));
                 addUser(new User("U002", "user@demo", "user123", "user", "Customer 1"));
                 addUser(new User("U004", "seller@demo", "seller123", "seller", "+91-9876543210"));
                 addUser(new User("U005", "seller1@demo", "seller123", "seller", "+91-9876543211"));
                 addCar(new Car("C001","Toyota Camry","Camry",3500,"Sedan","Available","images/Toyota_Camry.jpeg","U004","Petrol",5,"Automatic"));
                 addCar(new Car("C002","Honda Civic","Civic",3200,"Sedan","Available","images/Honda_Civic.jpeg","U005","Petrol",5,"Automatic"));
                 addCar(new Car("C003","BMW X5","X5",7000,"SUV","Available","images/BMW_X5.jpeg","U004","Diesel",5,"Automatic"));
            } catch (Exception e) {
                 System.err.println("Failed to insert default data: " + e.getMessage());
            }
        }

        // --- ID Generation ---
        private static String getNextId(String table, String prefix) {
            String sql = "SELECT MAX(CAST(SUBSTR(id, 2) AS INTEGER)) FROM " + table;
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) { int max = rs.getInt(1); return String.format(prefix + "%03d", max + 1); }
            } catch (SQLException e) { e.printStackTrace(); }
            return prefix + "001"; 
        }
        public static String nextCarId(){ return getNextId("cars", "C"); }
        public static String nextUserId(){ return getNextId("users", "U"); }
        public static String nextBookingId(){ return getNextId("bookings", "B"); }

        // --- Data Mappers ---
        private static Car mapCar(ResultSet rs) throws SQLException {
            return new Car(rs.getString("id"), rs.getString("name"), rs.getString("model"), rs.getDouble("price_per_day"),
                rs.getString("category"), rs.getString("status"), rs.getString("image_path"), rs.getString("owner_id"),
                rs.getString("fuel_type"), rs.getInt("seats"), rs.getString("transmission"));
        }
        private static User mapUser(ResultSet rs) throws SQLException {
            return new User(rs.getString("id"), rs.getString("username"), rs.getString("password"),
                rs.getString("role"), rs.getString("contact"));
        }
        private static Booking mapBooking(ResultSet rs) throws SQLException {
            return new Booking(rs.getString("id"), rs.getString("car_id"), rs.getString("user_id"), 
                rs.getString("pickup_place"), rs.getString("pickup_date"), rs.getString("pickup_time"),
                rs.getInt("days"), rs.getDouble("total_price"), rs.getString("status")); 
        }

        // --- CRUD and AUTH Operations ---
        public static Optional<User> authenticate(String username, String password, String role) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username); pstmt.setString(2, password); pstmt.setString(3, role);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) { return Optional.of(mapUser(rs)); }
            } catch (SQLException e) { e.printStackTrace(); } return Optional.empty();
        }
        public static boolean usernameExists(String username) {
            String sql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username); return pstmt.executeQuery().next();
            } catch (SQLException e) { e.printStackTrace(); } return false;
        }
        public static List<User> fetchUsers() {
            List<User> users = new ArrayList<>();
            String sql = "SELECT * FROM users";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) { users.add(mapUser(rs)); }
            } catch (SQLException e) { e.printStackTrace(); } return users;
        }
        public static List<Car> fetchCars() {
            List<Car> cars = new ArrayList<>();
            String sql = "SELECT * FROM cars";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) { cars.add(mapCar(rs)); }
            } catch (SQLException e) { e.printStackTrace(); } return cars;
        }
        public static List<Booking> fetchBookings() { // Fetch ALL bookings for Admin/Seller view
            List<Booking> allBookings = new ArrayList<>();
            String sql = "SELECT * FROM bookings";
            try (Statement stmt = connection.createStatement(); 
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) { allBookings.add(mapBooking(rs)); }
            } catch (SQLException e) { e.printStackTrace(); } return allBookings;
        }
        public static List<Booking> fetchBookingsByUserId(String userId) { // Fetch bookings by User ID
            List<Booking> bookings = new ArrayList<>();
            String sql = "SELECT * FROM bookings WHERE user_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) { bookings.add(mapBooking(rs)); }
            } catch (SQLException e) { e.printStackTrace(); } return bookings;
        }

        public static void updateBookingStatus(String bookingId, String newStatus) {
            String sql = "UPDATE bookings SET status = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, newStatus);
                pstmt.setString(2, bookingId);
                pstmt.executeUpdate();
            } catch (SQLException e) { 
                e.printStackTrace(); 
                JOptionPane.showMessageDialog(null, "DB Error: Could not update booking status.", "DB Write Error", JOptionPane.ERROR_MESSAGE); 
            }
        }

        public static void addCar(Car c) {
            String sql = "INSERT INTO cars (id, name, model, price_per_day, category, status, image_path, owner_id, fuel_type, seats, transmission) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, c.id); pstmt.setString(2, c.name); pstmt.setString(3, c.model); pstmt.setDouble(4, c.pricePerDay); 
                pstmt.setString(5, c.category); pstmt.setString(6, c.status); pstmt.setString(7, c.imagePath); pstmt.setString(8, c.ownerId);   
                pstmt.setString(9, c.fuelType); pstmt.setInt(10, c.seats); pstmt.setString(11, c.transmission); pstmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(null, "DB Error: Could not add car.", "DB Write Error", JOptionPane.ERROR_MESSAGE); }
        }
        public static void addUser(User u) {
            String sql = "INSERT INTO users (id, username, password, role, contact) VALUES (?,?,?,?,?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, u.id); pstmt.setString(2, u.username); pstmt.setString(3, u.password); 
                pstmt.setString(4, u.role); pstmt.setString(5, u.contact); pstmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(null, "DB Error: Could not add user.", "DB Write Error", JOptionPane.ERROR_MESSAGE); }
        }
        public static void addBooking(Booking b) {
            String sql = "INSERT INTO bookings (id, car_id, user_id, pickup_place, pickup_date, pickup_time, days, total_price, status) VALUES (?,?,?,?,?,?,?,?, 'Pending')";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, b.id); pstmt.setString(2, b.carId); pstmt.setString(3, b.userId);
                pstmt.setString(4, b.pickupPlace); pstmt.setString(5, b.pickupDate); pstmt.setString(6, b.pickupTime);
                pstmt.setInt(7, b.days); pstmt.setDouble(8, b.totalPrice); pstmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(null, "DB Error: Could not add booking.", "DB Write Error", JOptionPane.ERROR_MESSAGE); }
        }
        public static void updateCar(Car updated) {
            String sql = "UPDATE cars SET name=?, model=?, price_per_day=?, category=?, status=?, image_path=?, owner_id=?, fuel_type=?, seats=?, transmission=? WHERE id=?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, updated.name); pstmt.setString(2, updated.model); pstmt.setDouble(3, updated.pricePerDay);
                pstmt.setString(4, updated.category); pstmt.setString(5, updated.status); pstmt.setString(6, updated.imagePath);
                pstmt.setString(7, updated.ownerId); pstmt.setString(8, updated.fuelType); pstmt.setInt(9, updated.seats);
                pstmt.setString(10, updated.transmission); pstmt.setString(11, updated.id); pstmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(null, "DB Error: Could not update car.", "DB Write Error", JOptionPane.ERROR_MESSAGE); }
        }
        public static void deleteCar(String id) {
            String sql = "DELETE FROM cars WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, id); pstmt.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); JOptionPane.showMessageDialog(null, "DB Error: Could not delete car.", "DB Write Error", JOptionPane.ERROR_MESSAGE); }
        }
        public static User findUserById(String id) {
            String sql = "SELECT * FROM users WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) { return mapUser(rs); }
            } catch (SQLException e) { e.printStackTrace(); } return null;
        }
        public static Car findCarById(String id) {
            String sql = "SELECT * FROM cars WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) { return mapCar(rs); }
            } catch (SQLException e) { e.printStackTrace(); } return null;
        }
    }

    // ---------------- UI Utils ----------------
    public static class UIUtils {
        public static final DecimalFormat MONEY = new DecimalFormat("#,###.##");
        public static final String PLACEHOLDER_IMAGE = "images/placeholder.png";

        public static void ensureImagesFolder(){try{Files.createDirectories(Paths.get("images"));}catch(IOException e){System.err.println(e);}}

        public static String copyImageToStore(File sourceFile,String targetFileName)throws IOException{
            ensureImagesFolder();
            Path target = Paths.get("images",targetFileName);
            Files.copy(sourceFile.toPath(),target,StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace("\\","/");
        }

        public static ImageIcon loadScaledIcon(String imagePath,int w, int h){
            if(imagePath==null || !new File(imagePath).exists()) imagePath=PLACEHOLDER_IMAGE;
            try{
                BufferedImage img = ImageIO.read(new File(imagePath));
                return new ImageIcon(img.getScaledInstance(w,h,Image.SCALE_SMOOTH));
            }catch(IOException e){return null;}
        }

        public static JLabel makeHeader(String text){
            JLabel l = new JLabel(text,SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI",Font.BOLD,20));
            l.setOpaque(true);
            l.setBackground(new Color(18,120,225));
            l.setForeground(Color.WHITE);
            l.setBorder(new EmptyBorder(12,12,12,12));
            return l;
        }
    }

    // ---------------- Main Frame ----------------
    public static class MainFrame extends JFrame {
        private CardLayout cards = new CardLayout();
        private JPanel cardPanel = new JPanel(cards);
        private LoginPanel loginPanel; private DashboardPanel dashboardPanel;
        private User currentUser;

        public MainFrame(){
            setTitle("Car Rental — Connected Roles");
            setSize(1100,700);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            loginPanel = new LoginPanel(this);
            dashboardPanel = new DashboardPanel(this);

            cardPanel.add(loginPanel,"login");
            cardPanel.add(dashboardPanel,"dashboard");

            add(cardPanel);
            showLogin();
        }

        public void showLogin(){cards.show(cardPanel,"login");}
        public void showDashboard(User user){
            currentUser=user;
            dashboardPanel.setUser(user);
            cards.show(cardPanel,"dashboard");
        }
        public User getCurrentUser(){return currentUser;}
        public void refreshAll(){dashboardPanel.refreshAll();}
        public DashboardPanel getDashboardPanel(){return dashboardPanel;}
    }

    // ---------------- Login Panel ----------------
    public static class LoginPanel extends JPanel {
        private MainFrame parent;
        private JTextField txtUser; private JPasswordField txtPass; private JComboBox<String> roleBox;

        public LoginPanel(MainFrame parent){
            this.parent=parent;
            setLayout(new BorderLayout());
            setBackground(new Color(248,249,250));
            add(UIUtils.makeHeader("Car Rental & Dealership"),BorderLayout.NORTH);

            JPanel center = new JPanel(new GridBagLayout());
            center.setBorder(new EmptyBorder(24,120,24,120));
            center.setBackground(new Color(248,249,250));
            GridBagConstraints gbc = new GridBagConstraints(); gbc.insets=new Insets(10,10,10,10); gbc.fill=GridBagConstraints.HORIZONTAL;

            gbc.gridx=0; gbc.gridy=0; center.add(new JLabel("Username:"),gbc);
            gbc.gridx=1; txtUser = new JTextField(22); center.add(txtUser,gbc);

            gbc.gridx=0; gbc.gridy=1; center.add(new JLabel("Password:"),gbc);
            gbc.gridx=1; txtPass = new JPasswordField(22); center.add(txtPass,gbc);

            gbc.gridx=0; gbc.gridy=2; center.add(new JLabel("Role:"),gbc);
            gbc.gridx=1; roleBox = new JComboBox<>(new String[]{"user","seller","admin"}); center.add(roleBox,gbc);

            gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2;
            JPanel pbtn = new JPanel(); pbtn.setBackground(new Color(248,249,250));
            JButton btnLogin = new JButton("Login"); btnLogin.setBackground(new Color(37,150,190)); btnLogin.setForeground(Color.WHITE); btnLogin.setFocusPainted(false);
            btnLogin.addActionListener(e->doLogin());
            JButton btnSignup = new JButton("Create Account"); btnSignup.setBackground(new Color(37,180,90)); btnSignup.setForeground(Color.WHITE); btnSignup.setFocusPainted(false);
            btnSignup.addActionListener(e->doSignup());
            pbtn.add(btnLogin); pbtn.add(btnSignup); center.add(pbtn,gbc);

            add(center,BorderLayout.CENTER);
        }

        private void doLogin(){
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword());
            String role = (String)roleBox.getSelectedItem();

            if(u.isEmpty() || p.isEmpty()){
                JOptionPane.showMessageDialog(this,"Enter credentials.","Validation",JOptionPane.WARNING_MESSAGE);
                return;
            }

            Optional<User> found = DataStore.authenticate(u,p,role);
            if(found.isPresent()){
                String otp = JOptionPane.showInputDialog(this,
                        "We are facing an SMS issue.\nPlease use 910298 as your OTP:",
                        "OTP Verification", JOptionPane.INFORMATION_MESSAGE);
                if(otp != null && otp.equals("910298")){
                    parent.showDashboard(found.get());
                    txtPass.setText("");
                } else {
                    JOptionPane.showMessageDialog(this,"Invalid OTP! Login failed.","Login Failed",JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,"Invalid credentials or role.","Login Failed",JOptionPane.ERROR_MESSAGE);
            }
        }

        private void doSignup(){
            JTextField username = new JTextField();
            JPasswordField password = new JPasswordField();
            JComboBox<String> role = new JComboBox<>(new String[]{"user","seller"});
            JTextField contact = new JTextField();

            Object[] fields = {"Username:",username,"Password:",password,"Role:",role,"Contact:",contact};

            int ans = JOptionPane.showConfirmDialog(this,fields,"Create Account",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(ans==JOptionPane.OK_OPTION){
                String u=username.getText().trim();
                String p=new String(password.getPassword());
                String r=(String)role.getSelectedItem();
                String c=contact.getText().trim();
                if(u.isEmpty()||p.isEmpty()||c.isEmpty()){
                    JOptionPane.showMessageDialog(this,"Fill all fields.","Validation",JOptionPane.WARNING_MESSAGE); return;
                }
                if(DataStore.usernameExists(u)){
                    JOptionPane.showMessageDialog(this,"Username already exists!","Validation",JOptionPane.WARNING_MESSAGE); return;
                }
                User newUser = new User(DataStore.nextUserId(),u,p,r,c);
                DataStore.addUser(newUser);
                JOptionPane.showMessageDialog(this,"Account created! You can login now.","Success",JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public static class DashboardPanel extends JPanel {
        private MainFrame parent; private User user; private JTabbedPane tabs;
        private CardBrowsePanel browsePanel; 
        private SellerPanel sellerPanel; 
        private AdminPanel adminPanel;
        private BookingHistoryPanel historyPanel; 
        private AllBookingsPanel allBookingsPanel; // New Admin/Seller View
        private User currentUser; // The current user logged in

        public DashboardPanel(MainFrame parent){
            this.parent=parent;
            setLayout(new BorderLayout());

            JPanel top = new JPanel(new BorderLayout()); top.setBorder(new EmptyBorder(8,12,8,12)); top.setBackground(Color.WHITE);
            JLabel lbl = new JLabel("Dashboard",SwingConstants.LEFT); lbl.setFont(new Font("Segoe UI",Font.BOLD,18));
            top.add(lbl,BorderLayout.WEST);

            JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT)); rightControls.setBackground(Color.WHITE);
            JButton logout = new JButton("Logout"); logout.addActionListener(e->parent.showLogin());
            rightControls.add(logout); top.add(rightControls,BorderLayout.EAST);
            add(top,BorderLayout.NORTH);

            tabs = new JTabbedPane();
            browsePanel = new CardBrowsePanel(parent);
            sellerPanel = new SellerPanel(parent);
            adminPanel = new AdminPanel(parent);
            historyPanel = new BookingHistoryPanel(parent); 
            allBookingsPanel = new AllBookingsPanel(parent); // Initialize here
            add(tabs,BorderLayout.CENTER);
        }
        public void setUser(User user){
            this.user=user; 
            this.currentUser = user; // Set the class member here
            tabs.removeAll();
            tabs.addTab("Browse Cars",browsePanel);
            
            // User-specific history view
            if(user.role.equals("user")) {
                tabs.addTab("My Bookings", historyPanel); 
            }
            
            // Role-based inventory and management views
            switch(user.role.toLowerCase()){
                case "seller": 
                    tabs.addTab("My Inventory",sellerPanel); 
                    tabs.addTab("Car Bookings", allBookingsPanel); // Seller uses this view to track *their* car bookings
                    checkPendingBookings(); // Alert seller upon login/refresh
                    break;
                case "admin": 
                    tabs.addTab("Manage Cars",sellerPanel); 
                    tabs.addTab("Users",adminPanel); 
                    tabs.addTab("All Bookings", allBookingsPanel); // Admin sees ALL bookings
                    break;
            }
            browsePanel.setCurrentUser(user); 
            sellerPanel.setCurrentUser(user);
            historyPanel.setCurrentUser(user); 
            allBookingsPanel.setCurrentUser(user); // Pass user for filtering
            refreshAll();
        }

        public void refreshAll(){
            browsePanel.refresh(); 
            sellerPanel.refresh(); 
            adminPanel.refresh();
            allBookingsPanel.refresh();
            if(user!=null && user.role.equals("user")) {
                historyPanel.refresh(); 
            }
        }
        
        // FIX: The variable currentUser is now accessible due to the declaration fix above.
        private void checkPendingBookings() {
            if (currentUser == null) return; 
            
            int pendingCount = 0;
            String sellerId = currentUser.id;
            
            List<Booking> allBookings = DataStore.fetchBookings();
            
            for (Booking b : allBookings) {
                Car car = DataStore.findCarById(b.carId);
                // Check if the car belongs to the current seller, and if the booking is pending
                if (car != null && car.ownerId.equals(sellerId) && b.status.equals("Pending")) {
                    pendingCount++;
                }
            }
            
            if (pendingCount > 0) {
                JOptionPane.showMessageDialog(this, 
                    "<html>You have <b>" + pendingCount + " pending booking(s)</b> that require approval!</html>", 
                    "New Booking Alert", JOptionPane.INFORMATION_MESSAGE);
                tabs.setSelectedIndex(tabs.indexOfTab("Car Bookings")); // Switch to the approval tab
            }
        }
    }

    public static class CardBrowsePanel extends JPanel {
    private MainFrame parent; 
    private JPanel cardContainer; 
    private JScrollPane scrollPane;
    private JTextField txtSearch; 
    private User currentUser;

    public CardBrowsePanel(MainFrame parent){
        this.parent = parent; 
        setLayout(new BorderLayout()); 
        setBorder(new EmptyBorder(10,10,10,10));

        // Header
        JLabel header = new JLabel("🚗 Car Inventory", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        header.setOpaque(true);
        header.setBackground(new Color(18, 120, 225));
        header.setForeground(Color.WHITE);
        header.setBorder(new EmptyBorder(12,12,12,12));

        // Top panel with search
        JPanel topPanel = new JPanel(new BorderLayout(4,4));
        topPanel.add(header, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(20);
        searchPanel.add(txtSearch);
        JButton btnSearch = new JButton("Go");
        btnSearch.addActionListener(e -> refresh());
        searchPanel.add(btnSearch);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Card container with wrap layout inside scroll pane
        cardContainer = new JPanel(); 
        cardContainer.setLayout(new WrapLayout(FlowLayout.LEFT,16,16));
        scrollPane = new JScrollPane(cardContainer); 
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setCurrentUser(User u){
        currentUser = u;
    }

    public void refresh(){
        String query = txtSearch.getText().trim().toLowerCase();
        cardContainer.removeAll();

        for(Car c : DataStore.fetchCars()){
            // Search filter
            if(!query.isEmpty() && !(c.name.toLowerCase().contains(query) || c.category.toLowerCase().contains(query)))
                continue;

            // Card panel
            JPanel card = new JPanel(new BorderLayout(4,4));
            card.setPreferredSize(new Dimension(240,360));
            card.setBackground(Color.WHITE); 
            card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY,1,true));

            // Car image
            JLabel imgLabel = new JLabel();
            imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imgLabel.setIcon(UIUtils.loadScaledIcon(c.imagePath, 220, 140));
            card.add(imgLabel, BorderLayout.NORTH);

            // Info panel
            JPanel info = new JPanel(); 
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS)); 
            info.setBorder(new EmptyBorder(6,6,6,6));

            JLabel lblName = new JLabel(c.name + " (" + c.model + ")");
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 14)); 
            info.add(lblName);

            // Random star rating
            int stars = 3 + new Random().nextInt(3); 
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<stars;i++) sb.append("★"); 
            for(int i=stars;i<5;i++) sb.append("☆");
            JLabel lblRating = new JLabel(sb.toString());
            lblRating.setForeground(new Color(255,140,0)); 
            info.add(lblRating);

            // Car details
            info.add(new JLabel("Seats: " + c.seats + " | Fuel: " + c.fuelType + " | Transmission: " + c.transmission));
            info.add(new JLabel("Category: " + c.category));
            info.add(new JLabel("Price/day: ₹" + UIUtils.MONEY.format(c.pricePerDay)));

            // Status
            JLabel lblStatus = new JLabel("Status: " + c.status);
            lblStatus.setForeground("Available".equalsIgnoreCase(c.status) ? new Color(0,128,0) : Color.RED);
            info.add(lblStatus);

            // Admin sees owner
            if(currentUser.role.equals("admin")){
                User owner = DataStore.findUserById(c.ownerId);
                info.add(new JLabel("Owner: " + (owner != null ? owner.username : "Unknown")));
            }

            // User booking button
            if(currentUser.role.equals("user")){
                JButton bookBtn = new JButton("Book");
                bookBtn.setEnabled("Available".equalsIgnoreCase(c.status));
                bookBtn.addActionListener(e -> {
                    BookingDialog bd = new BookingDialog(parent, c, currentUser);
                    bd.setVisible(true);
                    // refreshAll happens inside the dialog confirmation!
                });
                info.add(bookBtn);
            }

            card.add(info, BorderLayout.CENTER);
            cardContainer.add(card);
        }

        cardContainer.revalidate(); 
        cardContainer.repaint();
    }
}


    public static class BookingDialog extends JDialog {
    public BookingDialog(MainFrame parent, Car car, User user){
        super(parent,"Book Car",true);
        setSize(400,420); 
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(); 
        gbc.insets = new Insets(10,10,10,10); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Pickup Place
        gbc.gridx=0; gbc.gridy=0; add(new JLabel("Pickup Place:"),gbc);
        gbc.gridx=1; 
        JTextField txtPlace=new JTextField(car.category+" Rent"); 
        add(txtPlace,gbc);

        // Pickup Date
        gbc.gridx=0; gbc.gridy=1; add(new JLabel("Pickup Date (dd/MM/yyyy):"),gbc);
        gbc.gridx=1; 
        JTextField txtDate=new JTextField(new SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date())); 
        add(txtDate,gbc);

        // Pickup Time
        gbc.gridx=0; gbc.gridy=2; add(new JLabel("Pickup Time (HH:mm):"),gbc);
        gbc.gridx=1; 
        JTextField txtTime=new JTextField(new SimpleDateFormat("HH:mm").format(new java.util.Date())); 
        add(txtTime,gbc);

        // No. of Days
        gbc.gridx=0; gbc.gridy=3; add(new JLabel("No. of Days:"),gbc);
        gbc.gridx=1; 
        JSpinner spinDays = new JSpinner(new SpinnerNumberModel(1,1,30,1)); 
        add(spinDays,gbc);

        // Total Price Label
        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2;
        JLabel lblTotal = new JLabel("Total Price: ₹" + UIUtils.MONEY.format(car.pricePerDay));
        add(lblTotal,gbc);

        // Update total dynamically when spinner changes
        spinDays.addChangeListener(e -> {
            int days = (Integer)spinDays.getValue();
            lblTotal.setText("Total Price: ₹" + UIUtils.MONEY.format(car.pricePerDay * days));
        });

        // Confirm Booking Button
        gbc.gridy=5;
        JButton confirm = new JButton("Confirm Booking");
        confirm.addActionListener(e->{
            int days = (Integer)spinDays.getValue();
            double total = car.pricePerDay*days;
            
            // Booking status set to pending
            DataStore.addBooking(new Booking(
                DataStore.nextBookingId(),
                car.id,
                user.id,
                txtPlace.getText().trim(),
                txtDate.getText().trim(),
                txtTime.getText().trim(),
                days,
                total,
                "Pending" // Status set to pending
            ));
            JOptionPane.showMessageDialog(this,"Booking sent for approval!","Pending Approval",JOptionPane.INFORMATION_MESSAGE);
            dispose();
            parent.refreshAll(); 
        });
        add(confirm,gbc);
    }
}

    public static class SellerPanel extends JPanel {
        private MainFrame parent; private User currentUser;
        private JTable tbl; private DefaultTableModel model;
        public SellerPanel(MainFrame parent){
            this.parent=parent; setLayout(new BorderLayout());

            JLabel header = UIUtils.makeHeader("My Inventory");
            add(header,BorderLayout.NORTH);

            model = new DefaultTableModel(new String[]{"ID","Name","Model","Price/Day","Status","Fuel","Seats","Transmission"},0){
                public boolean isCellEditable(int r, int c){return false;} 
            };
            tbl = new JTable(model);
            add(new JScrollPane(tbl),BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addBtn = new JButton("Add Car"); addBtn.addActionListener(e->addOrEditCar(null));
            JButton editBtn = new JButton("Edit Car"); editBtn.addActionListener(e->{int r=tbl.getSelectedRow(); if(r>=0) addOrEditCar(getSelectedCar(r));});
            JButton delBtn = new JButton("Delete Car"); delBtn.addActionListener(e->{int r=tbl.getSelectedRow(); if(r>=0) deleteCar(getSelectedCar(r));});
            bottom.add(addBtn); bottom.add(editBtn); bottom.add(delBtn);
            add(bottom,BorderLayout.SOUTH);
        }

        public void setCurrentUser(User u){this.currentUser=u; refresh();}

        private Car getSelectedCar(int row){
            String id=(String)model.getValueAt(row,0);
            return DataStore.findCarById(id);
        }

        public void refresh(){
            model.setRowCount(0);
            for(Car c:DataStore.fetchCars()){
                if(currentUser.role.equals("seller") && !c.ownerId.equals(currentUser.id)) continue;
                model.addRow(new Object[]{c.id, c.name, c.model, c.pricePerDay, c.status, c.fuelType, c.seats, c.transmission});
            }
        }

        private void addOrEditCar(Car car){
            if(car!=null && currentUser.role.equals("seller") && !car.ownerId.equals(currentUser.id)){
                JOptionPane.showMessageDialog(this,"You cannot edit this car.","Permission Denied",JOptionPane.WARNING_MESSAGE);
                return;
            }

            JTextField name = new JTextField(car!=null?car.name:"");
            JTextField modelField = new JTextField(car!=null?car.model:"");
            JTextField price = new JTextField(car!=null?""+car.pricePerDay:"");
            JTextField fuel = new JTextField(car!=null?car.fuelType:"");
            JTextField seats = new JTextField(car!=null?""+car.seats:"");
            JTextField trans = new JTextField(car!=null?car.transmission:"");
            JTextField category = new JTextField(car!=null?car.category:"");
            JComboBox<String> statusBox = new JComboBox<>(new String[]{"Available","Rented"});
            if(car!=null) statusBox.setSelectedItem(car.status);

            JButton imgBtn = new JButton("Choose Image"); JLabel imgLbl = new JLabel(car!=null?car.imagePath:"");
            imgBtn.addActionListener(e->{
                JFileChooser fc = new JFileChooser(); fc.setFileFilter(new FileNameExtensionFilter("Image files","jpg","jpeg","png"));
                int res = fc.showOpenDialog(this);
                if(res==JFileChooser.APPROVE_OPTION){
                    imgLbl.setText(fc.getSelectedFile().getAbsolutePath());
                }
            });

            JPanel panel = new JPanel(new GridLayout(0,2,6,6));
            panel.add(new JLabel("Name:")); panel.add(name);
            panel.add(new JLabel("Model:")); panel.add(modelField);
            panel.add(new JLabel("Price/Day:")); panel.add(price);
            panel.add(new JLabel("Fuel:")); panel.add(fuel);
            panel.add(new JLabel("Seats:")); panel.add(seats);
            panel.add(new JLabel("Transmission:")); panel.add(trans);
            panel.add(new JLabel("Category:")); panel.add(category);
            panel.add(new JLabel("Status:")); panel.add(statusBox);
            panel.add(new JLabel("Image:")); panel.add(imgBtn);

            int ans = JOptionPane.showConfirmDialog(this,panel,car==null?"Add Car":"Edit Car",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(ans==JOptionPane.OK_OPTION){
                try{
                    String newImg = imgLbl.getText().isEmpty()? (car!=null?car.imagePath:UIUtils.PLACEHOLDER_IMAGE)
                            :UIUtils.copyImageToStore(new File(imgLbl.getText()),UUID.randomUUID().toString()+".png");
                    Car newCar = new Car(
                            car!=null?car.id:DataStore.nextCarId(),
                            name.getText().trim(),
                            modelField.getText().trim(),
                            Double.parseDouble(price.getText().trim()),
                            category.getText().trim(),
                            (String)statusBox.getSelectedItem(),
                            newImg,
                            currentUser.role.equals("seller")?currentUser.id: (car!=null?car.ownerId:currentUser.id),
                            fuel.getText().trim(),
                            Integer.parseInt(seats.getText().trim()),
                            trans.getText().trim()
                    );
                    if(car==null) DataStore.addCar(newCar); else DataStore.updateCar(newCar);
                    refresh();
                }catch(Exception e){JOptionPane.showMessageDialog(this,"Invalid input!","Error",JOptionPane.ERROR_MESSAGE);}
            }
        }

        private void deleteCar(Car car){
            if(car!=null && currentUser.role.equals("seller") && !car.ownerId.equals(currentUser.id)){
                JOptionPane.showMessageDialog(this,"You cannot edit this car.","Permission Denied",JOptionPane.WARNING_MESSAGE);
                return;
            }
            int ans = JOptionPane.showConfirmDialog(this,"Delete car "+car.name+"?","Confirm",JOptionPane.YES_NO_OPTION);
            if(ans==JOptionPane.YES_OPTION){
                DataStore.deleteCar(car.id); refresh();
            }
        }
    }

    public static class AdminPanel extends JPanel {
        private JTable tbl; private DefaultTableModel model;
        public AdminPanel(MainFrame parent){
            setLayout(new BorderLayout());
            JLabel header = UIUtils.makeHeader("Registered Users"); add(header,BorderLayout.NORTH);
            model = new DefaultTableModel(new String[]{"ID","Username","Role","Contact"},0){
                public boolean isCellEditable(int r, int c){return false;} 
            };
            tbl = new JTable(model); add(new JScrollPane(tbl),BorderLayout.CENTER);
        }

        public void refresh(){
            model.setRowCount(0);
            for(User u:DataStore.fetchUsers()){
                model.addRow(new Object[]{u.id,u.username,u.role,u.contact});
            }
        }
    }
    
    public static class BookingHistoryPanel extends JPanel {
        private MainFrame parent; 
        private User currentUser;
        private JTable tbl; 
        private DefaultTableModel model;

        public BookingHistoryPanel(MainFrame parent){
            this.parent=parent; 
            setLayout(new BorderLayout());

            JLabel header = UIUtils.makeHeader("My Booking History");
            add(header,BorderLayout.NORTH);

            model = new DefaultTableModel(new String[]{"Booking ID", "Car", "Days", "Status", "Pickup Place", "Date", "Time", "Total Price (₹)"}, 0){
                public boolean isCellEditable(int row,int col){return false;}
            };
            tbl = new JTable(model);
            add(new JScrollPane(tbl),BorderLayout.CENTER);
        }

        public void setCurrentUser(User u){this.currentUser=u; refresh();}

        public void refresh(){
            model.setRowCount(0);
            if(currentUser == null) return;
            
            List<Booking> bookings = DataStore.fetchBookingsByUserId(currentUser.id);
            
            for(Booking b : bookings){
                Car car = DataStore.findCarById(b.carId);
                String carName = (car != null) ? car.name + " (" + car.model + ")" : "Unknown Car";
                String totalPrice = "₹" + UIUtils.MONEY.format(b.totalPrice);
                
                model.addRow(new Object[]{
                    b.id, 
                    carName, 
                    b.days,
                    b.status, // Show status here
                    b.pickupPlace, 
                    b.pickupDate, 
                    b.pickupTime, 
                    totalPrice
                });
            }
            model.fireTableDataChanged();
        }
    }
    
    // NEW ADMIN/SELLER VIEW FOR ALL BOOKINGS (Filtered by role)
    public static class AllBookingsPanel extends JPanel {
        private MainFrame parent;
        private User currentUser;
        private JTable tbl; 
        private DefaultTableModel model;
        private JButton btnApprove;
        private JButton btnReject;
        private int selectedRow = -1;

        public AllBookingsPanel(MainFrame parent){
            this.parent = parent;
            setLayout(new BorderLayout());
            
            model = new DefaultTableModel(); // Initialize here, headers set in refresh()
            tbl = new JTable(model); 

            // Control Panel for Approval/Rejection
            btnApprove = new JButton("Approve");
            btnReject = new JButton("Reject");
            
            btnApprove.addActionListener(e -> handleBookingAction("Confirmed"));
            btnReject.addActionListener(e -> handleBookingAction("Rejected"));
            
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            controlPanel.add(btnApprove);
            controlPanel.add(btnReject);

            tbl.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && tbl.getSelectedRow() != -1) {
                    selectedRow = tbl.convertRowIndexToModel(tbl.getSelectedRow());
                    updateControlButtons();
                }
            });

            add(controlPanel, BorderLayout.SOUTH);
            add(new JScrollPane(tbl), BorderLayout.CENTER);
        }

        public void setCurrentUser(User u){this.currentUser=u; refresh();}
        
        private void updateControlButtons() {
            if (selectedRow >= 0 && model.getColumnCount() > 0) {
                // Status is the 3rd column (index 2) in the data structure
                Object statusValue = model.getValueAt(selectedRow, 2); 
                boolean isPending = "Pending".equals(statusValue);
                
                // Only show buttons for Sellers if the booking is Pending
                if (currentUser.role.equals("seller")) {
                    btnApprove.setVisible(true);
                    btnReject.setVisible(true);
                    btnApprove.setEnabled(isPending);
                    btnReject.setEnabled(isPending);
                } else { // Admin (buttons hidden, view only)
                    btnApprove.setVisible(false);
                    btnReject.setVisible(false);
                }
            } else {
                btnApprove.setEnabled(false);
                btnReject.setEnabled(false);
                if (currentUser!=null && !currentUser.role.equals("admin")) { // Hide buttons if nothing is selected and not admin
                    btnApprove.setVisible(false);
                    btnReject.setVisible(false);
                }
            }
        }
        
        private void handleBookingAction(String action) {
            if (selectedRow < 0) return;
            
            String bookingId = (String)model.getValueAt(selectedRow, 0);
            String carId = (String)model.getValueAt(selectedRow, 1); // Car ID is the 2nd column (index 1)

            // 1. Update Booking Status in DB
            DataStore.updateBookingStatus(bookingId, action);
            
            // 2. If Confirmed, update Car Status to Rented
            if (action.equals("Confirmed")) {
                Car currentCar = DataStore.findCarById(carId); 
                if (currentCar != null) {
                    currentCar.status = "Rented";
                    DataStore.updateCar(currentCar);
                }
            }
            
            JOptionPane.showMessageDialog(this, "Booking " + bookingId + " has been " + action.toLowerCase() + ".", "Action Complete", JOptionPane.INFORMATION_MESSAGE);
            parent.refreshAll();
        }

        public void refresh(){
            if(currentUser == null) return;
            
            // 1. Define Headers: Status is the 3rd column (index 2)
            String[] headers = currentUser.role.equals("admin") 
                ? new String[]{"Booking ID", "Car ID", "Status", "Buyer", "Owner Contact", "Date", "Time", "Days", "Total Price (₹)"}
                : new String[]{"Booking ID", "Car ID", "Status", "Buyer", "Buyer Contact", "Date", "Time", "Days", "Total Price (₹)"};
            
            model = new DefaultTableModel(headers, 0);
            tbl.setModel(model);
            
            add(UIUtils.makeHeader(currentUser.role.equals("admin") ? "All System Bookings" : "Bookings for My Cars"), BorderLayout.NORTH);
            
            List<Booking> allBookings = DataStore.fetchBookings(); 
            
            for(Booking b : allBookings){
                Car car = DataStore.findCarById(b.carId);
                User buyer = DataStore.findUserById(b.userId);
                User owner = (car != null) ? DataStore.findUserById(car.ownerId) : null;
                
                // SELLER FILTER: Only show bookings for cars they own
                if (currentUser.role.equals("seller") && (car == null || !car.ownerId.equals(currentUser.id))) {
                    continue; 
                }

                String carName = (car != null) ? car.name + " (" + car.model + ")" : "Unknown Car";
                String buyerName = (buyer != null) ? buyer.username : "Unknown Buyer";
                String buyerContact = (buyer != null) ? buyer.contact : "N/A";
                String ownerContact = (owner != null) ? owner.contact : "N/A";
                
                Object[] rowData;
                // Note: We use car.id for the table column data, which is index 1.
                if (currentUser.role.equals("admin")) {
                    rowData = new Object[]{
                        b.id, car.id, b.status, buyerName, ownerContact, b.pickupDate, b.pickupTime, b.days, "₹" + UIUtils.MONEY.format(b.totalPrice)
                    };
                } else { // Seller
                    rowData = new Object[]{
                        b.id, car.id, b.status, buyerName, buyerContact, b.pickupDate, b.pickupTime, b.days, "₹" + UIUtils.MONEY.format(b.totalPrice)
                    };
                }
                model.addRow(rowData);
            }
            updateControlButtons();
            model.fireTableDataChanged();
            revalidate();
            repaint();
        }
    }


    public static class WrapLayout extends FlowLayout {
        public WrapLayout(){super();}
        public WrapLayout(int align,int hgap,int vgap){super(align,hgap,vgap);}
        public Dimension preferredLayoutSize(Container target){
            return layoutSize(target,true);
        }
        public Dimension minimumLayoutSize(Container target){return layoutSize(target,false);}
        private Dimension layoutSize(Container target, boolean preferred){
            synchronized(target.getTreeLock()){
                int w = target.getWidth();
                if(w==0) w=Integer.MAX_VALUE;
                Insets insets = target.getInsets();
                int maxWidth = w - insets.left - insets.right - 10;
                int x = 0, y = insets.top, rowHeight = 0;

                int hgap = getHgap();
                int vgap = getVgap();

                for(Component c : target.getComponents()){
                    if(!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if(x + d.width > maxWidth){
                        x = 0;
                        y += rowHeight + vgap;
                        rowHeight = 0;
                    }
                    x += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                y += rowHeight + insets.bottom;
                return new Dimension(w, y);
            }
        }
    }

    public static void main(String[] args){
        UIUtils.ensureImagesFolder();
        SwingUtilities.invokeLater(()->new MainFrame().setVisible(true));
    }
}
