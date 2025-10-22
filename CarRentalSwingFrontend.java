import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class CarRentalUI extends JFrame {

    private JTextField txtName, txtPrice, txtStatus;
    private JLabel lblImage;
    private JTable table;
    private DefaultTableModel model;
    private String selectedImagePath = null;

    public CarRentalUI() {
        setTitle("🚗 Car Rental & Dealership System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ------------------- HEADER -------------------
        JLabel title = new JLabel("Car Rental & Dealership System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(new Color(52, 152, 219));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(15, 0, 15, 0));
        add(title, BorderLayout.NORTH);

        // ------------------- INPUT PANEL -------------------
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        inputPanel.setBackground(new Color(245, 247, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblName = new JLabel("Car Name:");
        JLabel lblPrice = new JLabel("Price per Day:");
        JLabel lblStatus = new JLabel("Status:");
        JLabel lblImg = new JLabel("Car Image:");

        txtName = new JTextField();
        txtPrice = new JTextField();
        txtStatus = new JTextField("Available");

        lblImage = new JLabel("No Image Selected", SwingConstants.CENTER);
        lblImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblImage.setPreferredSize(new Dimension(150, 100));

        JButton btnBrowse = new JButton("Choose Image");
        btnBrowse.addActionListener(e -> chooseImage());

        JButton btnAdd = new JButton("Add Car");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");

        // Row 1
        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(lblName, gbc);
        gbc.gridx = 1; inputPanel.add(txtName, gbc);
        // Row 2
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(lblPrice, gbc);
        gbc.gridx = 1; inputPanel.add(txtPrice, gbc);
        // Row 3
        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(lblStatus, gbc);
        gbc.gridx = 1; inputPanel.add(txtStatus, gbc);
        // Row 4
        gbc.gridx = 0; gbc.gridy = 3; inputPanel.add(lblImg, gbc);
        gbc.gridx = 1; inputPanel.add(lblImage, gbc);
        gbc.gridx = 2; inputPanel.add(btnBrowse, gbc);
        // Row 5
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd); btnPanel.add(btnUpdate); btnPanel.add(btnDelete); btnPanel.add(btnClear);
        inputPanel.add(btnPanel, gbc);

        add(inputPanel, BorderLayout.WEST);

        // ------------------- TABLE -------------------
        String[] cols = {"Car Name", "Price per Day", "Status", "Image Path"};
        model = new DefaultTableModel(cols, 0);
        table = new JTable(model);
        table.setRowHeight(25);
        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        // ------------------- BUTTON ACTIONS -------------------
        btnAdd.addActionListener(e -> addCar());
        btnUpdate.addActionListener(e -> updateCar());
        btnDelete.addActionListener(e -> deleteCar());
        btnClear.addActionListener(e -> clearFields());

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int i = table.getSelectedRow();
                txtName.setText(model.getValueAt(i, 0).toString());
                txtPrice.setText(model.getValueAt(i, 1).toString());
                txtStatus.setText(model.getValueAt(i, 2).toString());
                selectedImagePath = model.getValueAt(i, 3).toString();
                lblImage.setText(new File(selectedImagePath).getName());
            }
        });
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImagePath = chooser.getSelectedFile().getAbsolutePath();
            lblImage.setText(chooser.getSelectedFile().getName());
        }
    }

    private void addCar() {
        if (txtName.getText().isEmpty() || txtPrice.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all details!");
            return;
        }
        model.addRow(new Object[]{txtName.getText(), txtPrice.getText(), txtStatus.getText(), selectedImagePath});
        clearFields();
    }

    private void updateCar() {
        int i = table.getSelectedRow();
        if (i >= 0) {
            model.setValueAt(txtName.getText(), i, 0);
            model.setValueAt(txtPrice.getText(), i, 1);
            model.setValueAt(txtStatus.getText(), i, 2);
            model.setValueAt(selectedImagePath, i, 3);
        } else {
            JOptionPane.showMessageDialog(this, "Select a car to update!");
        }
    }

    private void deleteCar() {
        int i = table.getSelectedRow();
        if (i >= 0) {
            model.removeRow(i);
        } else {
            JOptionPane.showMessageDialog(this, "Select a car to delete!");
        }
    }

    private void clearFields() {
        txtName.setText("");
        txtPrice.setText("");
        txtStatus.setText("Available");
        lblImage.setText("No Image Selected");
        selectedImagePath = null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CarRentalUI().setVisible(true));
    }
}
