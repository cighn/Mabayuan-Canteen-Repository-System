import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StockAddWindow extends JDialog {

    private static final Color C_BG = new Color(0x1E2E1E);
    private static final Color C_CARD = new Color(0x163320);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);

    private JTextField txtLogTitle = new JTextField();
    private JTextField txtSupplier = new JTextField();
    private JTextField txtTotalCost = new JTextField();
    private JComboBox<String> cbRecordedBy = new JComboBox<>();
    private JComboBox<String> cbStatus = new JComboBox<>();
    private JComboBox<String> cbCategory = new JComboBox<>();

    // Store IDs parallel to combo items
    private java.util.List<Integer> entityIds = new java.util.ArrayList<>();
    private java.util.List<Integer> statusIds = new java.util.ArrayList<>();
    private java.util.List<Integer> categoryIds = new java.util.ArrayList<>();

    private final Runnable onSuccess;

    public StockAddWindow(JFrame parent, Runnable onSuccess) {
        super(parent, "New Supply Entry", true);
        this.onSuccess = onSuccess;

        setSize(460, 420);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setBackground(C_BG);
        main.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("+ New Replenishment Entry");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(C_TEXT);
        main.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));
        form.setOpaque(false);

        addFormRow(form, "Log Title", txtLogTitle);
        addFormRow(form, "Supplier", txtSupplier);
        addFormRow(form, "Total Cost", txtTotalCost);
        addComboRow(form, "Recorded By", cbRecordedBy);
        addComboRow(form, "Status", cbStatus);
        addComboRow(form, "Category", cbCategory);

        main.add(form, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        JButton cancelBtn = new JButton("Cancel");
        JButton saveBtn = new JButton("Save Entry");
        styleBtn(cancelBtn, new Color(0x4A5568));
        styleBtn(saveBtn, C_ACCENT);
        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(e -> saveEntry());
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        main.add(btnRow, BorderLayout.SOUTH);

        setContentPane(main);
        loadCombos();
        setVisible(true);
    }

    private void addFormRow(JPanel panel, String label, JTextField field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(C_MUTED);
        styleField(field);
        panel.add(lbl);
        panel.add(field);
    }

    private void addComboRow(JPanel panel, String label, JComboBox<String> combo) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(C_MUTED);
        combo.setFont(new Font("Arial", Font.PLAIN, 13));
        panel.add(lbl);
        panel.add(combo);
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("Arial", Font.PLAIN, 13));
        f.setBackground(new Color(0x0F2218));
        f.setForeground(C_TEXT);
        f.setCaretColor(C_TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void styleBtn(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void loadCombos() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Recorded By
            ResultSet rs1 = conn.createStatement()
                    .executeQuery("SELECT entity_id, entity_name FROM entity_table ORDER BY entity_name");
            while (rs1.next()) {
                entityIds.add(rs1.getInt("entity_id"));
                cbRecordedBy.addItem(rs1.getString("entity_name"));
            }
            // Status
            ResultSet rs2 = conn.createStatement()
                    .executeQuery("SELECT id, status_supply_name FROM supply_status");
            while (rs2.next()) {
                statusIds.add(rs2.getInt("id"));
                cbStatus.addItem(rs2.getString("status_supply_name"));
            }
            // Category
            ResultSet rs3 = conn.createStatement()
                    .executeQuery("SELECT id, categories_name FROM categories_table ORDER BY categories_name");
            while (rs3.next()) {
                categoryIds.add(rs3.getInt("id"));
                cbCategory.addItem(rs3.getString("categories_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading dropdowns: " + ex.getMessage());
        }
    }

    private void saveEntry() {
        String logTitle = txtLogTitle.getText().trim();
        String supplier = txtSupplier.getText().trim();
        String costStr = txtTotalCost.getText().trim();

        if (logTitle.isEmpty() || supplier.isEmpty() || costStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        double cost;
        try {
            cost = Double.parseDouble(costStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Total Cost must be a number.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int recordedById = entityIds.get(cbRecordedBy.getSelectedIndex());
        int statusId = statusIds.get(cbStatus.getSelectedIndex());
        int categoryId = categoryIds.get(cbCategory.getSelectedIndex());

        String sql = "INSERT INTO supply_log (log_title, supplier_name, recorded_by, total_cost, status, category_restock) "
                +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, logTitle);
            ps.setString(2, supplier);
            ps.setInt(3, recordedById);
            ps.setDouble(4, cost);
            ps.setInt(5, statusId);
            ps.setInt(6, categoryId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Entry saved successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            onSuccess.run();
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}