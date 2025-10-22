/*
 CarRentalMultiRole.java
 Single-file Swing demo app (Java 17+)
 - Login with roles: admin, user, seller
 - Seller can add/update/delete cars and upload images
 - Uploaded images are copied into ./images/ and shown immediately
 - Users can browse cars and view image previews
 - Admin can view all users and all cars
 This is a simple in-memory demo (no DB). Designed to be readable and "soft-coded".
*/

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class CarRentalMultiRole {

    // ----------------------- Models -----------------------
    public static class Car {
        public String id;
        public String name;
        public double pricePerDay;
        public String status; // Available, Rented, Sold
        public String imagePath; // relative path under images/

        public Car(String id, String name, double pricePerDay, String status, String imagePath) {
            this.id = id;
            this.name = name;
            this.pricePerDay = pricePerDay;
            this.status = status;
            this.imagePath = imagePath;
        }
    }

    public static class User {
        public String id;
        public String username;
        public String password; // demo only (plaintext)
        public String role; // admin, user, seller

        public User(String id, String username, String password, String role) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }

    // ----------------------- Services (in-memory) -----------------------
    public static class DataStore {
        private static final List<User> users = new ArrayList<>();
        private static final List<Car> cars = new ArrayList<>();

        static {
            // demo users (username, password)
            users.add(new User("U001", "admin@demo", "admin123", "admin"));
            users.add(new User("U002", "user@demo", "user123", "user"));
            users.add(new User("U003", "seller@demo", "seller123", "seller"));

            // sample cars (with no images initially)
            cars.add(new Car("C001", "Toyota Camry", 3500.0, "Available", null));
            cars.add(new Car("C002", "Honda Civic", 3200.0, "Available", null));
            cars.add(new Car("C003", "Ford Mustang", 7000.0, "Sold", null));
        }

        public static Optional<User> authenticate(String username, String password, String role) {
            return users.stream()
                    .filter(u -> u.username.equalsIgnoreCase(username) && u.password.equals(password) && u.role.equalsIgnoreCase(role))
                    .findFirst();
        }

        public static List<User> fetchUsers() { return new ArrayList<>(users); }
        public static List<Car> fetchCars() { return new ArrayList<>(cars); }

        public static void addCar(Car c) { cars.add(c); }
        public static void updateCar(Car updated) {
            for (int i = 0; i < cars.size(); i++) {
                if (cars.get(i).id.equals(updated.id)) {
                    cars.set(i, updated);
                    return;
                }
            }
        }
        public static void deleteCar(String id) { cars.removeIf(c -> c.id.equals(id)); }

        public static String nextCarId() {
            int max = cars.stream().mapToInt(c -> {
                try { return Integer.parseInt(c.id.replaceAll("\\D+", "")); }
                catch (Exception e) { return 0; }
            }).max().orElse(0);
            return String.format("C%03d", max + 1);
        }
    }

    // ----------------------- Helpers -----------------------
    public static class UIUtils {
        public static final DecimalFormat MONEY = new DecimalFormat("#,###.##");

        // create images folder if not exists
        public static void ensureImagesFolder() {
            try {
                Files.createDirectories(Paths.get("images"));
            } catch (IOException e) {
                System.err.println("Unable to create images folder: " + e.getMessage());
            }
        }

        // copy selected image file into images/ folder and return relative path
        public static String copyImageToStore(File sourceFile, String targetFileName) throws IOException {
            ensureImagesFolder();
            Path target = Paths.get("images", targetFileName);
            Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace("\\", "/");
        }

        // load scaled ImageIcon (thumbnail)
        public static ImageIcon loadScaledIcon(String imagePath, int w, int h) {
            if (imagePath == null) return null;
            try {
                BufferedImage img = ImageIO.read(new File(imagePath));
                Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            } catch (IOException e) {
                return null;
            }
        }
    }

    // ----------------------- Main Frame -----------------------
    public static class MainFrame extends JFrame {
        private CardLayout cards = new CardLayout();
        private JPanel cardPanel = new JPanel(cards);

        private LoginPanel loginPanel;
        private DashboardPanel dashboardPanel;

        private User currentUser;

        public MainFrame() {
            setTitle("Car Rental & Dealership");
            setSize(1000, 650);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            loginPanel = new LoginPanel(this);
            dashboardPanel = new DashboardPanel(this);

            cardPanel.add(loginPanel, "login");
            cardPanel.add(dashboardPanel, "dashboard");

            add(cardPanel);
            showLogin();
        }

        public void showLogin() { cards.show(cardPanel, "login"); }
        public void showDashboard(User user) {
            this.currentUser = user;
            dashboardPanel.setUser(user);
            cards.show(cardPanel, "dashboard");
        }
        public User getCurrentUser() { return currentUser; }
    }

    // ----------------------- Login Panel -----------------------
    public static class LoginPanel extends JPanel {
        private MainFrame parent;
        private JTextField txtUser;
        private JPasswordField txtPass;
        private JComboBox<String> roleBox;

        public LoginPanel(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBackground(new Color(250, 250, 250));

            JLabel title = new JLabel("Car Rental & Dealership", SwingConstants.CENTER);
            title.setOpaque(true);
            title.setBackground(new Color(34, 134, 230));
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 22));
            title.setBorder(new EmptyBorder(20, 10, 20, 10));
            add(title, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridBagLayout());
            center.setBackground(new Color(250, 250, 250));
            center.setBorder(new EmptyBorder(20, 60, 20, 60));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0; center.add(new JLabel("Username:"), gbc);
            gbc.gridx = 1; txtUser = new JTextField(20); center.add(txtUser, gbc);

            gbc.gridx = 0; gbc.gridy = 1; center.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1; txtPass = new JPasswordField(20); center.add(txtPass, gbc);

            gbc.gridx = 0; gbc.gridy = 2; center.add(new JLabel("Role:"), gbc);
            gbc.gridx = 1; roleBox = new JComboBox<>(new String[]{"user", "seller", "admin"}); center.add(roleBox, gbc);

            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            JButton btnLogin = new JButton("Login");
            btnLogin.addActionListener(e -> doLogin());
            JPanel pbtn = new JPanel();
            pbtn.add(btnLogin);
            center.add(pbtn, gbc);

            add(center, BorderLayout.CENTER);

            JLabel hint = new JLabel("<html><small>Demo users: admin@demo/admin123 (admin) | user@demo/user123 (user) | seller@demo/seller123 (seller)</small></html>", SwingConstants.CENTER);
            hint.setBorder(new EmptyBorder(10,10,10,10));
            add(hint, BorderLayout.SOUTH);
        }

        private void doLogin() {
            String u = txtUser.getText().trim();
            String p = new String(txtPass.getPassword());
            String role = (String) roleBox.getSelectedItem();

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username and password.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Optional<User> found = DataStore.authenticate(u, p, role);
            if (found.isPresent()) {
                JOptionPane.showMessageDialog(this, "Welcome, " + found.get().username + " (" + role + ")");
                parent.showDashboard(found.get());
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials or role.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------------- Dashboard Panel -----------------------
    public static class DashboardPanel extends JPanel {
        private MainFrame parent;
        private User user;
        private JTabbedPane tabs;
        private SellerPanel sellerPanel;
        private BrowsePanel browsePanel;
        private AdminPanel adminPanel;

        public DashboardPanel(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());

            JPanel top = new JPanel(new BorderLayout());
            top.setBorder(new EmptyBorder(8, 10, 8, 10));
            JLabel lbl = new JLabel("Dashboard", SwingConstants.LEFT);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
            top.add(lbl, BorderLayout.WEST);

            JButton logout = new JButton("Logout");
            logout.addActionListener(e -> parent.showLogin());
            top.add(logout, BorderLayout.EAST);

            add(top, BorderLayout.NORTH);

            tabs = new JTabbedPane();
            sellerPanel = new SellerPanel(parent);
            browsePanel = new BrowsePanel(parent);
            adminPanel = new AdminPanel(parent);

            add(tabs, BorderLayout.CENTER);
        }

        public void setUser(User user) {
            this.user = user;
            tabs.removeAll();
            // common tab: Browse
            tabs.add("Browse Cars", browsePanel);
            // role-based
            switch (user.role.toLowerCase()) {
                case "seller":
                    tabs.add("Manage My Cars", sellerPanel);
                    break;
                case "admin":
                    tabs.add("Manage Cars", sellerPanel); // admin also can manage
                    tabs.add("Users", adminPanel);
                    break;
                case "user":
                default:
                    // only browse
                    break;
            }
            // refresh panels each time
            browsePanel.refresh();
            sellerPanel.refresh();
            adminPanel.refresh();
        }
    }

    // ----------------------- Browse Panel (for Users & everyone) -----------------------
    public static class BrowsePanel extends JPanel {
        private MainFrame parent;
        private JTable table;
        private DefaultTableModel model;
        private JLabel previewLabel;
        private JTextArea detailsArea;

        public BrowsePanel(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 10, 10, 10));

            // Table with custom model (holds image icon object in hidden column)
            String[] cols = {"Image", "ID", "Name", "Price/day", "Status", "ImagePath"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            table = new JTable(model) {
                // show icon in first column
                @Override public Class<?> getColumnClass(int column) {
                    if (column == 0) return ImageIcon.class;
                    return Object.class;
                }
            };
            table.setRowHeight(70);
            TableColumnModel cm = table.getColumnModel();
            cm.getColumn(0).setPreferredWidth(90);
            cm.getColumn(1).setPreferredWidth(50);
            cm.getColumn(2).setPreferredWidth(300);
            cm.getColumn(5).setMinWidth(0); cm.getColumn(5).setMaxWidth(0); // hide imagePath column

            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel right = new JPanel(new BorderLayout());
            right.setPreferredSize(new Dimension(300, 0));

            previewLabel = new JLabel("Select a car to preview", SwingConstants.CENTER);
            previewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            previewLabel.setPreferredSize(new Dimension(280, 220));
            right.add(previewLabel, BorderLayout.NORTH);

            detailsArea = new JTextArea();
            detailsArea.setEditable(false);
            detailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            detailsArea.setBorder(new EmptyBorder(8,8,8,8));
            right.add(new JScrollPane(detailsArea), BorderLayout.CENTER);

            add(right, BorderLayout.EAST);

            // selection listener
            table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
                if (!e.getValueIsAdjusting()) {
                    int r = table.getSelectedRow();
                    if (r >= 0) {
                        String id = (String) model.getValueAt(r, 1);
                        String name = (String) model.getValueAt(r, 2);
                        String price = (String) model.getValueAt(r, 3);
                        String status = (String) model.getValueAt(r, 4);
                        String imgPath = (String) model.getValueAt(r, 5);
                        detailsArea.setText("ID: " + id + "\nName: " + name + "\nPrice/day: " + price + "\nStatus: " + status);
                        ImageIcon icon = UIUtils.loadScaledIcon(imgPath, 260, 200);
                        if (icon != null) previewLabel.setIcon(icon);
                        else {
                            previewLabel.setIcon(null);
                            previewLabel.setText("No image available");
                        }
                    }
                }
            });
        }

        public void refresh() {
            model.setRowCount(0);
            for (Car c : DataStore.fetchCars()) {
                ImageIcon thumb = UIUtils.loadScaledIcon(c.imagePath, 90, 70);
                String price = UIUtils.MONEY.format(c.pricePerDay);
                model.addRow(new Object[]{thumb, c.id, c.name, price, c.status, c.imagePath});
            }
        }
    }

    // ----------------------- Seller Panel (Add / Edit / Delete cars) -----------------------
    public static class SellerPanel extends JPanel {
        private MainFrame parent;
        private JTable table;
        private DefaultTableModel model;

        private JTextField txtId, txtName, txtPrice, txtStatus;
        private JLabel imgLabel;
        private JButton btnChooseImg, btnSave, btnClear, btnDelete;

        private String selectedImagePath = null; // path in images/ folder for current editing

        public SellerPanel(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10,10,10,10));

            // Left: form
            JPanel form = new JPanel(new GridBagLayout());
            form.setPreferredSize(new Dimension(360, 0));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx=0; gbc.gridy=0; form.add(new JLabel("ID:"), gbc);
            gbc.gridx=1; txtId = new JTextField(15); txtId.setEditable(false); form.add(txtId, gbc);

            gbc.gridx=0; gbc.gridy=1; form.add(new JLabel("Name:"), gbc);
            gbc.gridx=1; txtName = new JTextField(15); form.add(txtName, gbc);

            gbc.gridx=0; gbc.gridy=2; form.add(new JLabel("Price/day:"), gbc);
            gbc.gridx=1; txtPrice = new JTextField(15); form.add(txtPrice, gbc);

            gbc.gridx=0; gbc.gridy=3; form.add(new JLabel("Status:"), gbc);
            gbc.gridx=1; txtStatus = new JTextField(15); txtStatus.setText("Available"); form.add(txtStatus, gbc);

            gbc.gridx=0; gbc.gridy=4; form.add(new JLabel("Image Preview:"), gbc);
            gbc.gridx=1;
            imgLabel = new JLabel("No image", SwingConstants.CENTER);
            imgLabel.setPreferredSize(new Dimension(150,100));
            imgLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            form.add(imgLabel, gbc);

            gbc.gridx=1; gbc.gridy=5;
            btnChooseImg = new JButton("Choose Image");
            btnChooseImg.addActionListener(e -> chooseImage());
            form.add(btnChooseImg, gbc);

            gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=2;
            JPanel btns = new JPanel();
            btnSave = new JButton("Save/Add");
            btnClear = new JButton("Clear");
            btnDelete = new JButton("Delete Selected");
            btns.add(btnSave); btns.add(btnClear); btns.add(btnDelete);
            form.add(btns, gbc);

            add(form, BorderLayout.WEST);

            // Right: table
            String[] cols = {"Image", "ID", "Name", "Price/day", "Status", "ImagePath"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = new JTable(model) {
                @Override public Class<?> getColumnClass(int column) {
                    if (column == 0) return ImageIcon.class;
                    return Object.class;
                }
            };
            table.setRowHeight(64);
            TableColumnModel cm = table.getColumnModel();
            cm.getColumn(5).setMinWidth(0); cm.getColumn(5).setMaxWidth(0);

            add(new JScrollPane(table), BorderLayout.CENTER);

            // Actions
            btnSave.addActionListener(e -> saveCar());
            btnClear.addActionListener(e -> clearForm());
            btnDelete.addActionListener(e -> deleteSelected());

            // table selection -> populate form
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int r = table.getSelectedRow();
                    if (r >= 0) {
                        txtId.setText((String) model.getValueAt(r, 1));
                        txtName.setText((String) model.getValueAt(r, 2));
                        txtPrice.setText((String) model.getValueAt(r, 3));
                        txtStatus.setText((String) model.getValueAt(r, 4));
                        String imgPath = (String) model.getValueAt(r, 5);
                        selectedImagePath = imgPath;
                        ImageIcon ic = UIUtils.loadScaledIcon(imgPath, 150, 100);
                        if (ic != null) { imgLabel.setIcon(ic); imgLabel.setText(""); } else { imgLabel.setIcon(null); imgLabel.setText("No image"); }
                    }
                }
            });
        }

        public void refresh() {
            model.setRowCount(0);
            for (Car c : DataStore.fetchCars()) {
                ImageIcon thumb = UIUtils.loadScaledIcon(c.imagePath, 64, 64);
                String price = UIUtils.MONEY.format(c.pricePerDay);
                model.addRow(new Object[]{thumb, c.id, c.name, price, c.status, c.imagePath});
            }
        }

        private void chooseImage() {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));
            int ret = chooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                try {
                    String targetName = UUID.randomUUID().toString() + "_" + f.getName();
                    String copied = UIUtils.copyImageToStore(f, targetName);
                    selectedImagePath = copied;
                    ImageIcon ic = UIUtils.loadScaledIcon(copied, 150, 100);
                    imgLabel.setIcon(ic); imgLabel.setText("");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to copy image: " + ex.getMessage());
                }
            }
        }

        private void saveCar() {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String priceTxt = txtPrice.getText().trim();
            String status = txtStatus.getText().trim();

            if (name.isEmpty() || priceTxt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and price are required.");
                return;
            }

            double price;
            try { price = Double.parseDouble(priceTxt.replaceAll(",", "")); } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid price.");
                return;
            }

            if (id.isEmpty()) {
                // new car
                String nid = DataStore.nextCarId();
                Car c = new Car(nid, name, price, status.isEmpty() ? "Available" : status, selectedImagePath);
                DataStore.addCar(c);
                JOptionPane.showMessageDialog(this, "Car added: " + nid);
            } else {
                // update existing
                Car c = new Car(id, name, price, status.isEmpty() ? "Available" : status, selectedImagePath);
                DataStore.updateCar(c);
                JOptionPane.showMessageDialog(this, "Car updated: " + id);
            }
            refresh();
            clearForm();
        }

        private void deleteSelected() {
            int r = table.getSelectedRow();
            if (r >= 0) {
                String id = (String) model.getValueAt(r, 1);
                int choice = JOptionPane.showConfirmDialog(this, "Delete car " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    DataStore.deleteCar(id);
                    refresh();
                    clearForm();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Select a car to delete.");
            }
        }

        private void clearForm() {
            txtId.setText("");
            txtName.setText("");
            txtPrice.setText("");
            txtStatus.setText("Available");
            imgLabel.setIcon(null);
            imgLabel.setText("No image");
            selectedImagePath = null;
            table.clearSelection();
        }
    }

    // ----------------------- Admin Panel -----------------------
    public static class AdminPanel extends JPanel {
        private MainFrame parent;
        private JTable userTable;
        private DefaultTableModel userModel;

        public AdminPanel(MainFrame parent) {
            this.parent = parent;
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10,10,10,10));

            userModel = new DefaultTableModel(new Object[]{"ID", "Username", "Role"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            userTable = new JTable(userModel);
            add(new JScrollPane(userTable), BorderLayout.CENTER);
        }

        public void refresh() {
            userModel.setRowCount(0);
            for (User u : DataStore.fetchUsers()) {
                userModel.addRow(new Object[]{u.id, u.username, u.role});
            }
        }
    }

    // ----------------------- Main -----------------------
    public static void main(String[] args) {
        UIUtils.ensureImagesFolder(); // ensure folder exists before start

        SwingUtilities.invokeLater(() -> {
            try {
                // Use system look & feel for better native appearance
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MainFrame f = new MainFrame();
            f.setVisible(true);
        });
    }
}
