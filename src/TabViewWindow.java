import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * Read-only detail window for a single teacher tab.
 * Shows: tab info header, items table, payment history.
 */
public class TabViewWindow extends JDialog {

    private static final Color C_BG = new Color(0x1A2A1A);
    private static final Color C_CARD = new Color(0x243824);
    private static final Color C_CARD2 = new Color(0x163320);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);
    private static final Color C_GREEN = new Color(0x22C55E);
    private static final Color C_AMBER = new Color(0xF59E0B);
    private static final Color C_RED = new Color(0xEF4444);

    private final int tabId;
    private final String teacherName;
    private final Runnable onRefresh;

    public TabViewWindow(JFrame parent, int tabId, String teacherName, Runnable onRefresh) {
        super(parent, "Tab Details — " + teacherName, true);
        this.tabId = tabId;
        this.teacherName = teacherName;
        this.onRefresh = onRefresh;

        setSize(700, 560);
        setLocationRelativeTo(parent);
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────
    // Header: teacher name, tab number, date, status, total
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x1A3A2A));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(14, 20, 14, 20)));

        // Info from DB
        String tabNum = String.format("Tab #%03d", tabId);
        String date = "—", status = "—";
        double total = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT t.total, t.created_at, ts.tabs_status_name " +
                            "FROM tabs t JOIN tabs_status ts ON t.status = ts.id " +
                            "WHERE t.id = ?");
            ps.setInt(1, tabId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
                status = rs.getString("tabs_status_name");
                if (rs.getTimestamp("created_at") != null) {
                    date = new java.text.SimpleDateFormat("MMM d, yyyy")
                            .format(rs.getTimestamp("created_at"));
                }
            }
        } catch (SQLException ex) {
            System.err.println("TabViewWindow header error: " + ex.getMessage());
        }

        Color statusColor = switch (status.toLowerCase()) {
            case "paid" -> C_GREEN;
            case "cancelled" -> C_RED;
            default -> C_AMBER;
        };

        JLabel nameLbl = new JLabel(teacherName);
        nameLbl.setFont(new Font("Arial", Font.BOLD, 16));
        nameLbl.setForeground(C_TEXT);

        JLabel metaLbl = new JLabel(tabNum + "  ·  " + date);
        metaLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        metaLbl.setForeground(C_MUTED);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(nameLbl);
        left.add(Box.createVerticalStrut(4));
        left.add(metaLbl);

        JPanel right = new JPanel(new BorderLayout(0, 4));
        right.setOpaque(false);

        JLabel statusBadge = new JLabel(status.toUpperCase(), SwingConstants.CENTER);
        statusBadge.setFont(new Font("Arial", Font.BOLD, 10));
        statusBadge.setForeground(Color.WHITE);
        statusBadge.setOpaque(true);
        statusBadge.setBackground(statusColor);
        statusBadge.setBorder(new EmptyBorder(3, 12, 3, 12));

        JLabel totalLbl = new JLabel(String.format("₱%.2f", total), SwingConstants.RIGHT);
        totalLbl.setFont(new Font("Arial", Font.BOLD, 20));
        totalLbl.setForeground(C_GREEN);

        right.add(statusBadge, BorderLayout.NORTH);
        right.add(totalLbl, BorderLayout.SOUTH);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────
    // Body: items table (top) + payment history (bottom)
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new GridLayout(2, 1, 0, 10));
        body.setBackground(C_BG);
        body.setBorder(new EmptyBorder(14, 16, 10, 16));

        body.add(buildItemsSection());
        body.add(buildPaymentSection());

        return body;
    }

    private JPanel buildItemsSection() {
        JPanel card = buildCard("Charged Items");

        String[] cols = { "#", "Product", "Qty", "Unit Price", "Subtotal", "Status", "Action" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return c == 6; // only Action column editable
            }
        };

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT ti.id, p.Item_name, ti.quantity, ti.unit_price, ti.subtotal, ti.is_paid, ti.added_at " +
                            "FROM tabs_items ti " +
                            "JOIN product_list p ON ti.product_id = p.Item_id " +
                            "WHERE ti.tab_id = ? ORDER BY ti.id");
            ps.setInt(1, tabId);
            ResultSet rs = ps.executeQuery();
            int rowNum = 1;
            while (rs.next()) {
                int itemId = rs.getInt("id");
                String product = rs.getString("Item_name");
                int qty = rs.getInt("quantity");
                double unitPrice = rs.getDouble("unit_price");
                double subtotal = rs.getDouble("subtotal");
                boolean isPaid = rs.getBoolean("is_paid");
                String status = isPaid ? "PAID" : "PENDING";

                // Add row with itemId stored in Action column
                model.addRow(new Object[] { rowNum++, product, qty,
                        String.format("₱%.2f", unitPrice), String.format("₱%.2f", subtotal), status, itemId });
            }
        } catch (SQLException ex) {
            System.err.println("Items load error: " + ex.getMessage());
        }

        JTable table = new JTable(model) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 6)
                    return Integer.class;
                return super.getColumnClass(column);
            }
        };

        // Custom button renderer/editor
        table.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox(), this));
        table.getColumnModel().getColumn(6).setHeaderValue("Action");

        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        scroll.getViewport().setBackground(new Color(0x1A2A1A));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildPaymentSection() {
        JPanel card = buildCard("Payment History");

        String[] cols = { "Payment ID", "Amount Paid", "Method", "Paid At" };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT tp.id, tp.amount_paid, pm.payment_name, tp.paid_at " +
                            "FROM tabs_payment tp " +
                            "JOIN payment_method pm ON tp.payment_method = pm.id " +
                            "WHERE tp.tab_id = ? ORDER BY tp.paid_at DESC");
            ps.setInt(1, tabId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String paidAt = rs.getTimestamp("paid_at") != null
                        ? new java.text.SimpleDateFormat("MMM d, yyyy  HH:mm").format(rs.getTimestamp("paid_at"))
                        : "—";
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        String.format("₱%.2f", rs.getDouble("amount_paid")),
                        rs.getString("payment_name"),
                        paidAt
                });
            }
        } catch (SQLException ex) {
            System.err.println("Payment load error: " + ex.getMessage());
        }

        if (model.getRowCount() == 0) {
            JLabel noPayment = new JLabel("No payments recorded yet.", SwingConstants.CENTER);
            noPayment.setFont(new Font("Arial", Font.ITALIC, 12));
            noPayment.setForeground(C_MUTED);
            card.add(noPayment, BorderLayout.CENTER);
        } else {
            JTable table = new JTable(model);
            styleTable(table);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
            scroll.getViewport().setBackground(new Color(0x1A2A1A));
            card.add(scroll, BorderLayout.CENTER);
        }

        return card;
    }

    private JPanel buildCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setForeground(C_TEXT);
        card.add(lbl, BorderLayout.NORTH);
        return card;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("Arial", Font.PLAIN, 12));
        t.setRowHeight(30);
        t.setBackground(new Color(0x1A2A1A));
        t.setForeground(C_TEXT);
        t.setGridColor(new Color(0x2E4A2E));
        t.setSelectionBackground(new Color(0x2E7D4F));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);

        JTableHeader h = t.getTableHeader();
        h.setBackground(new Color(0x1A3A2A));
        h.setForeground(new Color(0x7DC97D));
        h.setFont(new Font("Arial", Font.BOLD, 11));

        t.setDefaultRenderer(Object.class, (tbl, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val == null ? "" : val.toString());
            lbl.setOpaque(true);
            lbl.setFont(new Font("Arial", Font.PLAIN, 12));
            lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
            if (sel) {
                lbl.setBackground(new Color(0x2E7D4F));
                lbl.setForeground(Color.WHITE);
            } else {
                lbl.setBackground(row % 2 == 0 ? new Color(0x1A2A1A) : new Color(0x1E301E));
                lbl.setForeground(C_TEXT);
            }
            return lbl;
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Footer: close button
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setBackground(new Color(0x1A3A2A));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(0x2A3A2A));
        closeBtn.setForeground(C_MUTED);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 12));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(90, 34));
        closeBtn.addActionListener(e -> dispose());

        footer.add(closeBtn);
        return footer;
    }

    // ─────────────────────────────────────────────────────────────────
    // Item-level payment handling
    // ─────────────────────────────────────────────────────────────────
    private void payForItem(int tabItemId) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark this item as paid? This will record payment for its subtotal.",
                "Confirm Item Payment", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Get item details
            PreparedStatement getItem = conn.prepareStatement(
                    "SELECT subtotal, tab_id FROM tabs_items WHERE id = ?");
            getItem.setInt(1, tabItemId);
            ResultSet rs = getItem.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Item not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            double subtotal = rs.getDouble("subtotal");
            int currentTabId = rs.getInt("tab_id");

            // Mark item as paid
            PreparedStatement markPaid = conn.prepareStatement(
                    "UPDATE tabs_items SET is_paid = TRUE WHERE id = ?");
            markPaid.setInt(1, tabItemId);
            markPaid.executeUpdate();

            // Record a payment entry
            int methodId = getPaymentMethodId(conn, "Cash");
            PreparedStatement insPayment = conn.prepareStatement(
                    "INSERT INTO tabs_payment (tab_id, amount_paid, payment_method, paid_at) VALUES (?, ?, ?, NOW())");
            insPayment.setInt(1, currentTabId);
            insPayment.setDouble(2, subtotal);
            insPayment.setInt(3, methodId);
            insPayment.executeUpdate();

            // Reduce tab total
            PreparedStatement updTotal = conn.prepareStatement(
                    "UPDATE tabs SET total = total - ? WHERE id = ?");
            updTotal.setDouble(1, subtotal);
            updTotal.setInt(2, currentTabId);
            updTotal.executeUpdate();

            // Check if all items are now paid
            PreparedStatement checkAll = conn.prepareStatement(
                    "SELECT COUNT(*) FROM tabs_items WHERE tab_id = ? AND is_paid = FALSE");
            checkAll.setInt(1, currentTabId);
            ResultSet rsAll = checkAll.executeQuery();
            boolean allPaid = rsAll.next() && rsAll.getInt(1) == 0;

            if (allPaid) {
                int paidStatusId = getStatusId(conn, "paid");
                PreparedStatement updStatus = conn.prepareStatement(
                        "UPDATE tabs SET status = ? WHERE id = ?");
                updStatus.setInt(1, paidStatusId);
                updStatus.setInt(2, currentTabId);
                updStatus.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Item marked as paid!", "Success", JOptionPane.INFORMATION_MESSAGE);
            if (onRefresh != null)
                onRefresh.run();
            dispose();
            new TabViewWindow((JFrame) getParent(), tabId, teacherName, onRefresh);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getPaymentMethodId(Connection conn, String methodName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM payment_method WHERE payment_name = ?");
        ps.setString(1, methodName);
        ResultSet rs = ps.executeQuery();
        if (rs.next())
            return rs.getInt("id");
        return 1; // fallback
    }

    private int getStatusId(Connection conn, String statusName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM tabs_status WHERE LOWER(tabs_status_name) = ? LIMIT 1");
        ps.setString(1, statusName.toLowerCase());
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("id") : 1;
    }

    // ─────────────────────────────────────────────────────────────────
    // Button Renderer (shows "Pay" text)
    // ─────────────────────────────────────────────────────────────────
    // Replace the ButtonRenderer class
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setText("Pay Now");
            setBackground(C_ACCENT);
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 11));
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // Check if item is already paid
            String status = (String) table.getModel().getValueAt(row, 5);
            if ("PAID".equals(status)) {
                setText("Paid");
                setBackground(C_GREEN);
                setEnabled(false);
            } else {
                setText("Pay Now");
                setBackground(C_ACCENT);
                setEnabled(true);
            }
            return this;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Button Editor (handles click)
    // ─────────────────────────────────────────────────────────────────
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private int row;
        private JTable table;
        private TabViewWindow window;

        public ButtonEditor(JCheckBox checkBox, TabViewWindow window) {
            super(checkBox);
            this.window = window;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.table = table;
            this.row = row;
            button.setText("Pay");
            button.setBackground(C_ACCENT);
            button.setForeground(Color.WHITE);
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                int itemId = (int) table.getModel().getValueAt(row, 6);
                window.payForItem(itemId);
            }
            isPushed = false;
            return "Pay";
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}