import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class StockSupplyPanel extends JPanel {

    // ─────────────────────────────────────────────────────────────────
    // Color Palette (mirrors Dashboard/ProductsPanel)
    // ─────────────────────────────────────────────────────────────────
    private static final Color C_BG = new Color(0x1E2E1E);
    private static final Color C_CARD = new Color(0x163320);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);
    private static final Color C_PENDING = new Color(0xF59E0B);
    private static final Color C_COMPLETE = new Color(0x22C55E);
    private static final Color C_CANCEL = new Color(0xEF4444);

    // ─────────────────────────────────────────────────────────────────
    // Table
    // ─────────────────────────────────────────────────────────────────
    private static final String[] COLUMNS = {
            "ID", "Log Title", "Supplier", "Recorded By", "Total Cost", "Date", "Status", "Category", "Actions"
    };
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // ─────────────────────────────────────────────────────────────────
    // Stat card values (updated on refresh)
    // ─────────────────────────────────────────────────────────────────
    private JLabel lblTotalReplenishments = new JLabel("0");
    private JLabel lblPending = new JLabel("0");
    private JLabel lblTotalCost = new JLabel("₱0");
    private JLabel lblLowStock = new JLabel("0");

    public StockSupplyPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(C_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // ── Header row ──────────────────────────────────────────────
        add(buildHeader(), BorderLayout.NORTH);

        // ── Main split: table LEFT, alerts RIGHT ────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildTableSection(), buildRightSection());
        split.setResizeWeight(0.65);
        split.setDividerSize(8);
        split.setBackground(C_BG);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        refreshAll();
    }

    // ─────────────────────────────────────────────────────────────────
    // Header: stat cards + page title (fixed: left‑aligned + visible)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        // Page title
        JLabel title = new JLabel("Stock / Supply  /  Inventory");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(C_TEXT);
        wrapper.add(title, BorderLayout.NORTH);

        // 4 stat cards - using GridBagLayout for proper width distribution
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // Each column gets equal weight
        gbc.insets = new Insets(0, 6, 0, 6); // Add spacing between cards

        // Create and add all 4 cards with equal distribution
        JPanel card1 = buildStatCard("TOTAL REPLENISHMENTS", lblTotalReplenishments, "this quarter", C_ACCENT);
        JPanel card2 = buildStatCard("PENDING", lblPending, "awaiting delivery", C_PENDING);
        JPanel card3 = buildStatCard("TOTAL COST", lblTotalCost, "this quarter", new Color(0x3B82F6));
        JPanel card4 = buildStatCard("LOW STOCK ALERTS", lblLowStock, "urgent restock", C_CANCEL);

        gbc.gridx = 0;
        cards.add(card1, gbc);
        gbc.gridx = 1;
        cards.add(card2, gbc);
        gbc.gridx = 2;
        cards.add(card3, gbc);
        gbc.gridx = 3;
        cards.add(card4, gbc);

        // Make all cards have the same preferred width
        int maxWidth = Math.max(Math.max(card1.getPreferredSize().width, card2.getPreferredSize().width),
                Math.max(card3.getPreferredSize().width, card4.getPreferredSize().width));
        Dimension fixedSize = new Dimension(maxWidth + 20, 95); // Add some padding

        card1.setPreferredSize(fixedSize);
        card1.setMinimumSize(fixedSize);
        card2.setPreferredSize(fixedSize);
        card2.setMinimumSize(fixedSize);
        card3.setPreferredSize(fixedSize);
        card3.setMinimumSize(fixedSize);
        card4.setPreferredSize(fixedSize);
        card4.setMinimumSize(fixedSize);

        wrapper.add(cards, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildStatCard(String label, JLabel valueLabel, String sub, Color accent) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
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
        gbc.anchor = GridBagConstraints.WEST; // LEFT ALIGN
        gbc.fill = GridBagConstraints.HORIZONTAL; // Make text stretch horizontally
        gbc.weightx = 1.0; // Take full width
        gbc.insets = new Insets(12, 14, 2, 14);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 10));
        lbl.setForeground(C_MUTED);
        card.add(lbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 14, 2, 14);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setForeground(C_TEXT);
        card.add(valueLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 14, 12, 14);
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(new Font("Arial", Font.PLAIN, 10));
        subLbl.setForeground(C_MUTED);
        card.add(subLbl, gbc);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Left section: Replenishment Log table
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildTableSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Sub-header row
        JPanel subHeader = new JPanel(new BorderLayout());
        subHeader.setOpaque(false);
        subHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleGroup.setOpaque(false);
        JLabel logTitle = new JLabel("Replenishment Log");
        logTitle.setFont(new Font("Arial", Font.BOLD, 14));
        logTitle.setForeground(C_TEXT);
        JLabel logSub = new JLabel("  All supply / stock processes");
        logSub.setFont(new Font("Arial", Font.PLAIN, 11));
        logSub.setForeground(C_MUTED);
        titleGroup.add(logTitle);
        titleGroup.add(logSub);

        JButton addBtn = new JButton("+ New Entry");
        styleButton(addBtn, C_ACCENT);
        addBtn.addActionListener(e -> openAddWindow());

        JButton refreshBtn = new JButton("Refresh");
        styleButton(refreshBtn, new Color(0x1B5E3B));
        refreshBtn.addActionListener(e -> refreshAll());

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(refreshBtn);
        rightBtns.add(addBtn);

        subHeader.add(titleGroup, BorderLayout.WEST);
        subHeader.add(rightBtns, BorderLayout.EAST);
        panel.add(subHeader, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        styleTable();

        // Click handler for Actions column (index 8)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == 8) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        int modelRow = table.convertRowIndexToModel(row);
                        int cellX = table.getCellRect(row, col, true).x;
                        int half = cellX + (table.getColumnModel().getColumn(col).getWidth() / 2);
                        if (e.getX() < half) {
                            openEditWindow(modelRow);
                        } else {
                            deleteEntry(modelRow);
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ─────────────────────────────────────────────────────────────────
    // Right section: Low Stock Alerts + Cost Breakdown
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildRightSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        panel.add(buildLowStockPanel());
        panel.add(Box.createVerticalStrut(12));
        panel.add(buildCostBreakdownPanel());

        return panel;
    }

    private JPanel buildLowStockPanel() {
        JPanel card = buildCard("Low Stock Alerts");
        card.setPreferredSize(new Dimension(0, 250));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        card.add(listPanel, BorderLayout.CENTER);
        card.putClientProperty("lowStockList", listPanel);
        return card;
    }

    private JPanel buildCostBreakdownPanel() {
        JPanel card = buildCard("Cost Breakdown");
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        card.add(listPanel, BorderLayout.CENTER);
        card.putClientProperty("costList", listPanel);
        return card;
    }

    /** Reusable dark rounded card with a title */
    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(C_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setForeground(C_TEXT);
        card.add(lbl, BorderLayout.NORTH);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Table Styling (mirrors ProductsPanel)
    // ─────────────────────────────────────────────────────────────────
    private void styleTable() {
        JTableHeader header = table.getTableHeader();
        header.setBackground(C_ACCENT);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getWidth(), 38));

        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(36);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(8, 1));
        table.setSelectionBackground(new Color(200, 230, 200));
        table.setSelectionForeground(Color.BLACK);

        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(8).setCellRenderer(new ActionsRenderer());

        // Column widths
        int[] widths = { 40, 160, 130, 100, 90, 110, 90, 100, 90 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        table.getColumnModel().getColumn(4).setCellRenderer(center);
    }

    private class StripedRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
            if (!isSelected)
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return c;
        }
    }

    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            String status = value != null ? value.toString() : "";
            Color bg = switch (status.toLowerCase()) {
                case "completed" -> C_COMPLETE;
                case "pending" -> C_PENDING;
                case "cancelled" -> C_CANCEL;
                default -> C_MUTED;
            };
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, status, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.BOLD, 11));
            lbl.setOpaque(true);
            lbl.setBackground(bg);
            lbl.setForeground(Color.WHITE);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return lbl;
        }
    }

    private class ActionsRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, "Edit    Del",
                    isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.BOLD, 11));
            lbl.setForeground(new Color(70, 130, 200));
            lbl.setOpaque(true);
            if (!isSelected)
                lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            else
                lbl.setBackground(tbl.getSelectionBackground());
            return lbl;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Data Refresh — loads all sections
    // ─────────────────────────────────────────────────────────────────
    private void refreshAll() {
        refreshTable();
        refreshStatCards();
        refreshLowStockAlerts();
        refreshCostBreakdown();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String sql = "SELECT sl.id, sl.log_title, sl.supplier_name, " +
                "e.entity_name, sl.total_cost, sl.created_at, " +
                "ss.status_supply_name, c.categories_name " +
                "FROM supply_log sl " +
                "JOIN entity_table e  ON sl.recorded_by     = e.entity_id " +
                "JOIN supply_status ss ON sl.status          = ss.id " +
                "JOIN categories_table c ON sl.category_restock = c.id " +
                "ORDER BY sl.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("log_title"),
                        rs.getString("supplier_name"),
                        rs.getString("entity_name"),
                        String.format("₱%.2f", rs.getDouble("total_cost")),
                        rs.getTimestamp("created_at") != null
                                ? new java.text.SimpleDateFormat("MMM d, yyyy").format(rs.getTimestamp("created_at"))
                                : "",
                        rs.getString("status_supply_name"),
                        rs.getString("categories_name"),
                        ""
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshStatCards() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Total replenishments
            ResultSet rs1 = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM supply_log");
            if (rs1.next())
                lblTotalReplenishments.setText(String.valueOf(rs1.getInt(1)));

            // Pending
            ResultSet rs2 = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM supply_log sl " +
                            "JOIN supply_status ss ON sl.status = ss.id " +
                            "WHERE LOWER(ss.status_supply_name) = 'pending'");
            if (rs2.next())
                lblPending.setText(String.valueOf(rs2.getInt(1)));

            // Total cost
            ResultSet rs3 = conn.createStatement().executeQuery(
                    "SELECT SUM(total_cost) FROM supply_log");
            if (rs3.next())
                lblTotalCost.setText(String.format("₱%,.0f", rs3.getDouble(1)));

            // Low stock
            ResultSet rs4 = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM product_list WHERE Item_quantity < 100");
            if (rs4.next())
                lblLowStock.setText(String.valueOf(rs4.getInt(1)));

        } catch (SQLException ex) {
            System.err.println("Stat card error: " + ex.getMessage());
        }
    }

    private void refreshLowStockAlerts() {
        // Find the lowStockList panel inside the low stock card
        JPanel lowStockCard = findCardByTitle("Low Stock Alerts");
        if (lowStockCard == null)
            return;
        JPanel listPanel = (JPanel) lowStockCard.getClientProperty("lowStockList");
        if (listPanel == null)
            return;
        listPanel.removeAll();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT p.Item_name, p.Item_quantity, c.categories_name " +
                    "FROM product_list p JOIN categories_table c ON p.Item_category = c.id " +
                    "WHERE p.Item_quantity < 100 ORDER BY p.Item_quantity ASC LIMIT 6";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            boolean found = false;
            while (rs.next()) {
                found = true;
                String name = rs.getString("Item_name");
                int qty = rs.getInt("Item_quantity");
                String cat = rs.getString("categories_name");

                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

                JLabel nameLbl = new JLabel("<html><b style='color:#ECFEF5'>" + name + "</b>" +
                        "<br><span style='color:#7AAA8A;font-size:10px'>Only <b style='color:#EF4444'>"
                        + qty + " pcs</b> left · " + cat + "</span></html>");
                nameLbl.setFont(new Font("Arial", Font.PLAIN, 12));
                row.add(nameLbl, BorderLayout.CENTER);
                listPanel.add(row);
            }
            if (!found) {
                JLabel ok = new JLabel("✅  All stock levels are healthy");
                ok.setFont(new Font("Arial", Font.PLAIN, 12));
                ok.setForeground(C_COMPLETE);
                listPanel.add(ok);
            }
        } catch (SQLException ex) {
            System.err.println("Low stock error: " + ex.getMessage());
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void refreshCostBreakdown() {
        JPanel costCard = findCardByTitle("Cost Breakdown");
        if (costCard == null)
            return;
        JPanel listPanel = (JPanel) costCard.getClientProperty("costList");
        if (listPanel == null)
            return;
        listPanel.removeAll();

        Color[] barColors = {
                new Color(0x22C55E), new Color(0xEF4444),
                new Color(0xF59E0B), new Color(0x3B82F6),
                new Color(0xA855F7), new Color(0xEC4899)
        };

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT c.categories_name, SUM(sl.total_cost) AS total " +
                    "FROM supply_log sl JOIN categories_table c ON sl.category_restock = c.id " +
                    "GROUP BY c.categories_name ORDER BY total DESC LIMIT 6";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            // First pass: get max for bar scaling
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            double max = 1;
            while (rs.next()) {
                double total = rs.getDouble("total");
                rows.add(new Object[] { rs.getString("categories_name"), total });
                if (total > max)
                    max = total;
            }

            int colorIdx = 0;
            for (Object[] r : rows) {
                String cat = (String) r[0];
                double total = (double) r[1];
                Color color = barColors[colorIdx++ % barColors.length];

                JPanel row = new JPanel(new BorderLayout(0, 3));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

                JPanel labelRow = new JPanel(new BorderLayout());
                labelRow.setOpaque(false);
                JLabel catLbl = new JLabel(cat);
                catLbl.setFont(new Font("Arial", Font.PLAIN, 12));
                catLbl.setForeground(C_TEXT);
                JLabel costLbl = new JLabel(String.format("₱%,.0f", total));
                costLbl.setFont(new Font("Arial", Font.BOLD, 12));
                costLbl.setForeground(C_TEXT);
                labelRow.add(catLbl, BorderLayout.WEST);
                labelRow.add(costLbl, BorderLayout.EAST);

                int barWidth = (int) (180 * (total / max));
                JPanel bar = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(255, 255, 255, 30));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                        g2.setColor(color);
                        g2.fillRoundRect(0, 0, barWidth, getHeight(), 4, 4);
                        g2.dispose();
                    }
                };
                bar.setOpaque(false);
                bar.setPreferredSize(new Dimension(0, 8));

                row.add(labelRow, BorderLayout.NORTH);
                row.add(bar, BorderLayout.CENTER);
                listPanel.add(row);
            }
        } catch (SQLException ex) {
            System.err.println("Cost breakdown error: " + ex.getMessage());
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    // Helper to find the low-stock or cost card panel by title
    private JPanel findCardByTitle(String title) {
        return findCardRecursive(this, title);
    }

    private JPanel findCardRecursive(Container c, String title) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JPanel panel) {
                Object stored = panel.getClientProperty("lowStockList");
                Object stored2 = panel.getClientProperty("costList");
                if (stored != null && title.contains("Low Stock"))
                    return panel;
                if (stored2 != null && title.equals("Cost Breakdown"))
                    return panel;
                JPanel found = findCardRecursive(panel, title);
                if (found != null)
                    return found;
            } else if (comp instanceof JSplitPane sp) {
                JPanel found = findCardRecursive(sp, title);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────
    // Add / Edit / Delete
    // ─────────────────────────────────────────────────────────────────
    private void openAddWindow() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new StockAddWindow(parent, this::refreshAll);
    }

    private void openEditWindow(int modelRow) {
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String logTitle = (String) tableModel.getValueAt(modelRow, 1);
        String supplier = (String) tableModel.getValueAt(modelRow, 2);
        String costStr = tableModel.getValueAt(modelRow, 4).toString().replace("₱", "").replace(",", "");
        double cost = Double.parseDouble(costStr);
        String status = (String) tableModel.getValueAt(modelRow, 6);
        String category = (String) tableModel.getValueAt(modelRow, 7);

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new StockEditWindow(parent, id, logTitle, supplier, cost, status, category, this::refreshAll);
    }

    private void deleteEntry(int modelRow) {
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String title = (String) tableModel.getValueAt(modelRow, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + title + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM supply_log WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                refreshAll();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────
    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 34));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}