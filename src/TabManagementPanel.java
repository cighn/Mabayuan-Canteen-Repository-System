import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class TabManagementPanel extends JPanel {

    // ─────────────────────────────────────────────────────────────────
    // Color Palette (mirrors system palette exactly)
    // ─────────────────────────────────────────────────────────────────
    private static final Color C_BG = new Color(0x1E2E1E);
    private static final Color C_CARD = new Color(0x243824);
    private static final Color C_CARD2 = new Color(0x163320);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);
    private static final Color C_PENDING = new Color(0xF59E0B);
    private static final Color C_PAID = new Color(0x22C55E);
    private static final Color C_CANCEL = new Color(0xEF4444);
    private static final Color C_SIDEBAR = new Color(0x1A2E1A);

    // ─────────────────────────────────────────────────────────────────
    // Stat card labels
    // ─────────────────────────────────────────────────────────────────
    private JLabel lblActiveTabs = new JLabel("0");
    private JLabel lblPendingCharges = new JLabel("0");
    private JLabel lblTotalBilled = new JLabel("₱0");
    private JLabel lblTeachersListed = new JLabel("0");

    // ─────────────────────────────────────────────────────────────────
    // Grid panel that holds teacher tab cards
    // ─────────────────────────────────────────────────────────────────
    private JPanel cardsGrid;
    private JTextField searchField;
    private JCheckBox showAllCheckbox;

    // ─────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────
    public TabManagementPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(C_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        // Auto-refresh when panel becomes visible
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshAll();
            }
        });

        refreshAll();
    }

    // ─────────────────────────────────────────────────────────────────
    // Header: stat cards row
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        // Page title
        JLabel title = new JLabel("Tab Management / Management");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(C_TEXT);
        wrapper.add(title, BorderLayout.NORTH);

        // 4 stat cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 88));

        cards.add(buildStatCard("ACTIVE TABS", lblActiveTabs, "this week", C_ACCENT, "🟢"));
        cards.add(buildStatCard("PENDING CHARGES", lblPendingCharges, "unpaid", C_PENDING, "⏳"));
        cards.add(buildStatCard("TOTAL BILLED", lblTotalBilled, "this month", new Color(0x3B82F6), "💸"));
        cards.add(buildStatCard("TEACHERS LISTED", lblTeachersListed, "unique accounts", new Color(0xA855F7), "👨"));

        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildStatCard(String label, JLabel valueLabel, String sub, Color accent, String icon) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD2);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                // left accent bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 14, 2, 14);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Top row: label + icon
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 9));
        lbl.setForeground(C_MUTED);
        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLbl.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
        topRow.add(lbl, BorderLayout.WEST);
        topRow.add(iconLbl, BorderLayout.EAST);
        card.add(topRow, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 14, 2, 14);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 26));
        valueLabel.setForeground(C_TEXT);
        card.add(valueLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 14, 10, 14);
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(new Font("Arial", Font.PLAIN, 10));
        subLbl.setForeground(C_MUTED);
        card.add(subLbl, gbc);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Body: toolbar + 2-column card grid
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        // ── Toolbar ─────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout(0, 0));
        toolbar.setOpaque(false);

        JPanel leftToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftToolbar.setOpaque(false);

        JLabel sectionTitle = new JLabel("Teacher Tabs");
        sectionTitle.setFont(new Font("Arial", Font.BOLD, 15));
        sectionTitle.setForeground(C_TEXT);

        JLabel sectionSub = new JLabel("  Pending charges per teacher");
        sectionSub.setFont(new Font("Arial", Font.PLAIN, 11));
        sectionSub.setForeground(C_MUTED);

        leftToolbar.add(sectionTitle);
        leftToolbar.add(sectionSub);

        JPanel rightToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightToolbar.setOpaque(false);

        // Search field
        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 12));
        searchField.setPreferredSize(new Dimension(200, 34));
        searchField.setBackground(C_CARD2);
        searchField.setForeground(C_TEXT);
        searchField.setCaretColor(C_TEXT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        // Placeholder behaviour
        String ph = "Search teacher...";
        searchField.setForeground(C_MUTED);
        searchField.setText(ph);
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(ph)) {
                    searchField.setText("");
                    searchField.setForeground(C_TEXT);
                }
            }

            public void focusLost(FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setForeground(C_MUTED);
                    searchField.setText(ph);
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String kw = searchField.getText().equals(ph) ? "" : searchField.getText().trim();
                filterCards(kw);
            }
        });

        // Show Paid / Cancelled tabs
        showAllCheckbox = new JCheckBox("Show Paid/Cancelled");
        showAllCheckbox.setFont(new Font("Arial", Font.PLAIN, 12));
        showAllCheckbox.setForeground(C_TEXT);
        showAllCheckbox.setBackground(C_BG);
        showAllCheckbox.setFocusPainted(false);
        showAllCheckbox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showAllCheckbox.addActionListener(e -> {
            // Reload tabs with new filter setting
            String kw = searchField.getText().equals(ph) ? "" : searchField.getText().trim();
            loadTabCards(kw);
        });

        JButton newTabBtn = makeButton("+ New Tab", C_ACCENT);
        newTabBtn.addActionListener(e -> openNewTabWindow());

        rightToolbar.add(searchField);
        rightToolbar.add(showAllCheckbox);
        rightToolbar.add(newTabBtn);

        toolbar.add(leftToolbar, BorderLayout.WEST);
        toolbar.add(rightToolbar, BorderLayout.EAST);

        // ── Cards Grid (2-column, scrollable) ───────────────────────
        cardsGrid = new JPanel(new GridLayout(0, 2, 12, 12));
        cardsGrid.setOpaque(false);

        JScrollPane scroll = new JScrollPane(cardsGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        return body;
    }

    // ─────────────────────────────────────────────────────────────────
    // Refresh: stat cards + tab cards
    // ─────────────────────────────────────────────────────────────────
    void refreshAll() {
        refreshStatCards();
        String kw = searchField.getText().trim();
        // If search field shows placeholder, treat as empty
        String ph = "Search teacher...";
        if (kw.equals(ph))
            kw = "";
        loadTabCards(kw);
    }

    private void refreshStatCards() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            // Active Tabs = all tabs not cancelled
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM tabs t " +
                            "JOIN tabs_status ts ON t.status = ts.id " +
                            "WHERE LOWER(ts.tabs_status_name) != 'cancelled'");
            if (rs.next())
                lblActiveTabs.setText(String.valueOf(rs.getInt(1)));

            // Pending charges = tabs with status Pending
            rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM tabs t " +
                            "JOIN tabs_status ts ON t.status = ts.id " +
                            "WHERE LOWER(ts.tabs_status_name) = 'pending'");
            if (rs.next())
                lblPendingCharges.setText(String.valueOf(rs.getInt(1)));

            // Total billed = sum of all tab totals
            rs = conn.createStatement().executeQuery(
                    "SELECT COALESCE(SUM(total), 0) FROM tabs");
            if (rs.next())
                lblTotalBilled.setText(String.format("₱%,.0f", rs.getDouble(1)));

            // Teachers listed = distinct teachers with at least one tab
            rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(DISTINCT teacher_id) FROM tabs");
            if (rs.next())
                lblTeachersListed.setText(String.valueOf(rs.getInt(1)));

        } catch (SQLException ex) {
            System.err.println("Tab stat card error: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Load tab cards from DB, optionally filtered by teacher name + status
    // ─────────────────────────────────────────────────────────────────
    private void loadTabCards(String keyword) {
        cardsGrid.removeAll();

        boolean showAll = showAllCheckbox.isSelected();

        StringBuilder sql = new StringBuilder(
                "SELECT t.id, t.total, t.created_at, tc.teacher_name, ts.tabs_status_name " +
                        "FROM tabs t " +
                        "JOIN teacher tc ON t.teacher_id = tc.id " +
                        "JOIN tabs_status ts ON t.status = ts.id ");

        java.util.List<String> conditions = new ArrayList<>();
        java.util.List<Object> params = new ArrayList<>();

        if (!keyword.isBlank()) {
            conditions.add("LOWER(tc.teacher_name) LIKE ?");
            params.add("%" + keyword.toLowerCase() + "%");
        }

        if (!showAll) {
            // Show only pending tabs (unpaid)
            conditions.add("LOWER(ts.tabs_status_name) = 'pending'");
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        sql.append(" ORDER BY t.created_at DESC");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();
            boolean any = false;

            while (rs.next()) {
                any = true;
                int tabId = rs.getInt("id");
                String teacher = rs.getString("teacher_name");
                double total = rs.getDouble("total");
                String statusName = rs.getString("tabs_status_name");
                Timestamp createdAt = rs.getTimestamp("created_at");

                // Fetch items for this tab
                List<String> itemPills = loadTabItems(conn, tabId);

                JPanel card = buildTabCard(tabId, teacher, total, statusName, createdAt, itemPills);
                card.setPreferredSize(new Dimension(320, 0)); // fixed width, variable height
                cardsGrid.add(card);
            }

            if (!any) {
                JLabel empty = new JLabel("No tabs found.", SwingConstants.CENTER);
                empty.setFont(new Font("Arial", Font.PLAIN, 14));
                empty.setForeground(C_MUTED);
                cardsGrid.add(empty);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    private List<String> loadTabItems(Connection conn, int tabId) throws SQLException {
        List<String> pills = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT p.Item_name, ti.quantity " +
                        "FROM tabs_items ti " +
                        "JOIN product_list p ON ti.product_id = p.Item_id " +
                        "WHERE ti.tab_id = ? ORDER BY ti.id");
        ps.setInt(1, tabId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            pills.add(rs.getString("Item_name") + " ×" + rs.getInt("quantity"));
        }
        ps.close();
        return pills;
    }

    // ─────────────────────────────────────────────────────────────────
    // Build a single teacher tab card (matches reference screenshot)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildTabCard(int tabId, String teacherName, double total,
            String statusName, Timestamp createdAt,
            List<String> itemPills) {

        // Determine status colour
        Color statusColor = switch (statusName.toLowerCase()) {
            case "paid" -> C_PAID;
            case "cancelled" -> C_CANCEL;
            default -> C_PENDING;
        };

        // Card container
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        // ── Top row: teacher name (left) + date (right) ─────────────
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel nameLabel = new JLabel(teacherName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Increased font size
        nameLabel.setForeground(C_TEXT);

        String dateStr = createdAt != null
                ? new SimpleDateFormat("MMM d, yyyy").format(createdAt)
                : "—";
        JLabel dateLabel = new JLabel(dateStr);
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        dateLabel.setForeground(C_MUTED);

        topRow.add(nameLabel, BorderLayout.WEST);
        topRow.add(dateLabel, BorderLayout.EAST);

        // ── Sub row: tab number badge ────────────────────────────────
        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        subRow.setOpaque(false);
        String tabNum = String.format("Tab #%03d", tabId);
        JLabel tabBadge = new JLabel("# " + tabNum);
        tabBadge.setFont(new Font("Arial", Font.PLAIN, 11));
        tabBadge.setForeground(C_MUTED);
        subRow.add(tabBadge);

        // ── Product items row (better display) ────────────────────────────
        JPanel itemsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        itemsPanel.setOpaque(false);

        if (itemPills.isEmpty()) {
            JLabel noneLabel = new JLabel("No items yet");
            noneLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            noneLabel.setForeground(C_MUTED);
            itemsPanel.add(noneLabel);
        } else {
            int shown = 0;
            for (String pill : itemPills) {
                if (shown >= 5) {
                    JLabel moreLabel = new JLabel("+" + (itemPills.size() - 5) + " more");
                    moreLabel.setFont(new Font("Arial", Font.BOLD, 11));
                    moreLabel.setForeground(C_ACCENT);
                    itemsPanel.add(moreLabel);
                    break;
                }
                JLabel itemLabel = new JLabel(pill);
                itemLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Increased font size
                itemLabel.setForeground(C_TEXT);
                itemLabel.setBackground(new Color(0x2A4A35));
                itemLabel.setOpaque(true);
                itemLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                itemsPanel.add(itemLabel);
                shown++;
            }
        }

        // ── Bottom row: total + item count (left), buttons (right) ─
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        // Left: total price + item count
        JPanel leftBottom = new JPanel(new BorderLayout(0, 4));
        leftBottom.setOpaque(false);
        JLabel totalLabel = new JLabel(String.format("₱%.0f", total));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 20));
        totalLabel.setForeground(C_PAID);
        JLabel itemCountLabel = new JLabel(itemPills.size() + " item(s) charged");
        itemCountLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        itemCountLabel.setForeground(C_MUTED);
        leftBottom.add(totalLabel, BorderLayout.NORTH);
        leftBottom.add(itemCountLabel, BorderLayout.SOUTH);

        // Right: View + status button
        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBottom.setOpaque(false);

        JButton viewBtn = createStyledButton("View", new Color(0x2A4A35), Color.WHITE);
        viewBtn.setPreferredSize(new Dimension(80, 34));
        viewBtn.addActionListener(e -> openViewWindow(tabId, teacherName));

        boolean isPaid = statusName.equalsIgnoreCase("paid");
        JButton statusBtn = createStyledButton(
                isPaid ? "Paid" : "Mark Paid",
                isPaid ? C_PAID : C_ACCENT,
                Color.WHITE);
        statusBtn.setPreferredSize(new Dimension(110, 34));

        statusBtn.addActionListener(e -> {
            if (isPaid) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Mark this tab as Pending again?", "Revert Status",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    updateTabStatus(tabId, "Pending");
                    refreshAll();
                }
            } else {
                JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
                new TabPaymentWindow(parent, tabId, teacherName, total, this::refreshAll);
            }
        });

        rightBottom.add(viewBtn);
        rightBottom.add(statusBtn);
        bottomRow.add(leftBottom, BorderLayout.WEST);
        bottomRow.add(rightBottom, BorderLayout.EAST);

        // ── Assemble card ────────────────────────────────────────────
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(subRow);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(itemsPanel);

        card.add(topRow, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    // Add this helper method to TabManagementPanel
    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────
    // Pill tag label
    // ─────────────────────────────────────────────────────────────────
    private JLabel makePill(String text, Color bg, Color fg) {
        JLabel pill = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(new Font("Arial", Font.PLAIN, 11));
        pill.setForeground(fg);
        pill.setBackground(bg);
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return pill;
    }

    // ─────────────────────────────────────────────────────────────────
    // Filter cards live
    // ─────────────────────────────────────────────────────────────────
    private void filterCards(String keyword) {
        loadTabCards(keyword);
    }

    // ─────────────────────────────────────────────────────────────────
    // Update tab status in DB
    // ─────────────────────────────────────────────────────────────────
    private void updateTabStatus(int tabId, String statusName) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement getStatus = conn.prepareStatement(
                    "SELECT id FROM tabs_status WHERE tabs_status_name = ?");
            getStatus.setString(1, statusName);
            ResultSet rs = getStatus.executeQuery();
            if (rs.next()) {
                int statusId = rs.getInt("id");
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tabs SET status = ? WHERE id = ?");
                ps.setInt(1, statusId);
                ps.setInt(2, tabId);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Open new tab window
    // ─────────────────────────────────────────────────────────────────
    private void openNewTabWindow() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (parent instanceof Dashboard) {
            Dashboard dashboard = (Dashboard) parent;
            new TabAddWindow(parent, dashboard::refreshAllPanels);
        } else {
            new TabAddWindow(parent, this::refreshAll);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Open tab view / detail window
    // ─────────────────────────────────────────────────────────────────
    private void openViewWindow(int tabId, String teacherName) {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new TabViewWindow(parent, tabId, teacherName, this::refreshAll);
    }

    // ─────────────────────────────────────────────────────────────────
    // Button helper (mirrors StockSupplyPanel.styleButton)
    // ─────────────────────────────────────────────────────────────────
    private JButton makeButton(String label, Color bg) {
        JButton btn = new JButton(label);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 34));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}