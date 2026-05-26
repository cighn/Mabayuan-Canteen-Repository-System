import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class DashboardPanel extends JPanel {

    private JLabel totalProductsVal, totalCategoriesVal, activeUsersVal,
            lowStockVal, largestCategoryVal, highestPriceVal;
    private JTable productPreviewTable;
    private DefaultTableModel previewModel;
    private JPanel barsPanel;
    private JPanel supplyLogList; // holds supply log entries

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(0x1E2E1E));
        setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 20)); // ← left=0, right=20, top=20, bottom=20

        add(buildStatRow(), BorderLayout.NORTH);
        add(buildBottomRow(), BorderLayout.CENTER);

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshDashboard();
            }
        });
        refreshDashboard();
    }

    // ─────────────────────────────────────────────────────────────────
    // Stat Row (unchanged)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 10, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 90));

        totalProductsVal = new JLabel("—");
        totalCategoriesVal = new JLabel("—");
        activeUsersVal = new JLabel("—");
        lowStockVal = new JLabel("—");
        largestCategoryVal = new JLabel("—");
        highestPriceVal = new JLabel("—");

        row.add(makeStatCard("TOTAL PRODUCTS", totalProductsVal, "items in inventory", new Color(0x2E7D4F)));
        row.add(makeStatCard("CATEGORIES", totalCategoriesVal, "active groups", new Color(0x1565A0)));
        row.add(makeStatCard("ACTIVE USERS", activeUsersVal, "Admin · Staff", new Color(0x6A1B9A)));
        row.add(makeStatCard("LOW STOCK", lowStockVal, "needs replenishment", new Color(0xBF360C)));
        row.add(makeStatCard("LARGEST CATEGORY", largestCategoryVal, "most products", new Color(0x00695C)));
        row.add(makeStatCard("HIGHEST PRICE", highestPriceVal, "top item", new Color(0x4E342E)));
        return row;
    }

    private JPanel makeStatCard(String title, JLabel valueLabel, String sub, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(0x243824));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 2, 12, 12))); // ✅ left padding reduced from 12 to 2

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Arial", Font.BOLD, 10));
        titleLbl.setForeground(new Color(0x8AAA8A));

        valueLabel.setFont(new Font("Arial", Font.BOLD, 26));
        valueLabel.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(new Font("Arial", Font.PLAIN, 10));
        subLbl.setForeground(new Color(0x6A8A6A));

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subLbl);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Bottom Row: Left = Recent Products | Right = Supply Log + Category
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.add(buildProductPreviewPanel());
        row.add(buildRightColumn());
        return row;
    }

    // ── Left: Recent Products (unchanged) ───────────────────────────
    private JPanel buildProductPreviewPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(new Color(0x243824));
        card.setBorder(new EmptyBorder(16, 2, 16, 16)); // ✅ left padding reduced from 16 to 2

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Recent Products");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Last 8 added");
        sub.setFont(new Font("Arial", Font.PLAIN, 11));
        sub.setForeground(new Color(0x6A8A6A));
        header.add(title, BorderLayout.WEST);
        header.add(sub, BorderLayout.EAST);

        String[] cols = { "Name", "Category", "Price", "Stock" };
        previewModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        productPreviewTable = new JTable(previewModel);
        productPreviewTable.setFont(new Font("Arial", Font.PLAIN, 12));
        productPreviewTable.setRowHeight(32);
        productPreviewTable.setBackground(new Color(0x1A2A1A));
        productPreviewTable.setForeground(new Color(0xDDDDDD));
        productPreviewTable.setGridColor(new Color(0x2E4A2E));
        productPreviewTable.setSelectionBackground(new Color(0x2E7D4F));
        productPreviewTable.setShowHorizontalLines(true);
        productPreviewTable.setShowVerticalLines(false);
        productPreviewTable.getTableHeader().setBackground(new Color(0x1A3A1A));
        productPreviewTable.getTableHeader().setForeground(new Color(0x7DC97D));
        productPreviewTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));

        productPreviewTable.setDefaultRenderer(Object.class, (tbl, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val == null ? "" : val.toString());
            lbl.setOpaque(true);
            lbl.setFont(new Font("Arial", Font.PLAIN, 12));
            lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
            if (sel) {
                lbl.setBackground(new Color(0x2E7D4F));
                lbl.setForeground(Color.WHITE);
            } else {
                Object stockVal = tbl.getModel().getValueAt(row, 3);
                int stock = stockVal instanceof Integer ? (Integer) stockVal : 0;
                lbl.setBackground(row % 2 == 0 ? new Color(0x1A2A1A) : new Color(0x1E301E));
                lbl.setForeground(stock < 30
                        ? new Color(0xD85A30)
                        : stock < 100
                                ? new Color(0xF9A825)
                                : new Color(0xDDDDDD));
            }
            return lbl;
        });

        JScrollPane scroll = new JScrollPane(productPreviewTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x2E4A2E)));
        scroll.getViewport().setBackground(new Color(0x1A2A1A));

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── Right Column: Supply Log (top) + Category Breakdown (bottom) ─
    private JPanel buildRightColumn() {
        JPanel col = new JPanel(new GridLayout(2, 1, 0, 12));
        col.setOpaque(false);
        col.add(buildSupplyLogPanel());
        col.add(buildCategorySummaryPanel());
        return col;
    }

    // ── Supply Log Panel ─────────────────────────────────────────────
    private JPanel buildSupplyLogPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(new Color(0x243824));
        card.setBorder(new EmptyBorder(16, 2, 16, 16)); // ✅ left padding reduced from 16 to 2

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Supply Log");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Recent replenishments");
        sub.setFont(new Font("Arial", Font.PLAIN, 11));
        sub.setForeground(new Color(0x6A8A6A));
        header.add(title, BorderLayout.WEST);
        header.add(sub, BorderLayout.EAST);

        supplyLogList = new JPanel();
        supplyLogList.setLayout(new BoxLayout(supplyLogList, BoxLayout.Y_AXIS));
        supplyLogList.setBackground(new Color(0x243824));

        JScrollPane scroll = new JScrollPane(supplyLogList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x2E4A2E)));
        scroll.getViewport().setBackground(new Color(0x243824));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── Category Breakdown (unchanged logic) ────────────────────────
    private JPanel buildCategorySummaryPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(new Color(0x243824));
        card.setBorder(new EmptyBorder(16, 2, 16, 16)); // ✅ left padding reduced from 16 to 2

        JLabel title = new JLabel("Category Breakdown");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(Color.WHITE);
        card.add(title, BorderLayout.NORTH);

        barsPanel = new JPanel();
        barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
        barsPanel.setBackground(new Color(0x243824));
        card.add(barsPanel, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Refresh All
    // ─────────────────────────────────────────────────────────────────
    void refreshDashboard() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM product_list");
            if (rs.next())
                totalProductsVal.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM categories_table");
            if (rs.next())
                totalCategoriesVal.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM entity_table");
            if (rs.next())
                activeUsersVal.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM product_list WHERE Item_quantity < 50");
            if (rs.next())
                lowStockVal.setText(String.valueOf(rs.getInt(1)));

            rs = conn.createStatement().executeQuery(
                    "SELECT c.categories_name, COUNT(p.Item_id) as cnt " +
                            "FROM categories_table c " +
                            "LEFT JOIN product_list p ON c.id = p.Item_category " +
                            "GROUP BY c.id, c.categories_name ORDER BY cnt DESC LIMIT 1");
            if (rs.next())
                largestCategoryVal.setText(rs.getString("categories_name"));

            rs = conn.createStatement()
                    .executeQuery("SELECT MAX(Item_price) FROM product_list");
            if (rs.next())
                highestPriceVal.setText(String.format("₱%.0f", rs.getDouble(1)));

            // Recent Products
            rs = conn.createStatement().executeQuery(
                    "SELECT p.Item_name, c.categories_name, p.Item_price, p.Item_quantity " +
                            "FROM product_list p " +
                            "JOIN categories_table c ON p.Item_category = c.id " +
                            "ORDER BY p.Item_id DESC LIMIT 8");
            previewModel.setRowCount(0);
            while (rs.next()) {
                previewModel.addRow(new Object[] {
                        rs.getString("Item_name"),
                        rs.getString("categories_name"),
                        String.format("₱%.0f", rs.getDouble("Item_price")),
                        rs.getInt("Item_quantity")
                });
            }

            refreshCategoryBars(conn);
            refreshSupplyLog(conn);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Supply Log entries
    // ─────────────────────────────────────────────────────────────────
    private void refreshSupplyLog(Connection conn) throws SQLException {
        supplyLogList.removeAll();

        String sql = "SELECT sl.log_title, sl.supplier_name, e.entity_name, " +
                "sl.total_cost, sl.created_at, ss.status_supply_name " +
                "FROM supply_log sl " +
                "JOIN entity_table e   ON sl.recorded_by      = e.entity_id " +
                "JOIN supply_status ss ON sl.status            = ss.id " +
                "ORDER BY sl.created_at DESC LIMIT 5";

        ResultSet rs = conn.createStatement().executeQuery(sql);
        boolean any = false;

        while (rs.next()) {
            any = true;
            String logTitle = rs.getString("log_title");
            String supplier = rs.getString("supplier_name");
            String recorder = rs.getString("entity_name");
            double cost = rs.getDouble("total_cost");
            String status = rs.getString("status_supply_name");
            String date = rs.getTimestamp("created_at") != null
                    ? new java.text.SimpleDateFormat("MMM d").format(rs.getTimestamp("created_at"))
                    : "";

            Color statusColor = switch (status.toLowerCase()) {
                case "completed" -> new Color(0x22C55E);
                case "pending" -> new Color(0xF59E0B);
                case "cancelled" -> new Color(0xEF4444);
                default -> new Color(0x7AAA8A);
            };

            // Row panel
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2E4A2E)),
                    BorderFactory.createEmptyBorder(6, 4, 6, 4)));

            // Left: color dot
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Arial", Font.BOLD, 14));
            dot.setForeground(statusColor);
            dot.setPreferredSize(new Dimension(20, 20));

            // Center: title + supplier · recorded by
            JPanel center = new JPanel(new BorderLayout(0, 2));
            center.setOpaque(false);
            JLabel titleLbl = new JLabel(logTitle);
            titleLbl.setFont(new Font("Arial", Font.BOLD, 12));
            titleLbl.setForeground(new Color(0xECF5EC));
            JLabel detailLbl = new JLabel(supplier + " · recorded by " + recorder);
            detailLbl.setFont(new Font("Arial", Font.PLAIN, 10));
            detailLbl.setForeground(new Color(0x7AAA8A));
            center.add(titleLbl, BorderLayout.NORTH);
            center.add(detailLbl, BorderLayout.SOUTH);

            // Right: date + cost + status badge
            JPanel right = new JPanel(new BorderLayout(0, 2));
            right.setOpaque(false);
            JLabel dateLbl = new JLabel(date, SwingConstants.RIGHT);
            dateLbl.setFont(new Font("Arial", Font.PLAIN, 10));
            dateLbl.setForeground(new Color(0x6A8A6A));
            JLabel costLbl = new JLabel(String.format("₱%,.0f", cost), SwingConstants.RIGHT);
            costLbl.setFont(new Font("Arial", Font.BOLD, 12));
            costLbl.setForeground(new Color(0x2E7D4F));
            JLabel statusLbl = new JLabel(status.toUpperCase(), SwingConstants.RIGHT);
            statusLbl.setFont(new Font("Arial", Font.BOLD, 9));
            statusLbl.setForeground(statusColor);
            right.add(dateLbl, BorderLayout.NORTH);
            right.add(costLbl, BorderLayout.CENTER);
            right.add(statusLbl, BorderLayout.SOUTH);

            row.add(dot, BorderLayout.WEST);
            row.add(center, BorderLayout.CENTER);
            row.add(right, BorderLayout.EAST);

            supplyLogList.add(row);
        }

        if (!any) {
            JLabel empty = new JLabel("No supply entries yet", SwingConstants.CENTER);
            empty.setFont(new Font("Arial", Font.PLAIN, 12));
            empty.setForeground(new Color(0x4A7A4A));
            supplyLogList.add(empty);
        }

        supplyLogList.revalidate();
        supplyLogList.repaint();
    }

    // ─────────────────────────────────────────────────────────────────
    // Category Bars (unchanged)
    // ─────────────────────────────────────────────────────────────────
    private void refreshCategoryBars(Connection conn) throws SQLException {
        barsPanel.removeAll();

        Color[] catColors = {
                new Color(0xD85A30), new Color(0x378ADD),
                new Color(0xEF9F27), new Color(0x7F77DD),
                new Color(0x2E7D4F), new Color(0xD4537E)
        };

        ResultSet rs = conn.createStatement().executeQuery(
                "SELECT c.categories_name, COUNT(p.Item_id) as cnt " +
                        "FROM categories_table c " +
                        "LEFT JOIN product_list p ON c.id = p.Item_category " +
                        "GROUP BY c.id, c.categories_name ORDER BY cnt DESC");

        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        int maxCount = 1;
        while (rs.next()) {
            int cnt = rs.getInt("cnt");
            if (cnt > maxCount)
                maxCount = cnt;
            rows.add(new Object[] { rs.getString("categories_name"), cnt });
        }

        int colorIdx = 0;
        for (Object[] row : rows) {
            String name = (String) row[0];
            int cnt = (Integer) row[1];
            Color color = catColors[colorIdx % catColors.length];
            colorIdx++;

            JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
            rowPanel.setOpaque(false);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            JLabel nameLbl = new JLabel(name);
            nameLbl.setFont(new Font("Arial", Font.PLAIN, 12));
            nameLbl.setForeground(new Color(0xCCCCCC));
            nameLbl.setPreferredSize(new Dimension(140, 20));

            JLabel countLbl = new JLabel(cnt + " items");
            countLbl.setFont(new Font("Arial", Font.BOLD, 11));
            countLbl.setForeground(color);
            countLbl.setPreferredSize(new Dimension(60, 20));
            countLbl.setHorizontalAlignment(SwingConstants.RIGHT);

            int finalMaxCount = maxCount;
            JPanel barFill = new JPanel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(color);
                    int w = (int) ((double) cnt / finalMaxCount * getWidth());
                    g.fillRect(0, 0, w, getHeight());
                }
            };
            barFill.setBackground(new Color(0x1A2A1A));

            rowPanel.add(nameLbl, BorderLayout.WEST);
            rowPanel.add(barFill, BorderLayout.CENTER);
            rowPanel.add(countLbl, BorderLayout.EAST);

            barsPanel.add(Box.createVerticalStrut(10));
            barsPanel.add(rowPanel);
        }
        barsPanel.revalidate();
        barsPanel.repaint();
    }
}