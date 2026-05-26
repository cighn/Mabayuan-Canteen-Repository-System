import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Modal for recording a payment on a tab.
 * Opened when admin clicks "Mark Paid" on a Pending tab card.
 */
public class TabPaymentWindow extends JDialog {

    private static final Color C_BG = new Color(0x1A2A1A);
    private static final Color C_CARD = new Color(0x243824);
    private static final Color C_CARD2 = new Color(0x163320);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);
    private static final Color C_GREEN = new Color(0x22C55E);

    private JComboBox<String> methodBox;
    private JTextField amountField;
    private Runnable onSuccess;
    private int tabId;
    private double total;

    public TabPaymentWindow(JFrame parent, int tabId, String teacherName,
            double total, Runnable onSuccess) {
        super(parent, "Record Payment", true);
        this.tabId = tabId;
        this.total = total;
        this.onSuccess = onSuccess;

        setSize(380, 370);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(C_CARD);
        setLayout(new BorderLayout());

        add(buildTitleBar(teacherName), BorderLayout.NORTH);
        add(buildForm(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadPaymentMethods();
        setVisible(true);
    }

    private JPanel buildTitleBar(String teacherName) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x1A3A2A));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(14, 18, 14, 18)));

        JLabel title = new JLabel("●  Record Payment");
        title.setFont(new Font("Arial", Font.BOLD, 15));
        title.setForeground(C_TEXT);

        JLabel sub = new JLabel("Tab for: " + teacherName);
        sub.setFont(new Font("Arial", Font.PLAIN, 11));
        sub.setForeground(C_MUTED);

        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.add(title);
        col.add(Box.createVerticalStrut(3));
        col.add(sub);
        bar.add(col, BorderLayout.WEST);
        return bar;
    }

    private JPanel buildForm() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(C_CARD);
        panel.setBorder(new EmptyBorder(20, 22, 10, 22));

        // Total due
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                new EmptyBorder(10, 14, 10, 14)));
        JLabel totalLbl = new JLabel("TOTAL DUE");
        totalLbl.setFont(new Font("Arial", Font.BOLD, 10));
        totalLbl.setForeground(C_MUTED);
        JLabel totalAmt = new JLabel(String.format("₱%.2f", total));
        totalAmt.setFont(new Font("Arial", Font.BOLD, 22));
        totalAmt.setForeground(C_GREEN);
        totalRow.add(totalLbl, BorderLayout.NORTH);
        totalRow.add(totalAmt, BorderLayout.SOUTH);

        // Payment method
        JLabel methodLbl = makeLabel("PAYMENT METHOD");
        methodBox = new JComboBox<>();
        methodBox.setFont(new Font("Arial", Font.PLAIN, 13));
        methodBox.setBackground(C_CARD2);
        methodBox.setForeground(C_TEXT);
        methodBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        methodBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Amount paid
        JLabel amountLbl = makeLabel("AMOUNT PAID");
        amountField = new JTextField(String.format("%.2f", total));
        amountField.setFont(new Font("Arial", Font.PLAIN, 14));
        amountField.setBackground(C_CARD2);
        amountField.setForeground(C_TEXT);
        amountField.setCaretColor(C_TEXT);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        amountField.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(totalRow);
        panel.add(Box.createVerticalStrut(16));
        panel.add(methodLbl);
        panel.add(Box.createVerticalStrut(5));
        panel.add(methodBox);
        panel.add(Box.createVerticalStrut(14));
        panel.add(amountLbl);
        panel.add(Box.createVerticalStrut(5));
        panel.add(amountField);

        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(new Color(0x1A3A2A));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(new Color(0x2A3A2A));
        cancelBtn.setForeground(C_MUTED);
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 12));
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setPreferredSize(new Dimension(90, 34));
        cancelBtn.addActionListener(e -> dispose());

        JButton confirmBtn = new JButton("Confirm Payment");
        confirmBtn.setBackground(C_ACCENT);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 12));
        confirmBtn.setBorderPainted(false);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setPreferredSize(new Dimension(150, 34));
        confirmBtn.addActionListener(e -> handleConfirm());

        footer.add(cancelBtn);
        footer.add(confirmBtn);
        return footer;
    }

    private void loadPaymentMethods() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT id, payment_name FROM payment_method ORDER BY id");
            while (rs.next()) {
                methodBox.addItem(rs.getInt("id") + ":" + rs.getString("payment_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "DB Error loading payment methods: " + ex.getMessage());
        }
    }

    private void handleConfirm() {
        if (methodBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a payment method.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double amountPaid;
        try {
            amountPaid = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid amount.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String methodStr = (String) methodBox.getSelectedItem();
        int methodId = Integer.parseInt(methodStr.split(":")[0]);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert payment record
                PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO tabs_payment (tab_id, amount_paid, payment_method, paid_at) " +
                                "VALUES (?, ?, ?, NOW())");
                ins.setInt(1, tabId);
                ins.setDouble(2, amountPaid);
                ins.setInt(3, methodId);
                ins.executeUpdate();

                // Update tab status to Paid
                PreparedStatement getStatus = conn.prepareStatement(
                        "SELECT id FROM tabs_status WHERE LOWER(tabs_status_name) = 'paid' LIMIT 1");
                ResultSet rs = getStatus.executeQuery();
                int paidId = 2; // fallback
                if (rs.next())
                    paidId = rs.getInt("id");

                PreparedStatement upd = conn.prepareStatement(
                        "UPDATE tabs SET status = ? WHERE id = ?");
                upd.setInt(1, paidId);
                upd.setInt(2, tabId);
                upd.executeUpdate();

                conn.commit();

                JOptionPane.showMessageDialog(this,
                        "Payment recorded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                if (onSuccess != null)
                    onSuccess.run();
                dispose();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 10));
        lbl.setForeground(C_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 0, 3, 0));
        return lbl;
    }
}