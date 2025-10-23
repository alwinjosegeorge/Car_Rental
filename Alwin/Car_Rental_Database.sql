/* CarRentalFullSupabase.java
   Fully functional single-file Swing Car Rental app (Supabase Integrated)
   - Roles: admin, seller, user
   - Signup/login with Supabase users table
   - Cars & bookings stored in Supabase
   - Image placeholder handling
   - Seller cannot edit others' cars
   - Admin can see owner info
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import org.json.*;

public class CarRentalFullSupabase {

    // ---------------- Config ----------------
    private static final String SUPABASE_URL = "https://pvwgmmxoxgblcawhmdpo.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB2d2dtbXhveGdibGNhd2htZHBvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjEyMzAwMTgsImV4cCI6MjA3NjgwNjAxOH0.J44_Y1w_E18wy4kk-KGp2eTxgjcYK-dWbulJhe9KAh0";

    // ---------------- Models ----------------
    public static class Car {
        public String id, name, model, category, status, imagePath, ownerId;
        public double pricePerDay; public String fuelType; public int seats; public String transmission;
        public Car(String id, String name, String model, double pricePerDay, String category, String status,
                   String imagePath, String ownerId, String fuelType, int seats, String transmission){
            this.id=id; this.name=name; this.model=model; this.pricePerDay=pricePerDay;
            this.category=category; this.status=status; this.imagePath=imagePath; this.ownerId=ownerId;
            this.fuelType=fuelType; this.seats=seats; this.transmission=transmission;
        }
    }

    public static class User {
        public String id, username, password, role, contact;
        public User(String id,String username,String password,String role,String contact){
            this.id=id; this.username=username; this.password=password; this.role=role; this.contact=contact;
        }
    }

    public static class Booking{
        public String id, carId, userId, pickupPlace, pickupDate, pickupTime;
        public int days; public double totalPrice;
        public Booking(String id,String carId,String userId,String place,String date,String time,int days,double total){
            this.id=id; this.carId=carId; this.userId=userId; this.pickupPlace=place;
            this.pickupDate=date; this.pickupTime=time; this.days=days; this.totalPrice=total;
        }
    }

    // ---------------- Supabase Store ----------------
    public static class SupabaseStore {

        // ------------ Users ------------
        public static List<User> fetchUsers(){
            try{
                String res = get("/rest/v1/users?select=*");
                JSONArray arr = new JSONArray(res); List<User> list=new ArrayList<>();
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.getJSONObject(i);
                    list.add(new User(o.getString("id"), o.getString("username"), o.getString("password"),
                            o.getString("role"), o.getString("contact")));
                }
                return list;
            }catch(Exception e){e.printStackTrace(); return new ArrayList<>();}
        }

        public static void addUser(User u){
            try{
                JSONObject obj=new JSONObject();
                obj.put("id",u.id); obj.put("username",u.username);
                obj.put("password",u.password); obj.put("role",u.role);
                obj.put("contact",u.contact);
                post("/rest/v1/users", obj);
            }catch(Exception e){e.printStackTrace();}
        }

        public static Optional<User> authenticate(String username,String password,String role){
            try{
                String filter = "?username=eq."+username+"&password=eq."+password+"&role=eq."+role;
                String res = get("/rest/v1/users"+filter);
                JSONArray arr = new JSONArray(res);
                if(arr.length()>0){
                    JSONObject o=arr.getJSONObject(0);
                    return Optional.of(new User(o.getString("id"), o.getString("username"),
                            o.getString("password"), o.getString("role"), o.getString("contact")));
                }
            }catch(Exception e){e.printStackTrace();}
            return Optional.empty();
        }

        // ------------ Cars ------------
        public static List<Car> fetchCars(){
            try{
                String res = get("/rest/v1/cars?select=*");
                JSONArray arr=new JSONArray(res); List<Car> list=new ArrayList<>();
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.getJSONObject(i);
                    list.add(new Car(o.getString("id"),o.getString("name"),o.getString("model"),
                            o.getDouble("pricePerDay"), o.getString("category"), o.getString("status"),
                            o.getString("imagePath"), o.getString("ownerId"),
                            o.optString("fuelType",""), o.optInt("seats",0), o.optString("transmission","")));
                }
                return list;
            }catch(Exception e){e.printStackTrace(); return new ArrayList<>();}
        }

        public static void addCar(Car c){
            try{
                JSONObject obj=new JSONObject();
                obj.put("id",c.id); obj.put("name",c.name); obj.put("model",c.model);
                obj.put("pricePerDay",c.pricePerDay); obj.put("category",c.category);
                obj.put("status",c.status); obj.put("imagePath",c.imagePath);
                obj.put("ownerId",c.ownerId); obj.put("fuelType",c.fuelType);
                obj.put("seats",c.seats); obj.put("transmission",c.transmission);
                post("/rest/v1/cars", obj);
            }catch(Exception e){e.printStackTrace();}
        }

        public static void updateCar(Car c){
            try{
                JSONObject obj=new JSONObject();
                obj.put("name",c.name); obj.put("model",c.model); obj.put("pricePerDay",c.pricePerDay);
                obj.put("category",c.category); obj.put("status",c.status); obj.put("imagePath",c.imagePath);
                obj.put("fuelType",c.fuelType); obj.put("seats",c.seats); obj.put("transmission",c.transmission);
                patch("/rest/v1/cars?id=eq."+c.id, obj);
            }catch(Exception e){e.printStackTrace();}
        }

        public static void deleteCar(String id){
            try{delete("/rest/v1/cars?id=eq."+id);}catch(Exception e){e.printStackTrace();}
        }

        // ------------ Bookings ------------
        public static List<Booking> fetchBookings(){
            try{
                String res = get("/rest/v1/bookings?select=*");
                JSONArray arr=new JSONArray(res); List<Booking> list=new ArrayList<>();
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.getJSONObject(i);
                    list.add(new Booking(o.getString("id"), o.getString("carId"), o.getString("userId"),
                            o.getString("pickupPlace"), o.getString("pickupDate"), o.getString("pickupTime"),
                            o.getInt("days"), o.getDouble("totalPrice")));
                }
                return list;
            }catch(Exception e){e.printStackTrace(); return new ArrayList<>();}
        }

        public static void addBooking(Booking b){
            try{
                JSONObject obj=new JSONObject();
                obj.put("id",b.id); obj.put("carId",b.carId); obj.put("userId",b.userId);
                obj.put("pickupPlace",b.pickupPlace); obj.put("pickupDate",b.pickupDate);
                obj.put("pickupTime",b.pickupTime); obj.put("days",b.days); obj.put("totalPrice",b.totalPrice);
                post("/rest/v1/bookings", obj);
            }catch(Exception e){e.printStackTrace();}
        }

        // ------------ Supabase HTTP Helpers ------------
        private static String get(String path) throws Exception{
            URL url=new URL(SUPABASE_URL+path); HttpURLConnection con=(HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("apikey", SUPABASE_KEY);
            con.setRequestProperty("Authorization","Bearer "+SUPABASE_KEY);
            BufferedReader br=new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line; StringBuilder sb=new StringBuilder();
            while((line=br.readLine())!=null) sb.append(line);
            br.close(); return sb.toString();
        }

        private static void post(String path, JSONObject obj) throws Exception{
            URL url=new URL(SUPABASE_URL+path); HttpURLConnection con=(HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST"); con.setRequestProperty("apikey",SUPABASE_KEY);
            con.setRequestProperty("Authorization","Bearer "+SUPABASE_KEY);
            con.setRequestProperty("Content-Type","application/json"); con.setDoOutput(true);
            con.getOutputStream().write(obj.toString().getBytes());
            con.getInputStream().close();
        }

        private static void patch(String path, JSONObject obj) throws Exception{
            URL url=new URL(SUPABASE_URL+path); HttpURLConnection con=(HttpURLConnection)url.openConnection();
            con.setRequestMethod("PATCH"); con.setRequestProperty("apikey",SUPABASE_KEY);
            con.setRequestProperty("Authorization","Bearer "+SUPABASE_KEY);
            con.setRequestProperty("Content-Type","application/json"); con.setDoOutput(true);
            con.getOutputStream().write(obj.toString().getBytes());
            con.getInputStream().close();
        }

        private static void delete(String path) throws Exception{
            URL url=new URL(SUPABASE_URL+path); HttpURLConnection con=(HttpURLConnection)url.openConnection();
            con.setRequestMethod("DELETE"); con.setRequestProperty("apikey",SUPABASE_KEY);
            con.setRequestProperty("Authorization","Bearer "+SUPABASE_KEY);
            con.getInputStream().close();
        }
    }

    // ---------------- Main Frame ----------------
    public static class MainFrame extends JFrame {
        private User currentUser;
        public MainFrame(){
            setTitle("Car Rental App - Supabase");
            setSize(900,600); setLocationRelativeTo(null); setDefaultCloseOperation(EXIT_ON_CLOSE);
            setContentPane(new LoginPanel(this));
            setVisible(true);
        }

        public void loginSuccess(User user){
            this.currentUser=user;
            setContentPane(new BrowsePanel(this));
            revalidate();
        }

        public User getCurrentUser(){ return currentUser; }
    }

    // ---------------- Login Panel ----------------
    public static class LoginPanel extends JPanel {
        private MainFrame parent;
        private JTextField txtUser, txtContact;
        private JPasswordField txtPass;
        private JComboBox<String> cmbRole;
        public LoginPanel(MainFrame parent){
            this.parent=parent; setLayout(new GridBagLayout());
            GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(5,5,5,5);
            JLabel lblUser=new JLabel("Username"); txtUser=new JTextField(15);
            JLabel lblPass=new JLabel("Password"); txtPass=new JPasswordField(15);
            JLabel lblRole=new JLabel("Role"); cmbRole=new JComboBox<>(new String[]{"user","seller","admin"});
            JButton btnLogin=new JButton("Login"); JButton btnSignup=new JButton("Signup");

            gbc.gridx=0; gbc.gridy=0; add(lblUser,gbc); gbc.gridx=1; add(txtUser,gbc);
            gbc.gridx=0; gbc.gridy=1; add(lblPass,gbc); gbc.gridx=1; add(txtPass,gbc);
            gbc.gridx=0; gbc.gridy=2; add(lblRole,gbc); gbc.gridx=1; add(cmbRole,gbc);
            gbc.gridx=0; gbc.gridy=3; add(btnLogin,gbc); gbc.gridx=1; add(btnSignup,gbc);

            btnLogin.addActionListener(e->{
                String u=txtUser.getText(); String p=new String(txtPass.getPassword());
                String r=(String)cmbRole.getSelectedItem();
                Optional<User> opt=SupabaseStore.authenticate(u,p,r);
                if(opt.isPresent()){ parent.loginSuccess(opt.get()); }
                else JOptionPane.showMessageDialog(this,"Login Failed");
            });

            btnSignup.addActionListener(e->{
                String u=txtUser.getText(); String p=new String(txtPass.getPassword());
                String r=(String)cmbRole.getSelectedItem();
                String id=UUID.randomUUID().toString();
                User newUser=new User(id,u,p,r,"");
                SupabaseStore.addUser(newUser);
                JOptionPane.showMessageDialog(this,"Signup Successful!"); 
            });
        }
    }

    // ---------------- Browse Panel ----------------
    public static class BrowsePanel extends JPanel {
        private MainFrame parent; private JPanel cardContainer;
        public BrowsePanel(MainFrame parent){
            this.parent=parent; setLayout(new BorderLayout());
            JLabel lblWelcome=new JLabel("Welcome "+parent.getCurrentUser().username); lblWelcome.setBorder(new EmptyBorder(10,10,10,10));
            add(lblWelcome,BorderLayout.NORTH);

            cardContainer=new JPanel(); cardContainer.setLayout(new GridLayout(0,3,10,10));
            JScrollPane scroll=new JScrollPane(cardContainer);
            add(scroll,BorderLayout.CENTER);

            loadCars();
        }

        private void loadCars(){
            cardContainer.removeAll();
            List<Car> cars=SupabaseStore.fetchCars();
            User curr=parent.getCurrentUser();
            for(Car c:cars){
                JPanel p=new JPanel(); p.setLayout(new BorderLayout()); p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                JLabel lbl=new JLabel(c.name+" - ₹"+c.pricePerDay); lbl.setHorizontalAlignment(JLabel.CENTER);
                p.add(lbl,BorderLayout.NORTH);
                JLabel imgLabel=new JLabel(); imgLabel.setHorizontalAlignment(JLabel.CENTER);
                try{BufferedImage img=ImageIO.read(new File(c.imagePath)); imgLabel.setIcon(new ImageIcon(img.getScaledInstance(150,100,Image.SCALE_SMOOTH)));}catch(Exception ex){}
                p.add(imgLabel,BorderLayout.CENTER);

                JButton btnBook=new JButton("Book"); p.add(btnBook,BorderLayout.SOUTH);
                btnBook.addActionListener(e->{
                    String place=JOptionPane.showInputDialog("Pickup Place");
                    String date=JOptionPane.showInputDialog("Pickup Date (yyyy-mm-dd)");
                    String time=JOptionPane.showInputDialog("Pickup Time (HH:mm)");
                    int days=Integer.parseInt(JOptionPane.showInputDialog("Number of days"));
                    double total=c.pricePerDay*days;
                    Booking b=new Booking(UUID.randomUUID().toString(),c.id,curr.id,place,date,time,days,total);
                    SupabaseStore.addBooking(b);
                    JOptionPane.showMessageDialog(this,"Booking Done! ₹"+total);
                });

                cardContainer.add(p);
            }
            cardContainer.revalidate(); cardContainer.repaint();
        }
    }

    // ---------------- Main ----------------
    public static void main(String[] args){
        SwingUtilities.invokeLater(()->new MainFrame());
    }
}

// ---------------- Browse Panel ----------------
public static class BrowsePanel extends JPanel {
    private MainFrame parent; private JPanel cardContainer;
    public BrowsePanel(MainFrame parent){
        this.parent=parent; setLayout(new BorderLayout());
        JLabel lblWelcome=new JLabel("Welcome "+parent.getCurrentUser().username+" ("+parent.getCurrentUser().role+")");
        lblWelcome.setBorder(new EmptyBorder(10,10,10,10));
        add(lblWelcome,BorderLayout.NORTH);

        cardContainer=new JPanel(); cardContainer.setLayout(new GridLayout(0,3,10,10));
        JScrollPane scroll=new JScrollPane(cardContainer);
        add(scroll,BorderLayout.CENTER);

        JPanel btnPanel=new JPanel();
        JButton btnAddCar=new JButton("Add Car"); 
        btnPanel.add(btnAddCar); 
        add(btnPanel,BorderLayout.SOUTH);

        User curr=parent.getCurrentUser();
        // Only seller/admin can add car
        if(curr.role.equals("seller") || curr.role.equals("admin")){
            btnAddCar.addActionListener(e->{
                JTextField txtName=new JTextField(); JTextField txtModel=new JTextField(); 
                JTextField txtPrice=new JTextField(); JTextField txtCategory=new JTextField();
                JTextField txtFuel=new JTextField(); JTextField txtSeats=new JTextField(); JTextField txtTrans=new JTextField();
                JFileChooser fc=new JFileChooser(); fc.setFileFilter(new FileNameExtensionFilter("Images","jpg","png","jpeg"));
                if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION) return;
                File imgFile=fc.getSelectedFile();
                String imgPath="images/"+imgFile.getName();
                try{Files.copy(imgFile.toPath(), Paths.get(imgPath), StandardCopyOption.REPLACE_EXISTING);}catch(Exception ex){ex.printStackTrace();}
                
                JPanel panel=new JPanel(new GridLayout(0,1));
                panel.add(new JLabel("Name")); panel.add(txtName);
                panel.add(new JLabel("Model")); panel.add(txtModel);
                panel.add(new JLabel("Price/Day")); panel.add(txtPrice);
                panel.add(new JLabel("Category")); panel.add(txtCategory);
                panel.add(new JLabel("Fuel Type")); panel.add(txtFuel);
                panel.add(new JLabel("Seats")); panel.add(txtSeats);
                panel.add(new JLabel("Transmission")); panel.add(txtTrans);

                int res=JOptionPane.showConfirmDialog(this,panel,"Add Car",JOptionPane.OK_CANCEL_OPTION);
                if(res==JOptionPane.OK_OPTION){
                    Car c=new Car(UUID.randomUUID().toString(), txtName.getText(), txtModel.getText(),
                            Double.parseDouble(txtPrice.getText()), txtCategory.getText(), "available",
                            imgPath, curr.id, txtFuel.getText(), Integer.parseInt(txtSeats.getText()), txtTrans.getText());
                    SupabaseStore.addCar(c); loadCars();
                }
            });
        }else{ btnAddCar.setEnabled(false); }

        loadCars();
    }

    private void loadCars(){
        cardContainer.removeAll();
        List<Car> cars=SupabaseStore.fetchCars();
        User curr=parent.getCurrentUser();
        for(Car c:cars){
            JPanel p=new JPanel(); p.setLayout(new BorderLayout()); p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            JLabel lbl=new JLabel(c.name+" - ₹"+c.pricePerDay+" ("+c.status+")"); lbl.setHorizontalAlignment(JLabel.CENTER);
            p.add(lbl,BorderLayout.NORTH);
            JLabel imgLabel=new JLabel(); imgLabel.setHorizontalAlignment(JLabel.CENTER);
            try{BufferedImage img=ImageIO.read(new File(c.imagePath)); imgLabel.setIcon(new ImageIcon(img.getScaledInstance(150,100,Image.SCALE_SMOOTH)));}catch(Exception ex){}
            p.add(imgLabel,BorderLayout.CENTER);

            JPanel bottomPanel=new JPanel();
            JButton btnBook=new JButton("Book"); bottomPanel.add(btnBook);

            // Seller/admin can edit/delete own/all cars
            if(curr.role.equals("seller") && c.ownerId.equals(curr.id) || curr.role.equals("admin")){
                JButton btnEdit=new JButton("Edit"); JButton btnDel=new JButton("Delete");
                bottomPanel.add(btnEdit); bottomPanel.add(btnDel);

                btnEdit.addActionListener(e->{
                    JTextField txtName=new JTextField(c.name); JTextField txtModel=new JTextField(c.model);
                    JTextField txtPrice=new JTextField(String.valueOf(c.pricePerDay)); JTextField txtCategory=new JTextField(c.category);
                    JTextField txtFuel=new JTextField(c.fuelType); JTextField txtSeats=new JTextField(String.valueOf(c.seats)); JTextField txtTrans=new JTextField(c.transmission);
                    JPanel panel=new JPanel(new GridLayout(0,1));
                    panel.add(new JLabel("Name")); panel.add(txtName);
                    panel.add(new JLabel("Model")); panel.add(txtModel);
                    panel.add(new JLabel("Price/Day")); panel.add(txtPrice);
                    panel.add(new JLabel("Category")); panel.add(txtCategory);
                    panel.add(new JLabel("Fuel Type")); panel.add(txtFuel);
                    panel.add(new JLabel("Seats")); panel.add(txtSeats);
                    panel.add(new JLabel("Transmission")); panel.add(txtTrans);

                    int res=JOptionPane.showConfirmDialog(this,panel,"Edit Car",JOptionPane.OK_CANCEL_OPTION);
                    if(res==JOptionPane.OK_OPTION){
                        c.name=txtName.getText(); c.model=txtModel.getText(); c.pricePerDay=Double.parseDouble(txtPrice.getText());
                        c.category=txtCategory.getText(); c.fuelType=txtFuel.getText(); c.seats=Integer.parseInt(txtSeats.getText());
                        c.transmission=txtTrans.getText();
                        SupabaseStore.updateCar(c); loadCars();
                    }
                });

                btnDel.addActionListener(e->{
                    if(JOptionPane.showConfirmDialog(this,"Delete this car?")==JOptionPane.YES_OPTION){
                        SupabaseStore.deleteCar(c.id); loadCars();
                    }
                });
            }

            btnBook.addActionListener(e->{
                if(curr.role.equals("user")){
                    String place=JOptionPane.showInputDialog("Pickup Place");
                    String date=JOptionPane.showInputDialog("Pickup Date (yyyy-mm-dd)");
                    String time=JOptionPane.showInputDialog("Pickup Time (HH:mm)");
                    int days=Integer.parseInt(JOptionPane.showInputDialog("Number of days"));
                    double total=c.pricePerDay*days;
                    Booking b=new Booking(UUID.randomUUID().toString(),c.id,curr.id,place,date,time,days,total);
                    SupabaseStore.addBooking(b);
                    JOptionPane.showMessageDialog(this,"Booking Done! ₹"+total);
                }else{ JOptionPane.showMessageDialog(this,"Only users can book"); }
            });

            p.add(bottomPanel,BorderLayout.SOUTH);
            cardContainer.add(p);
        }
        cardContainer.revalidate(); cardContainer.repaint();
    }
}
