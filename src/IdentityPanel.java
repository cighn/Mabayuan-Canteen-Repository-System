import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class IdentityPanel extends JPanel {

    // ─── Color Palette ────────────────────────────────────────────────
    private static final Color C_BG = new Color(0x1E2E1E);
    private static final Color C_CARD = new Color(0x243824);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_ACCENT_HVR = new Color(0x3DAA6A);
    private static final Color C_BORDER = new Color(0x2E4A2E);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);
    private static final Color C_DIM = new Color(0x4A7A4A);
    private static final Color C_INPUT_BG = new Color(0x1A2A1A);
    private static final Color C_RED = new Color(0xD85A30);

    private static final Color C_ADMIN_BADGE = new Color(0xD85A30);
    private static final Color C_SENIOR_BADGE = new Color(0x1565A0);
    private static final Color C_STAFF_BADGE = new Color(0x2E7D4F);

    // ─── State ────────────────────────────────────────────────────────
    private final String currentRole;
    private final int currentEntityId;

    // ─── UI References ────────────────────────────────────────────────
    private JPanel userCardsGrid;
    private JPanel accessLogList;
    private JPanel permissionTableContainer;
    private JLabel totalUsersVal;
    private JLabel rolesAssignedVal;
    private JLabel systemAccessVal;

    public IdentityPanel(String role, int entityId) {
        this.currentRole = role;
        this.currentEntityId = entityId;

        setLayout(new BorderLayout(0, 0));
        setBackground(C_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshAll();
            }
        });

        refreshAll();
    }

    // ─────────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel pageTitle = new JLabel("Identity / Users");
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        pageTitle.setForeground(C_TEXT);

        JLabel subTitle = new JLabel("/ Management");
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitle.setForeground(C_MUTED);

        JPanel titleLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleLeft.setOpaque(false);
        titleLeft.add(pageTitle);
        titleLeft.add(subTitle);
        titleRow.add(titleLeft, BorderLayout.WEST);
        wrapper.add(titleRow, BorderLayout.NORTH);

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 80));

        totalUsersVal = new JLabel("\u2014");
        rolesAssignedVal = new JLabel("\u2014");
        systemAccessVal = new JLabel("\u2014");

        statsRow.add(makeStatCard("TOTAL USERS", totalUsersVal, "registered", new Color(0x6A1B9A), "Users"));
        statsRow.add(makeStatCard("ROLES ASSIGNED", rolesAssignedVal, "Admin · Senior · Staff", new Color(0xBF8A00),
                "Roles"));
        statsRow.add(makeStatCard("SYSTEM ACCESS", systemAccessVal, "All active", new Color(0x1565A0), "Access"));

        wrapper.add(statsRow, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel makeStatCard(String title, JLabel valLabel, String sub, Color accent, String icon) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLbl.setForeground(C_MUTED);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valLabel.setForeground(C_TEXT);

        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLbl.setForeground(C_DIM);

        left.add(titleLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(valLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(subLbl);

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        iconLbl.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 180));
        iconLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        iconLbl.setVerticalAlignment(SwingConstants.CENTER);

        card.add(left, BorderLayout.CENTER);
        card.add(iconLbl, BorderLayout.EAST);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Main Content
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildMainContent() {
        JPanel content = new JPanel(new GridLayout(1, 2, 14, 0));
        content.setOpaque(false);
        content.add(buildUserCardsPanel());
        content.add(buildRightColumn());
        return content;
    }

    private JPanel buildUserCardsPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(C_CARD);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        userCardsGrid = new JPanel(new GridLayout(0, 2, 12, 12));
        userCardsGrid.setOpaque(false);
        userCardsGrid.setBorder(new EmptyBorder(4, 4, 4, 4));

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.add(userCardsGrid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(scrollWrapper);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRightColumn() {
        JPanel col = new JPanel(new GridLayout(2, 1, 0, 14));
        col.setOpaque(false);
        col.add(buildPermissionsPanel());
        col.add(buildAccessLogPanel());
        return col;
    }

    private JPanel buildPermissionsPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_CARD);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Role Permissions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(C_TEXT);
        card.add(title, BorderLayout.NORTH);

        permissionTableContainer = new JPanel(new BorderLayout());
        permissionTableContainer.setOpaque(false);
        permissionTableContainer.setBackground(C_CARD);

        card.add(permissionTableContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAccessLogPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_CARD);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Access Log");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(C_TEXT);
        card.add(title, BorderLayout.NORTH);

        accessLogList = new JPanel();
        accessLogList.setLayout(new BoxLayout(accessLogList, BoxLayout.Y_AXIS));
        accessLogList.setOpaque(false);

        JScrollPane scroll = new JScrollPane(accessLogList);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Refresh
    // ─────────────────────────────────────────────────────────────────
    public void refreshAll() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            refreshStats(conn);
            refreshUserCards(conn);
            refreshPermissionTable(conn);
            refreshAccessLog(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "DB Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshStats(Connection conn) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM entity_table");
        if (rs.next())
            totalUsersVal.setText(String.valueOf(rs.getInt(1)));

        rs = conn.createStatement().executeQuery("SELECT COUNT(DISTINCT entity_role) FROM entity_table");
        if (rs.next())
            rolesAssignedVal.setText(String.valueOf(rs.getInt(1)));

        rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM entity_table");
        int total = rs.next() ? rs.getInt(1) : 0;
        systemAccessVal.setText(total + "/" + total);
    }

    private void refreshUserCards(Connection conn) throws SQLException {
        userCardsGrid.removeAll();

        ResultSet rs = conn.createStatement().executeQuery(
                "SELECT entity_id, entity_name, entity_email, `entity#`, entity_role, entity_passcode " +
                        "FROM entity_table ORDER BY entity_id ASC");

        while (rs.next()) {
            String passcode = rs.getString("entity_passcode");
            userCardsGrid.add(buildUserCard(
                    rs.getInt("entity_id"),
                    rs.getString("entity_name"),
                    rs.getString("entity_email"),
                    rs.getString("entity#"),
                    rs.getString("entity_role"),
                    passcode));
        }

        if ("Admin".equals(currentRole)) {
            userCardsGrid.add(buildAddUserCard());
        }

        userCardsGrid.revalidate();
        userCardsGrid.repaint();
    }

    // FIX #1: Admin sees actual passcode, others see dots
    private JPanel buildUserCard(int entityId, String name, String email, String phone, String role, String passcode) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(new Color(0x1A2A1A));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
        Color avatarColor = switch (role) {
            case "Admin" -> new Color(0xD85A30);
            case "Senior_Staff" -> new Color(0x1565A0);
            default -> new Color(0x2E7D4F);
        };

        JLabel avatar = new JLabel(initials, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(avatarColor);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avatar.setForeground(Color.WHITE);
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(52, 52));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel avatarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        avatarWrapper.setOpaque(false);
        avatarWrapper.add(avatar);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(name, SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(C_TEXT);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emailLbl = new JLabel(email, SwingConstants.CENTER);
        emailLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        emailLbl.setForeground(C_MUTED);
        emailLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleBadge = new JLabel(role.replace("_", " ").toUpperCase(), SwingConstants.CENTER);
        roleBadge.setFont(new Font("Segoe UI", Font.BOLD, 9));
        roleBadge.setForeground(Color.WHITE);
        roleBadge.setOpaque(true);
        roleBadge.setBackground(avatarColor);
        roleBadge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        roleBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(2));
        info.add(emailLbl);
        info.add(Box.createVerticalStrut(6));
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(roleBadge);
        info.add(badgeRow);

        // Show actual passcode only if current user is Admin
        String passcodeDisplay = "Admin".equals(currentRole) ? (passcode != null ? passcode : "")
                : "\u25CF\u25CF\u25CF\u25CF\u25CF";

        JPanel details = new JPanel(new GridLayout(3, 2, 4, 2));
        details.setOpaque(false);
        details.add(makeDetailLabel("Phone"));
        details.add(makeDetailValue(phone));
        details.add(makeDetailLabel("Entity ID"));
        details.add(makeDetailValue("#" + String.format("%03d", entityId)));
        details.add(makeDetailLabel("Passcode"));
        details.add(makeDetailValue(passcodeDisplay));

        boolean canEdit = "Admin".equals(currentRole) ||
                ("Senior_Staff".equals(currentRole) &&
                        PermissionManager.hasGrantedPermission(currentEntityId, "manage_users"));
        boolean canRemove = "Admin".equals(currentRole);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setOpaque(false);

        JButton editBtn = makeActionButton("Edit", C_ACCENT, canEdit);
        JButton removeBtn = makeActionButton("Remove", C_RED, canRemove);

        if (canEdit) {
            editBtn.addActionListener(e -> openEditDialog(entityId, name, email, phone, role));
        }
        if (canRemove) {
            removeBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Remove user '" + name + "'?", "Confirm Remove",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION)
                    removeUser(entityId);
            });
        }

        btnRow.add(editBtn);
        btnRow.add(removeBtn);

        card.add(avatarWrapper, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.add(details);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(btnRow);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAddUserCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(new Color(0x1A2A1A));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel plus = new JLabel("+", SwingConstants.CENTER);
        plus.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        plus.setForeground(C_MUTED);
        plus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel("Add New User", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(C_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(plus);
        inner.add(Box.createVerticalStrut(6));
        inner.add(lbl);
        card.add(inner);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                openAddUserDialog();
            }

            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(0x1E3A1E));
                card.repaint();
            }

            public void mouseExited(MouseEvent e) {
                card.setBackground(new Color(0x1A2A1A));
                card.repaint();
            }
        });
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Permissions Table – FIX #2: No scrollbar
    // ─────────────────────────────────────────────────────────────────
    private void refreshPermissionTable(Connection conn) throws SQLException {
        String[] columns = { "PERMISSION", "ADMIN", "SENIOR", "STAFF" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        String sql = "SELECT permission_name, " +
                "MAX(CASE WHEN role_name='Admin'        THEN is_allowed END) as admin_allowed, " +
                "MAX(CASE WHEN role_name='Senior_Staff' THEN is_allowed END) as senior_allowed, " +
                "MAX(CASE WHEN role_name='Staff'        THEN is_allowed END) as staff_allowed " +
                "FROM role_permissions GROUP BY permission_name ORDER BY permission_name";

        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            String perm = capitalize(rs.getString("permission_name").replace("_", " "));
            model.addRow(new Object[] {
                    perm,
                    rs.getInt("admin_allowed") == 1 ? "Y" : "N",
                    rs.getInt("senior_allowed") == 1 ? "Y" : "N",
                    rs.getInt("staff_allowed") == 1 ? "Y" : "N"
            });
        }

        JTable table = new JTable(model);
        table.setBackground(C_CARD);
        table.setForeground(C_TEXT);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(30);
        table.setGridColor(C_BORDER);
        table.setShowGrid(true);
        table.setPreferredScrollableViewportSize(null);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(C_ACCENT);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        for (int i = 1; i <= 3; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(60);
        }

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (col > 0) {
                    if ("Y".equals(value)) {
                        label.setForeground(new Color(0x22C55E));
                    } else {
                        label.setForeground(new Color(0xEF4444));
                    }
                    label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else {
                    label.setForeground(C_TEXT);
                }
                return label;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        permissionTableContainer.removeAll();
        permissionTableContainer.add(table, BorderLayout.CENTER);
        permissionTableContainer.revalidate();
        permissionTableContainer.repaint();
    }

    // ─────────────────────────────────────────────────────────────────
    // Access Log
    // ─────────────────────────────────────────────────────────────────
    private void refreshAccessLog(Connection conn) throws SQLException {
        accessLogList.removeAll();

        String sql = "SELECT e.entity_name, al.action, al.performed_at " +
                "FROM access_log al " +
                "JOIN entity_table e ON al.staff_id = e.entity_id " +
                "ORDER BY al.performed_at DESC LIMIT 10";

        ResultSet rs = conn.createStatement().executeQuery(sql);
        boolean any = false;

        while (rs.next()) {
            any = true;
            String name = rs.getString("entity_name");
            String action = rs.getString("action");
            String time = rs.getTimestamp("performed_at") != null
                    ? new java.text.SimpleDateFormat("MMM d HH:mm").format(rs.getTimestamp("performed_at"))
                    : "\u2014";

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                    BorderFactory.createEmptyBorder(6, 4, 6, 4)));

            JLabel nameLbl = new JLabel(name);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            nameLbl.setForeground(C_TEXT);
            nameLbl.setPreferredSize(new Dimension(80, 20));

            JLabel actionLbl = new JLabel(action);
            actionLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            actionLbl.setForeground(C_MUTED);

            JLabel timeLbl = new JLabel(time, SwingConstants.RIGHT);
            timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLbl.setForeground(C_DIM);

            row.add(nameLbl, BorderLayout.WEST);
            row.add(actionLbl, BorderLayout.CENTER);
            row.add(timeLbl, BorderLayout.EAST);
            accessLogList.add(row);
        }

        if (!any) {
            JLabel empty = new JLabel("No access logs yet", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            empty.setForeground(C_DIM);
            accessLogList.add(empty);
        }

        accessLogList.revalidate();
        accessLogList.repaint();
    }

    // ─────────────────────────────────────────────────────────────────
    // Add User Dialog
    // ─────────────────────────────────────────────────────────────────
    private void openAddUserDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Add New User", true);
        dialog.getContentPane().setBackground(C_CARD);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(C_CARD);
        form.setBorder(new EmptyBorder(20, 20, 10, 20));

        JTextField nameF = createDialogField();
        JTextField emailF = createDialogField();
        JTextField phoneF = createDialogField();
        JPasswordField passF = new JPasswordField();
        styleDialogField(passF);

        String[] roles = { "Admin", "Senior_Staff", "Staff" };
        JComboBox<String> roleBox = new JComboBox<>(roles);
        roleBox.setBackground(C_INPUT_BG);
        roleBox.setForeground(C_TEXT);
        roleBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        addDialogRow(form, 0, "Username", nameF);
        addDialogRow(form, 1, "Email", emailF);
        addDialogRow(form, 2, "Phone", phoneF);
        addDialogRow(form, 3, "Passcode", passF);
        addDialogRow(form, 4, "Role", roleBox);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(C_CARD);

        JButton cancelBtn = makeActionButton("Cancel", new Color(0x3A3A3A), true);
        JButton saveBtn = makeActionButton("Save", C_ACCENT, true);
        saveBtn.setPreferredSize(new Dimension(90, 34));
        cancelBtn.setPreferredSize(new Dimension(90, 34));

        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            String name = nameF.getText().trim();
            String email = emailF.getText().trim();
            String phone = phoneF.getText().trim();
            String pass = new String(passF.getPassword()).trim();
            String role = (String) roleBox.getSelectedItem();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill in all fields.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean success = insertUser(name, email, phone, pass, role);
            if (success) {
                logAccess(currentEntityId, "Added user: " + name);
                dialog.dispose();
                refreshAll();
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addDialogRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = row;
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(8, 0, 8, 12);
        form.add(makeDialogLabel(label), lc);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1;
        fc.gridy = row;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1.0;
        fc.insets = new Insets(8, 0, 8, 0);
        field.setPreferredSize(new Dimension(220, 34));
        form.add(field, fc);
    }

    // ─────────────────────────────────────────────────────────────────
    // Edit User Dialog
    // ─────────────────────────────────────────────────────────────────
    private void openEditDialog(int entityId, String name, String email, String phone, String role) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Edit User", true);
        dialog.getContentPane().setBackground(C_CARD);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(C_CARD);
        form.setBorder(new EmptyBorder(20, 20, 10, 20));

        JTextField nameF = createDialogField();
        nameF.setText(name);
        JTextField emailF = createDialogField();
        emailF.setText(email);
        JTextField phoneF = createDialogField();
        phoneF.setText(phone);
        JPasswordField passF = new JPasswordField();
        styleDialogField(passF);
        passF.setToolTipText("Leave blank to keep current passcode");

        String[] roles = { "Admin", "Senior_Staff", "Staff" };
        JComboBox<String> roleBox = new JComboBox<>(roles);
        roleBox.setSelectedItem(role);
        roleBox.setBackground(C_INPUT_BG);
        roleBox.setForeground(C_TEXT);
        roleBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        if ("Senior_Staff".equals(currentRole)) {
            nameF.setEditable(false);
            emailF.setEditable(false);
            phoneF.setEditable(false);
            passF.setEditable(false);
            roleBox.setEnabled(false);
        }

        addDialogRow(form, 0, "Username", nameF);
        addDialogRow(form, 1, "Email", emailF);
        addDialogRow(form, 2, "Phone", phoneF);
        addDialogRow(form, 3, "Passcode", passF);
        addDialogRow(form, 4, "Role", roleBox);

        if ("Admin".equals(currentRole) && "Senior_Staff".equals(role)) {
            boolean currentlyGranted = PermissionManager.hasGrantedPermission(entityId, "manage_users");
            JCheckBox grantCheck = new JCheckBox("Grant Identity/Users access");
            grantCheck.setSelected(currentlyGranted);
            grantCheck.setOpaque(false);
            grantCheck.setForeground(C_TEXT);
            grantCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            GridBagConstraints chkGbc = new GridBagConstraints();
            chkGbc.gridx = 0;
            chkGbc.gridy = 5;
            chkGbc.gridwidth = 2;
            chkGbc.anchor = GridBagConstraints.WEST;
            chkGbc.insets = new Insets(8, 0, 8, 0);
            form.add(grantCheck, chkGbc);

            JPanel btnRow = buildEditDialogButtons(dialog, entityId, nameF, emailF, phoneF, passF, roleBox, grantCheck);
            GridBagConstraints btnGbc = new GridBagConstraints();
            btnGbc.gridx = 0;
            btnGbc.gridy = 6;
            btnGbc.gridwidth = 2;
            btnGbc.fill = GridBagConstraints.HORIZONTAL;
            btnGbc.insets = new Insets(16, 0, 0, 0);
            form.add(btnRow, btnGbc);

            dialog.add(form, BorderLayout.CENTER);
            dialog.pack();
            dialog.setMinimumSize(new Dimension(450, 500));
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            return;
        }

        JPanel btnRow = buildEditDialogButtons(dialog, entityId, nameF, emailF, phoneF, passF, roleBox, null);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, 400));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel buildEditDialogButtons(JDialog dialog, int entityId,
            JTextField nameF, JTextField emailF, JTextField phoneF,
            JPasswordField passF, JComboBox<String> roleBox, JCheckBox grantCheck) {

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(C_CARD);

        boolean canEdit = "Admin".equals(currentRole) ||
                PermissionManager.hasGrantedPermission(currentEntityId, "manage_users");

        JButton cancelBtn = makeActionButton("Cancel", new Color(0x3A3A3A), true);
        JButton saveBtn = makeActionButton("Save", C_ACCENT, canEdit);

        cancelBtn.addActionListener(e -> dialog.dispose());

        if (canEdit) {
            saveBtn.addActionListener(e -> {
                String newName = nameF.getText().trim();
                String newEmail = emailF.getText().trim();
                String newPhone = phoneF.getText().trim();
                String newPass = new String(passF.getPassword()).trim();
                String newRole = (String) roleBox.getSelectedItem();

                updateUser(entityId, newName, newEmail, newPhone,
                        newPass.isEmpty() ? null : newPass, newRole);

                if (grantCheck != null) {
                    updateGrantedPermission(entityId, "manage_users", grantCheck.isSelected());
                }

                logAccess(currentEntityId, "Updated user #" + entityId);
                dialog.dispose();
                refreshAll();
            });
        }

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        return btnRow;
    }

    // ─────────────────────────────────────────────────────────────────
    // Database Operations
    // ─────────────────────────────────────────────────────────────────
    private boolean insertUser(String name, String email, String phone, String pass, String role) {
        String sql = "INSERT INTO entity_table (entity_name, entity_email, `entity#`, entity_passcode, entity_role) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, pass);
            ps.setString(5, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error adding user:\n" + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void updateUser(int entityId, String name, String email, String phone, String passcode, String role) {
        StringBuilder sql = new StringBuilder(
                "UPDATE entity_table SET entity_name=?, entity_email=?, `entity#`=?, entity_role=?");
        if (passcode != null && !passcode.isEmpty()) {
            sql.append(", entity_passcode=?");
        }
        sql.append(" WHERE entity_id=?");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, name);
            ps.setString(idx++, email);
            ps.setString(idx++, phone);
            ps.setString(idx++, role);
            if (passcode != null && !passcode.isEmpty()) {
                ps.setString(idx++, passcode);
            }
            ps.setInt(idx, entityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error updating user:\n" + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeUser(int entityId) {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM entity_table WHERE entity_id=?")) {
            ps.setInt(1, entityId);
            ps.executeUpdate();
            logAccess(currentEntityId, "Removed user #" + entityId);
            refreshAll();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error removing user:\n" + e.getMessage(),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateGrantedPermission(int entityId, String permissionName, boolean granted) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (granted) {
                String upsert = "INSERT INTO granted_permissions (entity_id, permission_name) VALUES (?,?) " +
                        "ON DUPLICATE KEY UPDATE permission_name = ?";
                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setInt(1, entityId);
                    ps.setString(2, permissionName);
                    ps.setString(3, permissionName);
                    ps.executeUpdate();
                }
            } else {
                String delete = "DELETE FROM granted_permissions WHERE entity_id=? AND permission_name=?";
                try (PreparedStatement ps = conn.prepareStatement(delete)) {
                    ps.setInt(1, entityId);
                    ps.setString(2, permissionName);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void logAccess(int staffId, String action) {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO access_log (staff_id, action, performed_at) VALUES (?, ?, NOW())")) {
            ps.setInt(1, staffId);
            ps.setString(2, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper UI methods
    // ─────────────────────────────────────────────────────────────────
    private JLabel makeDetailLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setForeground(C_DIM);
        return lbl;
    }

    private JLabel makeDetailValue(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(C_TEXT);
        return lbl;
    }

    private JButton makeActionButton(String text, Color bg, boolean enabled) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setEnabled(enabled);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JTextField createDialogField() {
        JTextField field = new JTextField();
        field.setBackground(C_INPUT_BG);
        field.setForeground(C_TEXT);
        field.setCaretColor(C_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return field;
    }

    private void styleDialogField(JTextField field) {
        field.setBackground(C_INPUT_BG);
        field.setForeground(C_TEXT);
        field.setCaretColor(C_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    private JLabel makeDialogLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(C_TEXT);
        return lbl;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                result.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}