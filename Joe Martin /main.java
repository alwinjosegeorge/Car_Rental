/* CarRentalFull.java
   Fully functional single-file Swing Car Rental app (Enhanced)
   - Roles: admin, seller, user
   - Signup for user/seller
   - Booking dialog with pickup info & total price
   - Car features: fuelType, seats, transmission
   - Image placeholder handling
   - Auto-refresh after booking or edits
*/

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
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

    // ---------------- DataStore ----------------
    public static class DataStore {
        private static final List<User> users = new ArrayList<>();
        private static final List<Car> cars = new ArrayList<>();

        static {
            users.add(new User("U001","admin@demo","admin123","admin","Admin Office"));
            users.add(new User("U002","user@demo","user123","user","Customer"));
            users.add(new User("U003","seller@demo","seller123","seller","+91-9876543210"));

            cars.add(new Car("C001","Toyota Camry","Camry",3500,"Sedan","Available","images/Toyota_Camry.jpeg","U003","Petrol",5,"Automatic"));
            cars.add(new Car("C002","Honda Civic","Civic",3200,"Sedan","Available","images/Honda_Civic.jpeg","U003","Petrol",5,"Automatic"));
        }

        public static Optional<User> authenticate(String username,String password,String role){
            return users.stream().filter(u->u.username.equalsIgnoreCase(username)&&u.password.equals(password)&&u.role.equalsIgnoreCase(role)).findFirst();
        }

        public static boolean usernameExists(String username){
            return users.stream().anyMatch(u->u.username.equalsIgnoreCase(username));
        }

        public static List<User> fetchUsers(){return new ArrayList<>(users);}
        public static List<Car> fetchCars(){return new ArrayList<>(cars);}

        public static void addCar(Car c){cars.add(c);}
        public static void updateCar(Car updated){for(int i=0;i<cars.size();i++){if(cars.get(i).id.equals(updated.id)){cars.set(i,updated);return;}}}
        public static void deleteCar(String id){cars.removeIf(c->c.id.equals(id));}

        public static void addUser(User u){users.add(u);}

        public static String nextCarId(){
            int max = cars.stream().mapToInt(c->{try{return Integer.parseInt(c.id.replaceAll("\\D+",""));}catch(Exception e){return 0;}}).max().orElse(0);
            return String.format("C%03d",max+1);
        }

        public static String nextUserId(){
            int max = users.stream().mapToInt(u->{try{return Integer.parseInt(u.id.replaceAll("\\D+",""));}catch(Exception e){return 0;}}).max().orElse(0);
            return String.format("U%03d",max+1);
        }

        public static User findUserById(String id){return users.stream().filter(u->u.id.equals(id)).findFirst().orElse(null);}
        public static Car findCarById(String id){return cars.stream().filter(c->c.id.equals(id)).findFirst().orElse(null);}
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

        public static ImageIcon loadScaledIcon(String imagePath,int w,int h){
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

    // ---------------- Login Panel with Signup ----------------
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
            gbc.gridx=1; txtUser = new JTextField(22); txtUser.setToolTipText("e.g. seller@demo"); center.add(txtUser,gbc);

            gbc.gridx=0; gbc.gridy=1; center.add(new JLabel("Password:"),gbc);
            gbc.gridx=1; txtPass = new JPasswordField(22); center.add(txtPass,gbc);

            gbc.gridx=0; gbc.gridy=2; center.add(new JLabel("Role:"),gbc);
            gbc.gridx=1; roleBox = new JComboBox<>(new String[]{"user","seller","admin"}); center.add(roleBox,gbc);

            gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2;
            JPanel pbtn = new JPanel(); pbtn.setBackground(new Color(248,249,250));
            JButton btnLogin = new JButton("Login");
            btnLogin.setBackground(new Color(37,150,190)); btnLogin.setForeground(Color.WHITE); btnLogin.setFocusPainted(false);
            btnLogin.addActionListener(e->doLogin());
            JButton btnSignup = new JButton("Create Account");
            btnSignup.setBackground(new Color(37,180,90)); btnSignup.setForeground(Color.WHITE); btnSignup.setFocusPainted(false);
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
                // Fake OTP system
                String otp = JOptionPane.showInputDialog(this,
                        "We are facing an SMS issue.\nPlease use 910296 as your OTP:",
                        "OTP Verification", JOptionPane.INFORMATION_MESSAGE);
                if(otp != null && otp.equals("910296")){
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

    // ---------------- Dashboard Panel ----------------
    public static class DashboardPanel extends JPanel {
        private MainFrame parent; private User user; private JTabbedPane tabs;
        private CardBrowsePanel browsePanel; private SellerPanel sellerPanel; private AdminPanel adminPanel;
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
            add(tabs,BorderLayout.CENTER);
        }

        public void setUser(User user){
            this.user=user; tabs.removeAll();
            tabs.addTab("Browse Cars",browsePanel);
            switch(user.role.toLowerCase()){
                case "seller": tabs.addTab("My Inventory",sellerPanel); break;
                case "admin": tabs.addTab("Manage Cars",sellerPanel); tabs.addTab("Users",adminPanel); break;
            }
            browsePanel.setCurrentUser(user); sellerPanel.setCurrentUser(user);
            refreshAll();
        }

        public void refreshAll(){browsePanel.refresh(); sellerPanel.refresh(); adminPanel.refresh();}
    }

    // ---------------- Card Browse Panel ----------------
    public static class CardBrowsePanel extends JPanel {
        private MainFrame parent; private JPanel cardContainer; private JScrollPane scrollPane;
        private JTextField txtSearch; private User currentUser;

        public CardBrowsePanel(MainFrame parent){
            this.parent=parent; setLayout(new BorderLayout()); setBorder(new EmptyBorder(10,10,10,10));

            JLabel header = new JLabel("🚗 Car Inventory",SwingConstants.CENTER);
            header.setFont(new Font("Segoe UI",Font.BOLD,22)); header.setOpaque(true);
            header.setBackground(new Color(18,120,225)); header.setForeground(Color.WHITE);
            header.setBorder(new EmptyBorder(12,12,12,12));

            JPanel topPanel = new JPanel(new BorderLayout(4,4));
            topPanel.add(header,BorderLayout.NORTH);

            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchPanel.add(new JLabel("Search:"));
            txtSearch = new JTextField(20);
            searchPanel.add(txtSearch);
            JButton btnSearch = new JButton("Go"); btnSearch.addActionListener(e -> refresh());
            searchPanel.add(btnSearch);
            topPanel.add(searchPanel,BorderLayout.SOUTH);

            add(topPanel,BorderLayout.NORTH);

            cardContainer = new JPanel(); cardContainer.setLayout(new WrapLayout(FlowLayout.LEFT,16,16));
            scrollPane = new JScrollPane(cardContainer); scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane,BorderLayout.CENTER);
        }

        public void setCurrentUser(User u){currentUser=u;}

        public void refresh(){
            String query = txtSearch.getText().trim().toLowerCase();
            cardContainer.removeAll();
            for(Car c:DataStore.fetchCars()){
                if(!query.isEmpty() && !(c.name.toLowerCase().contains(query) || c.category.toLowerCase().contains(query))) continue;

                JPanel card = new JPanel(new BorderLayout(4,4));
                card.setPreferredSize(new Dimension(240,360));
                card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY,1,true));

                JLabel imgLabel = new JLabel(); imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imgLabel.setIcon(UIUtils.loadScaledIcon(c.imagePath,220,140));
                card.add(imgLabel,BorderLayout.NORTH);

                JPanel info = new JPanel(); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS)); info.setBorder(new EmptyBorder(6,6,6,6));
                JLabel lblName = new JLabel(c.name+" ("+c.model+")"); lblName.setFont(new Font("Segoe UI",Font.BOLD,14)); info.add(lblName);

                int stars = 3 + new Random().nextInt(3); StringBuilder sb=new StringBuilder();
                for(int i=0;i<stars;i++) sb.append("★"); for(int i=stars;i<5;i++) sb.append("☆");
                JLabel lblRating=new JLabel(sb.toString()); lblRating.setForeground(new Color(255,140,0)); info.add(lblRating);

                info.add(new JLabel("Seats: "+c.seats+" | Fuel: "+c.fuelType+" | Transmission: "+c.transmission));
                info.add(new JLabel("Category: "+c.category));
                info.add(new JLabel("Price/day: ₹"+UIUtils.MONEY.format(c.pricePerDay)));
                JLabel lblStatus = new JLabel("Status: "+c.status);
                if("Available".equalsIgnoreCase(c.status)) lblStatus.setForeground(new Color(0,128,0)); else lblStatus.setForeground(Color.RED);
                info.add(lblStatus);

                // Booking button
                if(currentUser.role.equals("user")){
                    JButton bookBtn = new JButton("Book");
                    bookBtn.setEnabled("Available".equalsIgnoreCase(c.status));
                    bookBtn.addActionListener(e -> {
                        if(!"Available".equalsIgnoreCase(c.status)){
                            JOptionPane.showMessageDialog(this,"Car is not available for booking.","Booking Error",JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        new BookingDialog(parent,c).setVisible(true);
                    });
                    info.add(Box.createVerticalStrut(6)); info.add(bookBtn);
                }

                card.add(info,BorderLayout.CENTER);
                cardContainer.add(card);
            }
            cardContainer.revalidate(); cardContainer.repaint();
        }
    }

    // ---------------- Booking Dialog ----------------
    public static class BookingDialog extends JDialog {
        public BookingDialog(MainFrame parent,Car car){
            super(parent,"Booking: "+car.name,true);
            setSize(400,400); setLocationRelativeTo(parent);

            JPanel panel = new JPanel(new GridLayout(0,2,6,6)); panel.setBorder(new EmptyBorder(12,12,12,12));

            panel.add(new JLabel("Pickup Place:")); JTextField txtPlace=new JTextField(); panel.add(txtPlace);
            panel.add(new JLabel("Pickup Date (yyyy-mm-dd):")); JTextField txtDate=new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date())); panel.add(txtDate);
            panel.add(new JLabel("Pickup Time:")); JTextField txtTime=new JTextField("10:00 AM"); panel.add(txtTime);
            panel.add(new JLabel("Number of Days:")); JSpinner spinDays=new JSpinner(new SpinnerNumberModel(1,1,30,1)); panel.add(spinDays);

            JLabel lblPrice = new JLabel("Total Price: ₹0"); panel.add(lblPrice); panel.add(new JLabel("")); // placeholder

            spinDays.addChangeListener((ChangeEvent e)->{
                int days=(Integer)spinDays.getValue();
                double total=car.pricePerDay*days;
                lblPrice.setText("Total Price: ₹"+UIUtils.MONEY.format(total));
            });

            JButton btnConfirm=new JButton("Confirm Booking"); btnConfirm.addActionListener(e->{
                if(txtPlace.getText().trim().isEmpty()){JOptionPane.showMessageDialog(this,"Enter pickup place."); return;}
                try{
                    int days=(Integer)spinDays.getValue();
                    double total=car.pricePerDay*days;
                    car.status="Rented";
                    DataStore.updateCar(car);
                    parent.refreshAll();
                    JOptionPane.showMessageDialog(this,"Booking confirmed!\nTotal Price: ₹"+UIUtils.MONEY.format(total),"Success",JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }catch(Exception ex){ex.printStackTrace();}
            });

            JPanel bottom = new JPanel(); bottom.add(btnConfirm);
            add(panel,BorderLayout.CENTER); add(bottom,BorderLayout.SOUTH);
        }
    }

    // ---------------- Seller Panel ----------------
    public static class SellerPanel extends JPanel {
        private MainFrame parent; private User currentUser; private JTable table; private DefaultTableModel model;
        public SellerPanel(MainFrame parent){this.parent=parent; setLayout(new BorderLayout());}

        public void setCurrentUser(User u){currentUser=u; refresh();}

        public void refresh(){
            removeAll();
            model = new DefaultTableModel(new Object[]{"ID","Name","Model","Price/Day","Seats","Fuel","Transmission","Status"},0){
                public boolean isCellEditable(int row,int col){return false;}
            };
            table = new JTable(model);
            for(Car c:DataStore.fetchCars()){
                if(currentUser.role.equals("seller") && !c.ownerId.equals(currentUser.id)) continue;
                model.addRow(new Object[]{c.id,c.name,c.model,c.pricePerDay,c.seats,c.fuelType,c.transmission,c.status});
            }
            add(new JScrollPane(table),BorderLayout.CENTER);

            JPanel btnPanel = new JPanel();
            JButton addBtn=new JButton("Add Car"); addBtn.addActionListener(e->addOrEditCar(null));
            JButton editBtn=new JButton("Edit Car"); editBtn.addActionListener(e->{int r=table.getSelectedRow(); if(r>=0)addOrEditCar(DataStore.findCarById((String)table.getValueAt(r,0)));});
            JButton delBtn=new JButton("Delete Car"); delBtn.addActionListener(e->{int r=table.getSelectedRow(); if(r>=0){DataStore.deleteCar((String)table.getValueAt(r,0)); refresh();}});
            btnPanel.add(addBtn); btnPanel.add(editBtn); btnPanel.add(delBtn);
            add(btnPanel,BorderLayout.SOUTH);

            revalidate(); repaint();
        }

        private void addOrEditCar(Car car){
            JTextField name = new JTextField(car!=null?car.name:"");
            JTextField model = new JTextField(car!=null?car.model:"");
            JTextField price = new JTextField(car!=null?String.valueOf(car.pricePerDay):"0");
            JTextField seats = new JTextField(car!=null?String.valueOf(car.seats):"5");
            JTextField fuel = new JTextField(car!=null?car.fuelType:"Petrol");
            JTextField trans = new JTextField(car!=null?car.transmission:"Automatic");
            JComboBox<String> statusBox = new JComboBox<>(new String[]{"Available","Rented","Sold"});
            if(car!=null) statusBox.setSelectedItem(car.status);

            JButton imgBtn = new JButton("Upload Image"); JLabel imgLbl = new JLabel(car!=null?car.imagePath:""); imgLbl.setVisible(false);

            imgBtn.addActionListener(e->{
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Images","jpg","jpeg","png"));
                if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
                    File f=fc.getSelectedFile(); try{imgLbl.setText(UIUtils.copyImageToStore(f,f.getName()));}catch(Exception ex){ex.printStackTrace();}
                }
            });

            Object[] fields = {"Name",name,"Model",model,"Price/Day",price,"Seats",seats,"Fuel Type",fuel,"Transmission",trans,"Status",statusBox,imgBtn};

            int ans = JOptionPane.showConfirmDialog(this,fields,car==null?"Add Car":"Edit Car",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(ans==JOptionPane.OK_OPTION){
                try{
                    double p = Double.parseDouble(price.getText());
                    int s = Integer.parseInt(seats.getText());
                    String imgPath = imgLbl.getText().isEmpty()?car!=null?car.imagePath:UIUtils.PLACEHOLDER_IMAGE:UIUtils.PLACEHOLDER_IMAGE;
                    if(car==null){
                        Car newCar = new Car(DataStore.nextCarId(),name.getText(),model.getText(),p,"Unknown",(String)statusBox.getSelectedItem(),imgPath,currentUser.id,fuel.getText(),s,trans.getText());
                        DataStore.addCar(newCar);
                    }else{
                        car.name=name.getText(); car.model=model.getText(); car.pricePerDay=p; car.seats=s; car.fuelType=fuel.getText(); car.transmission=trans.getText(); car.status=(String)statusBox.getSelectedItem(); car.imagePath=imgPath;
                        DataStore.updateCar(car);
                    }
                    refresh(); parent.refreshAll();
                }catch(Exception ex){JOptionPane.showMessageDialog(this,"Invalid input!","Error",JOptionPane.ERROR_MESSAGE);}
            }
        }
    }

    // ---------------- Admin Panel ----------------
    public static class AdminPanel extends JPanel{
        private JTable table; private DefaultTableModel model;
        public AdminPanel(MainFrame parent){
            setLayout(new BorderLayout());
        }
        public void refresh(){
            removeAll();
            model = new DefaultTableModel(new Object[]{"ID","Username","Role","Contact"},0){public boolean isCellEditable(int r,int c){return false;}};
            table = new JTable(model);
            for(User u:DataStore.fetchUsers()){model.addRow(new Object[]{u.id,u.username,u.role,u.contact});}
            add(new JScrollPane(table),BorderLayout.CENTER);
            revalidate(); repaint();
        }
    }

    // ---------------- WrapLayout (for cards) ----------------
    public static class WrapLayout extends FlowLayout{
        public WrapLayout(){super();}
        public WrapLayout(int align,int hgap,int vgap){super(align,hgap,vgap);}
        public Dimension preferredLayoutSize(Container target){return layoutSize(target,true);}
        public Dimension minimumLayoutSize(Container target){return layoutSize(target,false);}
        private Dimension layoutSize(Container target,boolean preferred){
            synchronized(target.getTreeLock()){
                int w=target.getWidth(); if(w==0) w=Integer.MAX_VALUE;
                int hgap=getHgap(),vgap=getVgap(); Insets insets=target.getInsets(); int maxwidth=w-(insets.left+insets.right+hgap*2);
                int x=0,y=vgap,rowh=0;
                int nmembers=target.getComponentCount();
                for(int i=0;i<nmembers;i++){
                    Component m=target.getComponent(i); if(!m.isVisible()) continue;
                    Dimension d=preferred?m.getPreferredSize():m.getMinimumSize();
                    if(x==0||(x+d.width)>maxwidth){x=0;y+=rowh+vgap; rowh=d.height;}else{rowh=Math.max(rowh,d.height);}
                    x+=d.width+hgap;
                }
                y+=rowh+vgap; return new Dimension(w,y+insets.top+insets.bottom);
            }
        }
    }

    // ---------------- Main ----------------
    public static void main(String[] args){
        UIUtils.ensureImagesFolder();
        SwingUtilities.invokeLater(()->{
            MainFrame f = new MainFrame(); f.setVisible(true);
        });
    }
}
