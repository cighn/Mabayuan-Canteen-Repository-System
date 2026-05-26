import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;

public class Dashboard extends JFrame {

    private JPanel contentArea;
    private CardLayout cardLayout;
    private JButton btnDashboard, btnProducts, btnCategories,
            btnStock, btnTabManagement, btnIdentity;

    private ProductsPanel productsPanel;
    private TabManagementPanel tabManagementPanel;

    private int entityId;
    private String userRole;
    private String username;

    // Logo image
    private BufferedImage logoImage;

    public Dashboard(String username, String role, int entityId) {
        this.entityId = entityId;
        this.userRole = role;
        this.username = username;

        loadImages(); // load school logo

        setTitle("Mabayuan Canteen Inventory System");
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildSidebar(username, role), BorderLayout.WEST);
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildContentArea(), BorderLayout.CENTER);

        cardLayout.show(contentArea, "DASHBOARD");
        setVisible(true);
    }

    // Load logo (same as in Frame)
    private void loadImages() {
        logoImage = loadImage("school.png");
        if (logoImage == null)
            logoImage = loadImage("/school.png");
        if (logoImage == null)
            logoImage = loadImage("src/school.png");
        if (logoImage == null)
            logoImage = loadImage("res/school.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            // Try as resource from classpath
            URL url = getClass().getResource(path);
            if (url != null)
                return ImageIO.read(url);

            // Try as absolute file
            java.io.File f = new java.io.File(path);
            if (f.exists())
                return ImageIO.read(f);

            // Try from user directory
            f = new java.io.File(System.getProperty("user.dir"), path);
            if (f.exists())
                return ImageIO.read(f);
        } catch (Exception e) {
            /* ignore */ }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────
    // Sidebar (fixed: added logo, removed Global Search)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildSidebar(String username, String role) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(0x1A2E1A));
        sidebar.setPreferredSize(new Dimension(210, 0));

        // ── Brand block with logo ──────────────────────────────────────
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBackground(new Color(0x1A2E1A));
        brand.setBorder(BorderFactory.createEmptyBorder(16, 14, 12, 4)); // \u2705 right padding from 14 \u2192 4
        brand.setMaximumSize(new Dimension(210, 110));

        // Horizontal panel for logo + text
        JPanel brandTop = new JPanel(new BorderLayout(8, 0));
        brandTop.setOpaque(false);

        // School logo (left)
        JLabel logoLabel = new JLabel();
        if (logoImage != null) {
            Image scaled = logoImage.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaled));
        } else {
            logoLabel.setText("🏫");
            logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        }
        logoLabel.setVerticalAlignment(SwingConstants.TOP);

        // Text (right)
        JLabel title = new JLabel("<html>MABAYUAN ELEMENTARY<br>Canteen Inventory System</html>");
        title.setFont(new Font("Arial", Font.BOLD, 11));
        title.setForeground(new Color(0x7DC97D));

        brandTop.add(logoLabel, BorderLayout.WEST);
        brandTop.add(title, BorderLayout.CENTER);

        JLabel roleBadge = new JLabel(role.toUpperCase());
        roleBadge.setFont(new Font("Arial", Font.BOLD, 9));
        roleBadge.setForeground(Color.WHITE);
        roleBadge.setOpaque(true);
        roleBadge.setBackground(new Color(0xD85A30));
        roleBadge.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        roleBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(brandTop);
        brand.add(Box.createVerticalStrut(6));
        brand.add(roleBadge);
        sidebar.add(brand);
        sidebar.add(makeDivider());

        // ── Nav buttons ────────────────────────
        sidebar.add(makeSectionLabel("OVERVIEW"));
        btnDashboard = makeNavButton("  Dashboard", "DASHBOARD");
        sidebar.add(btnDashboard);

        sidebar.add(makeSectionLabel("INVENTORY"));
        btnProducts = makeNavButton("  Products", "PRODUCTS");
        btnCategories = makeNavButton("  Categories", "CATEGORIES");
        btnStock = makeNavButton("  Stock / Supply", "STOCK");
        sidebar.add(btnProducts);
        sidebar.add(btnCategories);
        sidebar.add(btnStock);

        sidebar.add(makeSectionLabel("MANAGEMENT"));
        btnTabManagement = makeNavButton("  Tab Management", "TABMANAGEMENT");
        btnIdentity = makeNavButton("  Identity / Users", "IDENTITY");

        if ("Staff".equals(role)) {
            btnIdentity.setVisible(false);
        }

        sidebar.add(btnTabManagement);
        sidebar.add(btnIdentity);

        // ── Footer ────────────────────────────────────────────────────
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(makeDivider());
        sidebar.add(buildUserFooter(username, role));
        sidebar.add(makeDivider());
        sidebar.add(buildLogoutButton());

        return sidebar;
    }

    // ─────────────────────────────────────────────────────────────────
    // Logout Button (unchanged)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildLogoutButton() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(0x1A2E1A));
        wrapper.setMaximumSize(new Dimension(210, 44));
        wrapper.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));

        JButton logoutBtn = new JButton("  \u2192  Log Out");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setForeground(new Color(0xD85A30));
        logoutBtn.setBackground(new Color(0x1A2E1A));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusable(false);
        logoutBtn.setHorizontalAlignment(SwingConstants.LEFT);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setMaximumSize(new Dimension(210, 36));

        logoutBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                logoutBtn.setBackground(new Color(0x2A1A1A));
                logoutBtn.repaint();
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                logoutBtn.setBackground(new Color(0x1A2E1A));
                logoutBtn.repaint();
            }
        });

        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to log out?",
                    "Log Out",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                logAccessLogout();
                dispose();
                new Frame();
            }
        });

        wrapper.add(logoutBtn, BorderLayout.CENTER);
        return wrapper;
    }

    private void logAccessLogout() {
        String sql = "INSERT INTO access_log (staff_id, action, performed_at) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entityId);
            ps.setString(2, "Logged out");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Improved nav button with optional icon (kept simple but consistent)
    private JButton makeNavButton(String label, String panelName) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Arial", Font.PLAIN, 13));
        btn.setForeground(new Color(0x8AAA8A));
        btn.setBackground(new Color(0x1A2E1A));
        btn.setBorderPainted(false);
        btn.setFocusable(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(210, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            cardLayout.show(contentArea, panelName);
            setAllNavInactive();
            btn.setBackground(new Color(0x2E7D32));
            btn.setForeground(Color.WHITE);
        });
        return btn;
    }

    private void setAllNavInactive() {
        for (JButton b : new JButton[] {
                btnDashboard, btnProducts,
                btnCategories, btnStock, btnTabManagement, btnIdentity }) {
            if (b != null) {
                b.setBackground(new Color(0x1A2E1A));
                b.setForeground(new Color(0x8AAA8A));
            }
        }
    }

    private JLabel makeSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 9));
        lbl.setForeground(new Color(0x4A7A4A));
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));
        lbl.setMaximumSize(new Dimension(210, 24));
        return lbl;
    }

    private JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x2E4A2E));
        sep.setMaximumSize(new Dimension(210, 1));
        return sep;
    }

    private JPanel buildUserFooter(String username, String role) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(new Color(0x1A2E1A));
        panel.setMaximumSize(new Dimension(210, 50));

        String initials = username.length() >= 2
                ? username.substring(0, 2).toUpperCase()
                : username.toUpperCase();

        JLabel avatar = new JLabel(initials, SwingConstants.CENTER);
        avatar.setFont(new Font("Arial", Font.BOLD, 11));
        avatar.setForeground(new Color(0x7DC97D));
        avatar.setOpaque(true);
        avatar.setBackground(new Color(0x2E7D32));
        avatar.setPreferredSize(new Dimension(30, 30));

        JLabel nameLabel = new JLabel(
                "<html><b style='color:#cccccc'>" + username +
                        "</b><br><span style='color:#4a7a4a;font-size:10px'>" + role + "</span></html>");

        panel.add(avatar);
        panel.add(nameLabel);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────
    // Top Bar (unchanged)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0x1A2E1A));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2E4A2E)),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));

        JLabel titleLabel = new JLabel("Dashboard  —  Inventory Overview");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 15));
        titleLabel.setForeground(new Color(0xE0E0E0));

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBar.setBackground(new Color(0x1A2E1A));

        JLabel statusPill = new JLabel("● System Online");
        statusPill.setFont(new Font("Arial", Font.BOLD, 11));
        statusPill.setForeground(new Color(0x7DC97D));
        statusPill.setOpaque(true);
        statusPill.setBackground(new Color(0x1B3A1B));
        statusPill.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));

        JLabel dateLabel = new JLabel(
                new java.text.SimpleDateFormat("EEE, MMM d, yyyy").format(new java.util.Date()));
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        dateLabel.setForeground(new Color(0x6A8A6A));

        rightBar.add(statusPill);
        rightBar.add(dateLabel);
        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(rightBar, BorderLayout.EAST);
        return topBar;
    }

    // ─────────────────────────────────────────────────────────────────
    // Content Area (Global Search placeholder removed)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildContentArea() {
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(new Color(0x1E2E1E));

        contentArea.add(new DashboardPanel(), "DASHBOARD");
        productsPanel = new ProductsPanel();
        contentArea.add(productsPanel, "PRODUCTS");
        contentArea.add(new CategoriesPanel(), "CATEGORIES");
        contentArea.add(new StockSupplyPanel(), "STOCK");
        tabManagementPanel = new TabManagementPanel();
        contentArea.add(tabManagementPanel, "TABMANAGEMENT");
        contentArea.add(new IdentityPanel(this.userRole, entityId), "IDENTITY");

        // No SEARCH panel anymore

        return contentArea;
    }

    public void refreshAllPanels() {
        if (productsPanel != null)
            productsPanel.refreshTable();
        if (tabManagementPanel != null)
            tabManagementPanel.refreshAll();
    }

    // Keep main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard("Elijah", "Admin", 1));
    }
}