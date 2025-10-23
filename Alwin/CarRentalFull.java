/* CarRentalFull.java
   Fully functional single-file Swing Car Rental app
   - Roles: admin, seller, user
   - Admin: manage all cars & users
   - Seller: manage own cars
   - User: browse cars (card-based)
   - Car image upload supported
   - In-memory datastore + images stored in images/
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
import java.util.*;
import java.util.List;

public class CarRentalFull {

    // ---------------- Models ----------------
    public static class Car {
        public String id, name, model, category, status, imagePath, ownerId;
        public double pricePerDay;

        public Car(String id, String name, String model, double pricePerDay, String category, String status, String imagePath, String ownerId) {
            this.id = id; this.name = name; this.model=model; this.pricePerDay=pricePerDay;
            this.category=category; this.status=status; this.imagePath=imagePath; this.ownerId=ownerId;
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

            cars.add(new Car("C001","Toyota Camry","Camry",3500,"Sedan","Available","images/Toyota_Camry.jpeg","U003"));
            cars.add(new Car("C002","Honda Civic","Civic",3200,"Sedan","Available","images/Honda_Civic.jpeg","U003"));
        }

        public static Optional<User> authenticate(String username,String password,String role){
            return users.stream().filter(u->u.username.equalsIgnoreCase(username)&&u.password.equals(password)&&u.role.equalsIgnoreCase(role)).findFirst();
        }

        public static List<User> fetchUsers(){return new ArrayList<>(users);}
        public static List<Car> fetchCars(){return new ArrayList<>(cars);}

        public static void addCar(Car c){cars.add(c);}
        public static void updateCar(Car updated){for(int i=0;i<cars.size();i++){if(cars.get(i).id.equals(updated.id)){cars.set(i,updated);return;}}}
        public static void deleteCar(String id){cars.removeIf(c->c.id.equals(id));}

        public static String nextCarId(){
            int max = cars.stream().mapToInt(c->{try{return Integer.parseInt(c.id.replaceAll("\\D+",""));}catch(Exception e){return 0;}}).max().orElse(0);
            return String.format("C%03d",max+1);
        }

        public static User findUserById(String id){return users.stream().filter(u->u.id.equals(id)).findFirst().orElse(null);}
    }

    // ---------------- UI Utils ----------------
    public static class UIUtils {
        public static final DecimalFormat MONEY = new DecimalFormat("#,###.##");
        public static void ensureImagesFolder(){try{Files.createDirectories(Paths.get("images"));}catch(IOException e){System.err.println(e);}}
        public static String copyImageToStore(File sourceFile,String targetFileName)throws IOException{
            ensureImagesFolder();
            Path target = Paths.get("images",targetFileName);
            Files.copy(sourceFile.toPath(),target,StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace("\\","/");
        }
        public static ImageIcon loadScaledIcon(String imagePath,int w,int h){
            if(imagePath==null) return null;
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
            gbc.gridx=1; txtUser = new JTextField(22); txtUser.setToolTipText("e.g. seller@demo"); center.add(txtUser,gbc);

            gbc.gridx=0; gbc.gridy=1; center.add(new JLabel("Password:"),gbc);
            gbc.gridx=1; txtPass = new JPasswordField(22); center.add(txtPass,gbc);

            gbc.gridx=0; gbc.gridy=2; center.add(new JLabel("Role:"),gbc);
            gbc.gridx=1; roleBox = new JComboBox<>(new String[]{"user","seller","admin"}); center.add(roleBox,gbc);

            gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2;
            JButton btnLogin = new JButton("Login");
            btnLogin.setBackground(new Color(37,150,190)); btnLogin.setForeground(Color.WHITE); btnLogin.setFocusPainted(false);
            btnLogin.addActionListener(e->doLogin());
            JPanel pbtn = new JPanel(); pbtn.setBackground(new Color(248,249,250)); pbtn.add(btnLogin); center.add(pbtn,gbc);
            add(center,BorderLayout.CENTER);

            JLabel hint = new JLabel("<html><small>Demo login: admin@demo/admin123 | user@demo/user123 | seller@demo/seller123</small></html>",SwingConstants.CENTER);
            hint.setBorder(new EmptyBorder(8,8,12,8));
            add(hint,BorderLayout.SOUTH);
        }
        private void doLogin(){
            String u=txtUser.getText().trim(); String p=new String(txtPass.getPassword());
            String role=(String)roleBox.getSelectedItem();
            if(u.isEmpty()||p.isEmpty()){JOptionPane.showMessageDialog(this,"Enter credentials.","Validation",JOptionPane.WARNING_MESSAGE); return;}
            Optional<User> found = DataStore.authenticate(u,p,role);
            if(found.isPresent()){parent.showDashboard(found.get()); txtPass.setText("");}
            else{JOptionPane.showMessageDialog(this,"Invalid credentials or role.","Login Failed",JOptionPane.ERROR_MESSAGE);}
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
            browsePanel.refresh(); sellerPanel.setCurrentUser(user); sellerPanel.refresh(); adminPanel.refresh();
        }
    }

    // ---------------- Card Browse Panel ----------------
    public static class CardBrowsePanel extends JPanel {
        private MainFrame parent; private JPanel cardContainer; private JScrollPane scrollPane;
        public CardBrowsePanel(MainFrame parent){
            this.parent=parent; setLayout(new BorderLayout()); setBorder(new EmptyBorder(10,10,10,10));

            JLabel header = new JLabel("🚗 Car Inventory",SwingConstants.CENTER);
            header.setFont(new Font("Segoe UI",Font.BOLD,22)); header.setOpaque(true);
            header.setBackground(new Color(18,120,225)); header.setForeground(Color.WHITE);
            header.setBorder(new EmptyBorder(12,12,12,12));
            add(header,BorderLayout.NORTH);

            cardContainer = new JPanel(); cardContainer.setLayout(new WrapLayout(FlowLayout.LEFT,16,16));
            scrollPane = new JScrollPane(cardContainer); scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane,BorderLayout.CENTER);
        }

        public void refresh(){
            cardContainer.removeAll();
            for(Car c:DataStore.fetchCars()){
                JPanel card = new JPanel(new BorderLayout(4,4));
                card.setPreferredSize(new Dimension(240,320));
                card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY,1,true));

                JLabel imgLabel = new JLabel(); imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imgLabel.setIcon(UIUtils.loadScaledIcon(c.imagePath,220,140));
                card.add(imgLabel,BorderLayout.NORTH);

                JPanel info = new JPanel(); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS)); info.setBorder(new EmptyBorder(6,6,6,6));
                JLabel lblName = new JLabel(c.name+" ("+c.model+")"); lblName.setFont(new Font("Segoe UI",Font.BOLD,14)); info.add(lblName);

                int stars = 3 + new Random().nextInt(3); StringBuilder sb=new StringBuilder();
                for(int i=0;i<stars;i++) sb.append("★"); for(int i=stars;i<5;i++) sb.append("☆");
                JLabel lblRating=new JLabel(sb.toString()); lblRating.setForeground(new Color(255,140,0)); info.add(lblRating);

                info.add(new JLabel("Seats: 5 | Fuel: Petrol | Transmission: Auto"));
                info.add(new JLabel("Category: "+c.category));
                info.add(new JLabel("Price/day: ₹"+UIUtils.MONEY.format(c.pricePerDay)));
                JLabel lblStatus = new JLabel("Status: "+c.status);
                if("Available".equalsIgnoreCase(c.status)) lblStatus.setForeground(new Color(0,128,0)); else lblStatus.setForeground(Color.RED);
                info.add(lblStatus);

                card.add(info,BorderLayout.CENTER);
                cardContainer.add(card);
            }
            cardContainer.revalidate(); cardContainer.repaint();
        }
    }

    // ---------------- Seller Panel (Add/Update/Delete Cars) ----------------
    public static class SellerPanel extends JPanel {
        private MainFrame parent; private User currentUser;
        private DefaultTableModel tableModel; private JTable table;
        public SellerPanel(MainFrame parent){
            this.parent=parent; setLayout(new BorderLayout());
            tableModel=new DefaultTableModel(new String[]{"ID","Name","Model","Price","Category","Status","Owner","Image"},0);
            table=new JTable(tableModel);
            JScrollPane sp=new JScrollPane(table); add(sp,BorderLayout.CENTER);

            JPanel btnPanel=new JPanel();
            JButton add=new JButton("Add Car"); JButton edit=new JButton("Edit Car"); JButton del=new JButton("Delete Car");
            btnPanel.add(add); btnPanel.add(edit); btnPanel.add(del); add(btnPanel,BorderLayout.SOUTH);

            add.addActionListener(e->addCar());
            edit.addActionListener(e->editCar());
            del.addActionListener(e->deleteCar());
        }

        public void setCurrentUser(User u){this.currentUser=u;}

        public void refresh(){
            tableModel.setRowCount(0);
            for(Car c:DataStore.fetchCars()){
                if(currentUser.role.equals("seller") && !c.ownerId.equals(currentUser.id)) continue;
                tableModel.addRow(new Object[]{c.id,c.name,c.model,c.pricePerDay,c.category,c.status,DataStore.findUserById(c.ownerId).username,c.imagePath});
            }
        }

        private void addCar(){CarDialog dlg=new CarDialog(parent,null,currentUser); dlg.setVisible(true); refresh();}
        private void editCar(){int r=table.getSelectedRow(); if(r<0){JOptionPane.showMessageDialog(this,"Select a car to edit."); return;}
            String id=(String)table.getValueAt(r,0); Car c=DataStore.fetchCars().stream().filter(car->car.id.equals(id)).findFirst().orElse(null);
            if(c==null) return; CarDialog dlg=new CarDialog(parent,c,currentUser); dlg.setVisible(true); refresh();
        }
        private void deleteCar(){int r=table.getSelectedRow(); if(r<0){JOptionPane.showMessageDialog(this,"Select a car to delete."); return;}
            String id=(String)table.getValueAt(r,0); int ans=JOptionPane.showConfirmDialog(this,"Delete this car?","Confirm",JOptionPane.YES_NO_OPTION);
            if(ans==JOptionPane.YES_OPTION){DataStore.deleteCar(id); refresh();}
        }
    }

    // ---------------- Car Add/Edit Dialog ----------------
    public static class CarDialog extends JDialog {
        private JTextField txtName,txtModel,txtPrice,txtCategory; private JComboBox<String> cmbStatus;
        private JLabel lblImage; private File selectedImage=null;
        private Car car; private User currentUser;

        public CarDialog(JFrame parent, Car car, User currentUser){
            super(parent,true); this.car=car; this.currentUser=currentUser;
            setTitle(car==null?"Add Car":"Edit Car"); setSize(400,500); setLocationRelativeTo(parent); setLayout(new BorderLayout());

            JPanel p=new JPanel(new GridBagLayout()); p.setBorder(new EmptyBorder(12,12,12,12));
            GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(8,8,8,8); gbc.fill=GridBagConstraints.HORIZONTAL;

            gbc.gridx=0; gbc.gridy=0; p.add(new JLabel("Name:"),gbc); gbc.gridx=1; txtName=new JTextField(20); p.add(txtName,gbc);
            gbc.gridx=0; gbc.gridy=1; p.add(new JLabel("Model:"),gbc); gbc.gridx=1; txtModel=new JTextField(20); p.add(txtModel,gbc);
            gbc.gridx=0; gbc.gridy=2; p.add(new JLabel("Price/day:"),gbc); gbc.gridx=1; txtPrice=new JTextField(20); p.add(txtPrice,gbc);
            gbc.gridx=0; gbc.gridy=3; p.add(new JLabel("Category:"),gbc); gbc.gridx=1; txtCategory=new JTextField(20); p.add(txtCategory,gbc);
            gbc.gridx=0; gbc.gridy=4; p.add(new JLabel("Status:"),gbc); gbc.gridx=1; cmbStatus=new JComboBox<>(new String[]{"Available","Rented","Sold"}); p.add(cmbStatus,gbc);

            gbc.gridx=0; gbc.gridy=5; gbc.gridwidth=2;
            lblImage=new JLabel("Click to select image",SwingConstants.CENTER); lblImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            lblImage.setPreferredSize(new Dimension(200,150));
            lblImage.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){chooseImage();}});
            p.add(lblImage,gbc);

            add(p,BorderLayout.CENTER);
            JButton save=new JButton("Save"); save.addActionListener(e->saveCar());
            JPanel sp=new JPanel(); sp.add(save); add(sp,BorderLayout.SOUTH);

            if(car!=null){txtName.setText(car.name); txtModel.setText(car.model); txtPrice.setText(""+car.pricePerDay); txtCategory.setText(car.category);
                cmbStatus.setSelectedItem(car.status); if(car.imagePath!=null) lblImage.setIcon(UIUtils.loadScaledIcon(car.imagePath,200,150));}
        }

        private void chooseImage(){
            JFileChooser chooser=new JFileChooser(); chooser.setFileFilter(new FileNameExtensionFilter("Images","jpg","png","jpeg","gif"));
            int r=chooser.showOpenDialog(this); if(r==JFileChooser.APPROVE_OPTION) selectedImage=chooser.getSelectedFile();
            if(selectedImage!=null) lblImage.setIcon(UIUtils.loadScaledIcon(selectedImage.getAbsolutePath(),200,150));
        }

        private void saveCar(){
            try{
                String name=txtName.getText().trim(), model=txtModel.getText().trim(), cat=txtCategory.getText().trim();
                double price=Double.parseDouble(txtPrice.getText().trim()); String status=(String)cmbStatus.getSelectedItem();
                String imgPath = car!=null?car.imagePath:null;
                if(selectedImage!=null){imgPath=UIUtils.copyImageToStore(selectedImage,(car==null?DataStore.nextCarId():car.id)+".jpg");}

                if(car==null){
                    String id=DataStore.nextCarId();
                    DataStore.addCar(new Car(id,name,model,price,cat,status,imgPath,currentUser.id));
                }else{
                    car.name=name; car.model=model; car.category=cat; car.pricePerDay=price; car.status=status; car.imagePath=imgPath;
                    DataStore.updateCar(car);
                }
                dispose();
            }catch(Exception e){JOptionPane.showMessageDialog(this,"Error: "+e.getMessage());}
        }
    }

    // ---------------- Admin Panel (User List) ----------------
    public static class AdminPanel extends JPanel {
        private DefaultTableModel tableModel; private JTable table;
        public AdminPanel(MainFrame parent){
            setLayout(new BorderLayout());
            tableModel=new DefaultTableModel(new String[]{"ID","Username","Role","Contact"},0);
            table=new JTable(tableModel);
            JScrollPane sp=new JScrollPane(table); add(sp,BorderLayout.CENTER);
            refresh();
        }
        public void refresh(){
            tableModel.setRowCount(0);
            for(User u:DataStore.fetchUsers()){
                tableModel.addRow(new Object[]{u.id,u.username,u.role,u.contact});
            }
        }
    }

    // ---------------- Custom WrapLayout ----------------
    public static class WrapLayout extends FlowLayout{
        public WrapLayout(){super();}
        public WrapLayout(int align,int hgap,int vgap){super(align,hgap,vgap);}
        public Dimension preferredLayoutSize(Container target){return layoutSize(target,true);}
        public Dimension minimumLayoutSize(Container target){return layoutSize(target,false);}
        private Dimension layoutSize(Container target,boolean preferred){
            synchronized(target.getTreeLock()){
                int hgap=getHgap(), vgap=getVgap(), maxWidth=target.getWidth()>0?target.getWidth():Integer.MAX_VALUE;
                Insets insets=target.getInsets(); int x=0,y=insets.top+vgap,rowHeight=0,reqWidth=0;
                for(Component c:target.getComponents()){if(!c.isVisible()) continue;
                    Dimension d=preferred?c.getPreferredSize():c.getMinimumSize();
                    if(x==0||x+d.width+insets.left+insets.right+hgap<=maxWidth){if(x>0)x+=hgap; x+=d.width; rowHeight=Math.max(rowHeight,d.height);}
                    else{reqWidth=Math.max(reqWidth,x); x=d.width; y+=vgap+rowHeight; rowHeight=d.height;}
                }
                y+=rowHeight+vgap; return new Dimension(reqWidth+insets.left+insets.right,y);
            }
        }
    }

    // ---------------- Main ----------------
    public static void main(String[] args){
        UIUtils.ensureImagesFolder();
        SwingUtilities.invokeLater(()->{
            try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
            new MainFrame().setVisible(true);
        });
    }
}