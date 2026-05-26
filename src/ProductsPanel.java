import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class ProductsPanel extends JPanel {

    private JTextField searchField = new JTextField();
    private JButton searchButton = new JButton("Search");
    private JButton resetButton = new JButton("Reset");
    private JButton addButton = new JButton("+ Add Product");

    private String[] columns = { "ID", "Name", "Category", "Price", "Quantity", "Stock Level", "Actions" };
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private String currentSearchKeyword = "";

    public ProductsPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0x1E2E1E));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top Panel (Search + Add)
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(200, 32));

        // ✅ Style Search & Reset buttons to green
        searchButton.setFont(new Font("Arial", Font.BOLD, 12));
        styleButton(searchButton, new Color(0x2E7D4F)); // same green as + Add Product

        resetButton.setFont(new Font("Arial", Font.BOLD, 12));
        styleButton(resetButton, new Color(0x2E7D4F));

        JPanel searchGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchGroup.setOpaque(false);
        searchGroup.add(searchField);
        searchGroup.add(searchButton);
        searchGroup.add(resetButton);

        styleButton(addButton, new Color(0x2E7D4F));

        JButton refreshBtn = new JButton("Refresh");
        styleButton(refreshBtn, new Color(0x1B5E3B));
        refreshBtn.addActionListener(e -> refreshTable());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(refreshBtn);
        rightPanel.add(addButton);

        topPanel.add(searchGroup, BorderLayout.WEST);
        topPanel.add(rightPanel, BorderLayout.EAST);

        // Table Setup
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        styleTable();

        // Mouse listener for Actions column (single click on "Edit" or "Del" text)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col == 6) { // Actions column
                    int row = table.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        int modelRow = table.convertRowIndexToModel(row);
                        int x = e.getX();
                        int cellX = table.getCellRect(row, col, true).x;
                        int half = cellX + (table.getColumnModel().getColumn(col).getWidth() / 2);
                        if (x < half) {
                            openEditWindow(modelRow);
                        } else {
                            deleteProduct(modelRow);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Buttons events
        addButton.addActionListener(
                e -> new AddWindow((JFrame) SwingUtilities.getWindowAncestor(this), this::refreshTable));
        searchButton.addActionListener(e -> {
            currentSearchKeyword = searchField.getText().trim();
            refreshTable();
        });
        resetButton.addActionListener(e -> {
            searchField.setText("");
            currentSearchKeyword = "";
            refreshTable();
        });

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        refreshTable();
    }

    // ------------------------------------------------------------------
    // Table Styling (clean, striped, proper selection)
    // ------------------------------------------------------------------
    private void styleTable() {
        // Header
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(0x2E7D4F));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getWidth(), 40));

        // Table body
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(38);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(8, 1));
        table.setSelectionBackground(new Color(200, 230, 200));
        table.setSelectionForeground(Color.BLACK);

        // Custom renderers
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new StockLevelRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new ActionsRenderer());

        // Center alignment for specific columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        table.getColumnModel().getColumn(3).setCellRenderer(center); // Price
        table.getColumnModel().getColumn(4).setCellRenderer(center); // Quantity
    }

    // Renders striped rows + preserves selection background
    private class StripedRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return c;
        }
    }

    // Stock Level Renderer – keeps badge color even when selected
    private class StockLevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            int qty = (int) tbl.getValueAt(row, 4);
            String status;
            Color bgColor;

            if (qty > 200) {
                status = "High";
                bgColor = new Color(200, 230, 200);
            } else if (qty >= 100) {
                status = "Medium";
                bgColor = new Color(255, 235, 190);
            } else {
                status = "Low";
                bgColor = new Color(255, 200, 200);
            }

            JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, status, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 12));
            label.setOpaque(true);
            // Always use the badge color – do NOT change to selection background.
            label.setBackground(bgColor);
            label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            return label;
        }
    }

    // Actions Renderer – shows "Edit Del" text (reliable, no broken icons)
    private class ActionsRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, "Edit    Del", isSelected, hasFocus, row,
                    column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 11));
            label.setForeground(new Color(70, 130, 200));
            label.setOpaque(true);
            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
            } else {
                label.setBackground(tbl.getSelectionBackground());
            }
            return label;
        }
    }

    // ------------------------------------------------------------------
    // Data Loading + Search
    // ------------------------------------------------------------------
    public void refreshTable() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT p.Item_id, p.Item_name, p.Item_price, p.Item_quantity, c.categories_name " +
                    "FROM product_list p JOIN categories_table c ON p.Item_category = c.id";
            if (!currentSearchKeyword.isEmpty()) {
                query += " WHERE p.Item_name LIKE ? OR c.categories_name LIKE ?";
            }
            PreparedStatement ps = conn.prepareStatement(query);
            if (!currentSearchKeyword.isEmpty()) {
                String like = "%" + currentSearchKeyword + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = new Object[7];
                row[0] = rs.getInt("Item_id");
                row[1] = rs.getString("Item_name");
                row[2] = rs.getString("categories_name");
                row[3] = rs.getDouble("Item_price");
                row[4] = rs.getInt("Item_quantity");
                row[5] = ""; // Stock Level (rendered dynamically)
                row[6] = ""; // Actions placeholder
                tableModel.addRow(row);
            }
            sorter.setRowFilter(null);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    // Edit & Delete functions (using model row index)
    // ------------------------------------------------------------------
    private void openEditWindow(int modelRow) {
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        String category = (String) tableModel.getValueAt(modelRow, 2);
        double price = (double) tableModel.getValueAt(modelRow, 3);
        int qty = (int) tableModel.getValueAt(modelRow, 4);
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        new EditWindow(parent, id, name, price, qty, category, this::refreshTable);
    }

    private void deleteProduct(int modelRow) {
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String name = (String) tableModel.getValueAt(modelRow, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + name + "\"?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM product_list WHERE Item_id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                refreshTable();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}