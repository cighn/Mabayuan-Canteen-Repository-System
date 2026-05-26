import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import org.w3c.dom.events.MouseEvent;

public class TabAddWindow extends JDialog {

    // ─────────────────────────────────────────────────────────────────
    // Color palette (mirrors system)
    // ─────────────────────────────────────────────────────────────────
    private static final Color C_BG = new Color(0x1A2A1A);
    private static final Color C_CARD = new Color(0x243824);
    private static final Color C_CARD2 = new Color(0x163320);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);
    private static final Color C_RED = new Color(0xEF4444);
    private static final Color C_AMBER = new Color(0xF59E0B);

    // ─────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────
    private JComboBox<String> teacherBox;
    private Map<Integer, Integer> cartQty = new LinkedHashMap<>(); // productId → qty
    private Map<Integer, Double> cartPrice = new HashMap<>(); // productId → unit_price
    private Map<Integer, String> cartName = new HashMap<>(); // productId → name
    private Map<Integer, JLabel> qtyLabels = new HashMap<>(); // productId → qty display
    private List<Integer> allProductIds = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────
    // Cart table
    // ─────────────────────────────────────────────────────────────────
    private DefaultTableModel cartModel;
    private JLabel totalLabel;

    private Runnable onSuccess;

    // ─────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────
    public TabAddWindow(JFrame parent, Runnable onSuccess) {
        super(parent, "New Tab — Add Products", true);
        this.onSuccess = onSuccess;

        setSize(900, 640);
        setLocationRelativeTo(parent);
        setResizable(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout(0, 0));

        add(buildTitleBar(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadTeachers();
        loadProductsFiltered("");

        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────
    // Title bar
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x1A3A2A));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JLabel title = new JLabel("+ New Teacher Tab");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(C_TEXT);

        JLabel sub = new JLabel("Select a teacher, then add products from the grid");
        sub.setFont(new Font("Arial", Font.PLAIN, 11));
        sub.setForeground(C_MUTED);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(Box.createVerticalStrut(3));
        left.add(sub);

        bar.add(left, BorderLayout.WEST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────
    // Body: LEFT = teacher picker + product grid, RIGHT = cart
    // ─────────────────────────────────────────────────────────────────
    private JSplitPane buildBody() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        split.setResizeWeight(0.58);
        split.setDividerSize(6);
        split.setBackground(C_BG);
        split.setBorder(null);
        return split;
    }

    // ── Left Panel: Teacher selector + Product grid ──────────────────
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(16, 16, 16, 8));

        // Teacher selector
        JPanel teacherRow = new JPanel(new BorderLayout(10, 0));
        teacherRow.setOpaque(false);

        JLabel teacherLbl = makeFieldLabel("TEACHER");

        teacherBox = new JComboBox<>();
        teacherBox.setFont(new Font("Arial", Font.PLAIN, 13));
        teacherBox.setBackground(C_CARD2);
        teacherBox.setForeground(C_TEXT);
        teacherBox.setPreferredSize(new Dimension(0, 36));

        teacherRow.add(teacherLbl, BorderLayout.NORTH);
        teacherRow.add(teacherBox, BorderLayout.CENTER);

        // ✅ NEW: Product search field
        JPanel searchRow = new JPanel(new BorderLayout(0, 6));
        searchRow.setOpaque(false);
        searchRow.setBorder(new EmptyBorder(8, 0, 8, 0));

        JLabel searchLbl = makeFieldLabel("SEARCH PRODUCTS");
        JTextField productSearchField = new JTextField();
        productSearchField.setFont(new Font("Arial", Font.PLAIN, 12));
        productSearchField.setBackground(C_CARD2);
        productSearchField.setForeground(C_TEXT);
        productSearchField.setCaretColor(C_TEXT);
        productSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        // Placeholder
        String ph = "Search by name or category...";
        productSearchField.setForeground(C_MUTED);
        productSearchField.setText(ph);
        productSearchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (productSearchField.getText().equals(ph)) {
                    productSearchField.setText("");
                    productSearchField.setForeground(C_TEXT);
                }
            }

            public void focusLost(FocusEvent e) {
                if (productSearchField.getText().isBlank()) {
                    productSearchField.setForeground(C_MUTED);
                    productSearchField.setText(ph);
                }
            }
        });
        productSearchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String kw = productSearchField.getText().equals(ph) ? "" : productSearchField.getText().trim();
                loadProductsFiltered(kw);
            }
        });

        searchRow.add(searchLbl, BorderLayout.NORTH);
        searchRow.add(productSearchField, BorderLayout.CENTER);

        // Product grid section label
        JPanel gridHeader = new JPanel(new BorderLayout());
        gridHeader.setOpaque(false);
        JLabel gridTitle = new JLabel("Products — Click to add to tab");
        gridTitle.setFont(new Font("Arial", Font.BOLD, 13));
        gridTitle.setForeground(C_TEXT);
        JLabel gridSub = new JLabel("Use +/− on each card to set quantity");
        gridSub.setFont(new Font("Arial", Font.PLAIN, 11));
        gridSub.setForeground(C_MUTED);
        gridHeader.add(gridTitle, BorderLayout.WEST);
        gridHeader.add(gridSub, BorderLayout.EAST);

        // Product grid container
        JPanel productGridContainer = new JPanel(new GridLayout(0, 3, 10, 10));
        productGridContainer.setBackground(C_BG);
        productGridContainer.setName("productGrid"); // reference for reload

        JScrollPane scroll = new JScrollPane(productGridContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(C_BG);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Store references for loadProducts()
        this.productGrid = productGridContainer;

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(teacherRow);
        top.add(Box.createVerticalStrut(8));
        top.add(searchRow);
        top.add(Box.createVerticalStrut(8));
        top.add(gridHeader);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel productGrid; // set in buildLeftPanel, populated in loadProducts

    // ── Right Panel: cart / order summary ───────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(C_CARD2);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, C_BORDER),
                new EmptyBorder(16, 12, 12, 16)));

        JLabel cartTitle = new JLabel("Cart / Tab Items");
        cartTitle.setFont(new Font("Arial", Font.BOLD, 14));
        cartTitle.setForeground(C_TEXT);

        JLabel cartSub = new JLabel("Items added to this tab");
        cartSub.setFont(new Font("Arial", Font.PLAIN, 11));
        cartSub.setForeground(C_MUTED);

        JPanel cartHeader = new JPanel(new BorderLayout(0, 2));
        cartHeader.setOpaque(false);
        cartHeader.add(cartTitle, BorderLayout.NORTH);
        cartHeader.add(cartSub, BorderLayout.SOUTH);

        // Cart table
        String[] cols = { "Product", "Qty", "Unit Price", "Subtotal" };
        cartModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable cartTable = new JTable(cartModel);
        styleCartTable(cartTable);

        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createLineBorder(C_BORDER));
        cartScroll.getViewport().setBackground(new Color(0x1A2A1A));
        cartScroll.setBackground(new Color(0x1A2A1A));

        // Total row
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                BorderFactory.createEmptyBorder(10, 4, 4, 4)));
        JLabel totalLbl = new JLabel("TOTAL");
        totalLbl.setFont(new Font("Arial", Font.BOLD, 12));
        totalLbl.setForeground(C_MUTED);
        totalLabel = new JLabel("₱0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 20));
        totalLabel.setForeground(new Color(0x22C55E));
        totalRow.add(totalLbl, BorderLayout.WEST);
        totalRow.add(totalLabel, BorderLayout.EAST);

        // Clear cart button
        JButton clearBtn = new JButton("Clear All");
        clearBtn.setBackground(new Color(0x3A1A1A));
        clearBtn.setForeground(C_RED);
        clearBtn.setFont(new Font("Arial", Font.BOLD, 11));
        clearBtn.setBorderPainted(false);
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.setPreferredSize(new Dimension(90, 28));
        clearBtn.addActionListener(e -> {
            cartQty.clear();
            qtyLabels.forEach((id, lbl) -> lbl.setText("0"));
            refreshCart();
        });

        JPanel totalSection = new JPanel(new BorderLayout());
        totalSection.setOpaque(false);
        totalSection.add(totalRow, BorderLayout.NORTH);
        JPanel clearRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        clearRow.setOpaque(false);
        clearRow.add(clearBtn);
        totalSection.add(clearRow, BorderLayout.SOUTH);

        panel.add(cartHeader, BorderLayout.NORTH);
        panel.add(cartScroll, BorderLayout.CENTER);
        panel.add(totalSection, BorderLayout.SOUTH);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────
    // Footer: Cancel + Save Tab buttons
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setBackground(new Color(0x1A3A2A));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(new Color(0x2A3A2A));
        cancelBtn.setForeground(C_MUTED);
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 13));
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setPreferredSize(new Dimension(100, 36));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Save Tab ");
        saveBtn.setBackground(C_ACCENT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Arial", Font.BOLD, 13));
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setPreferredSize(new Dimension(130, 36));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> handleSave());

        footer.add(cancelBtn);
        footer.add(saveBtn);
        return footer;
    }

    // ─────────────────────────────────────────────────────────────────
    // Load teachers into combobox
    // ─────────────────────────────────────────────────────────────────
    private void loadTeachers() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT id, teacher_name FROM teacher ORDER BY teacher_name");
            while (rs.next()) {
                teacherBox.addItem(rs.getInt("id") + ":" + rs.getString("teacher_name"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "DB Error loading teachers: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Load products as cards in the grid
    // ─────────────────────────────────────────────────────────────────
    private void loadProducts() {
        if (productGrid == null) {
            System.err.println("Product grid not initialized yet");
            return;
        }

        productGrid.removeAll();
        allProductIds.clear();
        qtyLabels.clear();

        try (Connection conn = DatabaseConnection.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT p.Item_id, p.Item_name, p.Item_price, p.Item_quantity, " +
                            "c.categories_name " +
                            "FROM product_list p " +
                            "JOIN categories_table c ON p.Item_category = c.id " +
                            "ORDER BY c.categories_name, p.Item_name");

            while (rs.next()) {
                int id = rs.getInt("Item_id");
                String name = rs.getString("Item_name");
                double price = rs.getDouble("Item_price");
                int stock = rs.getInt("Item_quantity");
                String cat = rs.getString("categories_name");

                allProductIds.add(id);
                cartPrice.put(id, price);
                cartName.put(id, name);

                productGrid.add(buildProductCard(id, name, price, stock, cat));
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "DB Error loading products: " + ex.getMessage());
        }

        productGrid.revalidate();
        productGrid.repaint();
    }

    private void loadProductsFiltered(String keyword) {
        if (productGrid == null)
            return;

        productGrid.removeAll();
        allProductIds.clear();
        qtyLabels.clear();

        String sql = "SELECT p.Item_id, p.Item_name, p.Item_price, p.Item_quantity, c.categories_name " +
                "FROM product_list p " +
                "JOIN categories_table c ON p.Item_category = c.id " +
                "WHERE LOWER(p.Item_name) LIKE ? OR LOWER(c.categories_name) LIKE ? " +
                "ORDER BY c.categories_name, p.Item_name";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword.toLowerCase() + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            ResultSet rs = ps.executeQuery();

            boolean hasResults = false;
            while (rs.next()) {
                hasResults = true;
                int id = rs.getInt("Item_id");
                String name = rs.getString("Item_name");
                double price = rs.getDouble("Item_price");
                int stock = rs.getInt("Item_quantity");
                String cat = rs.getString("categories_name");

                allProductIds.add(id);
                cartPrice.put(id, price);
                cartName.put(id, name);

                productGrid.add(buildProductCard(id, name, price, stock, cat));
            }

            if (!hasResults) {
                JLabel noResults = new JLabel("No products found for: " + keyword, SwingConstants.CENTER);
                noResults.setForeground(C_MUTED);
                noResults.setFont(new Font("Arial", Font.ITALIC, 12));
                productGrid.add(noResults);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        productGrid.revalidate();
        productGrid.repaint();
    }

    // ─────────────────────────────────────────────────────────────────
    // Single product card with +/− controls
    // ─────────────────────────────────────────────────────────────────
    private JPanel buildProductCard(int id, String name, double price, int stock, String category) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean inCart = cartQty.getOrDefault(id, 0) > 0;
                g2.setColor(inCart ? new Color(0x1A3A2A) : C_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(inCart ? C_ACCENT : C_BORDER);
                g2.setStroke(new BasicStroke(inCart ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Product name - LARGER FONT
        JLabel nameLbl = new JLabel("<html><b style='font-size:13px'>" + name + "</b></html>");
        nameLbl.setFont(new Font("Arial", Font.BOLD, 13));
        nameLbl.setForeground(C_TEXT);

        // Category tag + price
        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false);
        JLabel catLbl = new JLabel(category);
        catLbl.setFont(new Font("Arial", Font.PLAIN, 10));
        catLbl.setForeground(C_MUTED);
        JLabel priceLbl = new JLabel(String.format("₱%.2f", price));
        priceLbl.setFont(new Font("Arial", Font.BOLD, 13));
        priceLbl.setForeground(new Color(0x22C55E));
        metaRow.add(catLbl, BorderLayout.WEST);
        metaRow.add(priceLbl, BorderLayout.EAST);

        // Stock indicator
        Color stockColor = stock < 30 ? C_RED : stock < 100 ? C_AMBER : C_ACCENT;
        JLabel stockLbl = new JLabel("Stock: " + stock);
        stockLbl.setFont(new Font("Arial", Font.PLAIN, 10));
        stockLbl.setForeground(stockColor);

        // +/- qty controls - LARGER BUTTONS
        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        qtyRow.setOpaque(false);

        JButton minusBtn = createQuantityButton("−");
        JLabel qtyLbl = new JLabel("0", SwingConstants.CENTER);
        qtyLbl.setFont(new Font("Arial", Font.BOLD, 15));
        qtyLbl.setForeground(C_TEXT);
        qtyLbl.setPreferredSize(new Dimension(35, 28));
        JButton plusBtn = createQuantityButton("+");

        qtyLabels.put(id, qtyLbl);

        plusBtn.addActionListener(e -> {
            int current = cartQty.getOrDefault(id, 0);
            if (current < stock) {
                int newQty = current + 1;
                cartQty.put(id, newQty);
                qtyLbl.setText(String.valueOf(newQty));
                card.repaint();
                refreshCart();
            }
        });

        minusBtn.addActionListener(e -> {
            int current = cartQty.getOrDefault(id, 0);
            if (current > 0) {
                int newQty = current - 1;
                if (newQty == 0) {
                    cartQty.remove(id);
                } else {
                    cartQty.put(id, newQty);
                }
                qtyLbl.setText(String.valueOf(newQty));
                card.repaint();
                refreshCart();
            }
        });

        qtyRow.add(minusBtn);
        qtyRow.add(qtyLbl);
        qtyRow.add(plusBtn);

        JPanel topSection = new JPanel(new BorderLayout(0, 5));
        topSection.setOpaque(false);
        topSection.add(nameLbl, BorderLayout.NORTH);
        topSection.add(metaRow, BorderLayout.CENTER);
        topSection.add(stockLbl, BorderLayout.SOUTH);

        card.add(topSection, BorderLayout.CENTER);
        card.add(qtyRow, BorderLayout.SOUTH);

        return card;
    }

    // Add this helper method to TabAddWindow
    private JButton createQuantityButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(0x2E7D4F));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeQtyBtn(String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(0x2A4A35));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x1A3A2A), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(C_ACCENT);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(0x2A4A35));
            }
        });
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────
    // Refresh cart table + total
    // ─────────────────────────────────────────────────────────────────
    private void refreshCart() {
        cartModel.setRowCount(0);
        double total = 0;

        for (Map.Entry<Integer, Integer> entry : cartQty.entrySet()) {
            int productId = entry.getKey();
            int qty = entry.getValue();
            double unitPrice = cartPrice.getOrDefault(productId, 0.0);
            double subtotal = qty * unitPrice;
            total += subtotal;

            cartModel.addRow(new Object[] {
                    cartName.getOrDefault(productId, "?"),
                    qty,
                    String.format("₱%.2f", unitPrice),
                    String.format("₱%.2f", subtotal)
            });
        }

        totalLabel.setText(String.format("₱%.2f", total));
    }

    // ─────────────────────────────────────────────────────────────────
    // Cart table styling
    // ─────────────────────────────────────────────────────────────────
    private void styleCartTable(JTable t) {
        t.setFont(new Font("Arial", Font.PLAIN, 12));
        t.setRowHeight(32);
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
            if (col == 3)
                lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            if (sel) {
                lbl.setBackground(new Color(0x2E7D4F));
                lbl.setForeground(Color.WHITE);
            } else {
                lbl.setBackground(row % 2 == 0 ? new Color(0x1A2A1A) : new Color(0x1E301E));
                lbl.setForeground(col == 3 ? new Color(0x22C55E) : C_TEXT);
            }
            return lbl;
        });

        // column widths
        int[] widths = { 140, 40, 80, 80 };
        for (int i = 0; i < widths.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Save tab to DB
    // ─────────────────────────────────────────────────────────────────
    private void handleSave() {
        // Validate teacher
        if (teacherBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select a teacher.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (cartQty.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one product.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selected = (String) teacherBox.getSelectedItem();
        int teacherId = Integer.parseInt(selected.split(":")[0]);
        String teacherName = selected.split(":", 2)[1];

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // ✅ Check stock BEFORE anything else
            if (!validateStockAvailability(conn)) {
                conn.rollback();
                return;
            }

            // ✅ CHECK: Does this teacher have an existing PENDING tab?
            int existingTabId = checkExistingPendingTab(conn, teacherId);

            int tabId;

            if (existingTabId > 0) {
                // ✅ EXISTING TAB - Add items to it
                tabId = existingTabId;

                // Update total in tabs table
                double currentTotal = getCurrentTabTotal(conn, tabId);
                double newTotal = currentTotal + calculateCartTotal();
                updateTabTotal(conn, tabId, newTotal);

                // Add new items to tabs_items
                addItemsToExistingTab(conn, tabId);

                JOptionPane.showMessageDialog(this,
                        "Items added to existing tab for " + teacherName + "!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

            } else {
                // ✅ NEW TAB - Create fresh tab
                tabId = createNewTab(conn, teacherId);
                JOptionPane.showMessageDialog(this,
                        "New tab created successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            // ✅ DEDUCT STOCK (Fix #4)
            deductProductStock(conn, cartQty);

            conn.commit();

            if (onSuccess != null)
                onSuccess.run();
            dispose();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper methods:

    private int checkExistingPendingTab(Connection conn, int teacherId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT t.id FROM tabs t " +
                        "JOIN tabs_status ts ON t.status = ts.id " +
                        "WHERE t.teacher_id = ? AND LOWER(ts.tabs_status_name) = 'pending' " +
                        "LIMIT 1");
        ps.setInt(1, teacherId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("id") : -1;
    }

    private double getCurrentTabTotal(Connection conn, int tabId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT total FROM tabs WHERE id = ?");
        ps.setInt(1, tabId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getDouble("total") : 0;
    }

    private void updateTabTotal(Connection conn, int tabId, double newTotal) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("UPDATE tabs SET total = ? WHERE id = ?");
        ps.setDouble(1, newTotal);
        ps.setInt(2, tabId);
        ps.executeUpdate();
    }

    private double calculateCartTotal() {
        double total = 0;
        for (Map.Entry<Integer, Integer> e : cartQty.entrySet()) {
            total += e.getValue() * cartPrice.getOrDefault(e.getKey(), 0.0);
        }
        return total;
    }

    private void addItemsToExistingTab(Connection conn, int tabId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tabs_items (tab_id, product_id, quantity, unit_price, subtotal, added_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW())");

        for (Map.Entry<Integer, Integer> e : cartQty.entrySet()) {
            int pid = e.getKey();
            int qty = e.getValue();
            double unitP = cartPrice.getOrDefault(pid, 0.0);
            double subtotal = qty * unitP;
            ps.setInt(1, tabId);
            ps.setInt(2, pid);
            ps.setInt(3, qty);
            ps.setDouble(4, unitP);
            ps.setDouble(5, subtotal);
            ps.addBatch();
        }
        ps.executeBatch();
    }

    private int createNewTab(Connection conn, int teacherId) throws SQLException {
        int pendingStatusId = getStatusId(conn, "pending");

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tabs (teacher_id, total, status, created_at) VALUES (?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, teacherId);
        ps.setDouble(2, calculateCartTotal());
        ps.setInt(3, pendingStatusId);
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        int tabId = keys.next() ? keys.getInt(1) : 0;

        // Add items
        addItemsToExistingTab(conn, tabId);

        return tabId;
    }

    private int getStatusId(Connection conn, String statusName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM tabs_status WHERE LOWER(tabs_status_name) = ? LIMIT 1");
        ps.setString(1, statusName.toLowerCase());
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt("id") : 1;
    }

    private void deductProductStock(Connection conn, Map<Integer, Integer> cart) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE product_list SET Item_quantity = Item_quantity - ? WHERE Item_id = ?");

        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            ps.setInt(1, e.getValue());
            ps.setInt(2, e.getKey());
            ps.addBatch();
        }
        ps.executeBatch();
    }

    private boolean validateStockAvailability(Connection conn) throws SQLException {
        for (Map.Entry<Integer, Integer> entry : cartQty.entrySet()) {
            int productId = entry.getKey();
            int requestedQty = entry.getValue();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT Item_quantity FROM product_list WHERE Item_id = ?");
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int availableStock = rs.getInt("Item_quantity");
                if (requestedQty > availableStock) {
                    String productName = cartName.getOrDefault(productId, "Unknown");
                    JOptionPane.showMessageDialog(this,
                            "Not enough stock for " + productName + "!\n" +
                                    "Available: " + availableStock + ", Requested: " + requestedQty,
                            "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────
    private JLabel makeFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 10));
        lbl.setForeground(C_MUTED);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return lbl;
    }
}