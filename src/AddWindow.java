import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.*;

public class AddWindow extends JDialog {

    private JTextField nameField = new JTextField();
    private JTextField priceField = new JTextField();
    private JTextField quantityField = new JTextField();
    private JComboBox<String> categoryBox;
    private JButton addButton = new JButton("Add");
    private Runnable onSuccess;

    // Dark green palette (matching system)
    private static final Color C_DIALOG_BG = new Color(0x1E2E1E);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_INPUT_BG = new Color(0x1A2A1A);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_LABEL_BG = new Color(0x2E7D4F);

    public AddWindow(JFrame parent, Runnable onSuccess) {
        super(parent, "Add New Product", true);
        this.onSuccess = onSuccess;

        setSize(380, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(C_DIALOG_BG);

        // Load categories from DB
        categoryBox = new JComboBox<>();
        loadCategories();

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(C_DIALOG_BG);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));

        formPanel.add(makeLabel("Name"));
        formPanel.add(Box.createVerticalStrut(5));
        styleField(nameField);
        formPanel.add(nameField);

        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(makeLabel("Price"));
        formPanel.add(Box.createVerticalStrut(5));
        styleField(priceField);
        formPanel.add(priceField);

        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(makeLabel("Quantity"));
        formPanel.add(Box.createVerticalStrut(5));
        styleField(quantityField);
        formPanel.add(quantityField);

        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(makeLabel("Category"));
        formPanel.add(Box.createVerticalStrut(5));
        categoryBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        categoryBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryBox.setBackground(C_INPUT_BG);
        categoryBox.setForeground(C_TEXT);
        categoryBox.setFont(new Font("Arial", Font.PLAIN, 12));
        formPanel.add(categoryBox);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(C_DIALOG_BG);
        styleButton(addButton);
        addButton.addActionListener(e -> handleAdd());
        bottomPanel.add(addButton);

        add(formPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void loadCategories() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            var rs = conn.createStatement()
                    .executeQuery("SELECT id, categories_name FROM categories_table ORDER BY categories_name");
            while (rs.next()) {
                categoryBox.addItem(rs.getInt("id") + ":" + rs.getString("categories_name"));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "DB Error loading categories: " + ex.getMessage());
        }
    }

    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(C_LABEL_BG);
        label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBackground(C_INPUT_BG);
        field.setForeground(C_TEXT);
        field.setCaretColor(C_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void styleButton(JButton btn) {
        btn.setBackground(C_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(100, 38));
    }

    private void handleAdd() {
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();
        String quantityText = quantityField.getText().trim();

        if (name.isEmpty() || priceText.isEmpty() || quantityText.isEmpty()
                || categoryBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "All fields are required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            int quantity = Integer.parseInt(quantityText);
            String selected = (String) categoryBox.getSelectedItem();
            int categoryId = Integer.parseInt(selected.split(":")[0]);

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO product_list " +
                        "(Item_name, Item_price, Item_quantity, Item_category) VALUES (?,?,?,?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, name);
                ps.setDouble(2, price);
                ps.setInt(3, quantity);
                ps.setInt(4, categoryId);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Product added successfully!");
                if (onSuccess != null)
                    onSuccess.run();
                dispose();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price and Quantity must be numbers.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}