import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class CategoriesPanel extends JPanel {

    private JPanel cardsContainer;
    private JTextField searchField;
    private JButton searchButton, resetButton, addButton;
    private String currentSearchKeyword = "";
    private JLabel totalCategoriesVal, largestCategoryVal, smallestCategoryVal, avgPerCategoryVal;

    private static final Color[] CAT_COLORS = {
            new Color(0xD85A30), new Color(0x378ADD),
            new Color(0xEF9F27), new Color(0x7F77DD),
            new Color(0x2E7D4F), new Color(0xD4537E)
    };

    public CategoriesPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(0x1E2E1E));

        // NORTH: stats + search bar
        JPanel northBlock = new JPanel();
        northBlock.setLayout(new BoxLayout(northBlock, BoxLayout.Y_AXIS));
        northBlock.setOpaque(false);
        northBlock.setBorder(new EmptyBorder(16, 16, 0, 16));

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        totalCategoriesVal = new JLabel("0");
        largestCategoryVal = new JLabel("0");
        smallestCategoryVal = new JLabel("0");
        avgPerCategoryVal = new JLabel("0");

        statsRow.add(makeStatCard("TOTAL CATEGORIES", totalCategoriesVal, "active", new Color(0x2E7D4F)));
        statsRow.add(makeStatCard("LARGEST", largestCategoryVal, "products", new Color(0x1565A0)));
        statsRow.add(makeStatCard("SMALLEST", smallestCategoryVal, "products", new Color(0x00695C)));
        statsRow.add(makeStatCard("AVG PER CATEGORY", avgPerCategoryVal, "items avg", new Color(0x6A1B9A)));

        // Action bar (Search, Reset, Add)
        JPanel actionBar = new JPanel(new BorderLayout(10, 0));
        actionBar.setOpaque(false);
        actionBar.setBorder(new EmptyBorder(12, 0, 12, 0));

        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(200, 34));
        searchField.setBackground(new Color(0x1A2A1A));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2E4A2E)),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));

        searchButton = makeActionButton("Search", new Color(0x2E7D4F));
        resetButton = makeActionButton("Reset", new Color(0x2E7D4F));
        addButton = makeActionButton("+ Add Category", new Color(0xD85A30));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(searchField);
        leftGroup.add(searchButton);
        leftGroup.add(resetButton);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightGroup.setOpaque(false);
        rightGroup.add(addButton);

        actionBar.add(leftGroup, BorderLayout.WEST);
        actionBar.add(rightGroup, BorderLayout.EAST);

        northBlock.add(statsRow);
        northBlock.add(actionBar);
        add(northBlock, BorderLayout.NORTH);

        // CENTER: simple 2‑column grid, no horizontal scroll
        cardsContainer = new JPanel(new GridLayout(0, 2, 12, 12));
        cardsContainer.setBackground(new Color(0x1E2E1E));
        cardsContainer.setBorder(new EmptyBorder(4, 16, 16, 16));

        JScrollPane scrollPane = new JScrollPane(cardsContainer);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(0x1E2E1E));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Button listeners
        searchButton.addActionListener(e -> {
            currentSearchKeyword = searchField.getText().trim().toLowerCase();
            refreshCategories();
        });
        resetButton.addActionListener(e -> {
            searchField.setText("");
            currentSearchKeyword = "";
            refreshCategories();
        });
        addButton.addActionListener(e -> openAddCategoryDialog());

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refreshCategories();
            }
        });
        refreshCategories();
    }

    // ================== UI Helpers ==================
    private JPanel makeStatCard(String title, JLabel valueLabel, String sub, Color accent) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(0x243824));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

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

    private JButton makeActionButton(String label, Color bg) {
        JButton btn = new JButton(label);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 34));
        return btn;
    }

    // ================== Data Loading ==================
    private void refreshCategories() {
        cardsContainer.removeAll();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, categories_name FROM categories_table";
            if (!currentSearchKeyword.isEmpty())
                sql += " WHERE LOWER(categories_name) LIKE ?";

            PreparedStatement psCat = conn.prepareStatement(sql);
            if (!currentSearchKeyword.isEmpty())
                psCat.setString(1, "%" + currentSearchKeyword + "%");

            ResultSet rsCat = psCat.executeQuery();
            List<CategoryData> categories = new ArrayList<>();

            while (rsCat.next()) {
                int catId = rsCat.getInt("id");
                String catName = rsCat.getString("categories_name");

                PreparedStatement psProd = conn.prepareStatement(
                        "SELECT Item_name, Item_price, Item_quantity " +
                                "FROM product_list WHERE Item_category = ? ORDER BY Item_quantity DESC");
                psProd.setInt(1, catId);
                ResultSet rsProd = psProd.executeQuery();

                List<ProductInfo> products = new ArrayList<>();
                while (rsProd.next()) {
                    products.add(new ProductInfo(
                            rsProd.getString("Item_name"),
                            rsProd.getDouble("Item_price"),
                            rsProd.getInt("Item_quantity")));
                }
                categories.add(new CategoryData(catId, catName, products));
            }

            updateStats(categories);

            int colorIdx = 0;
            for (CategoryData cat : categories) {
                Color color = CAT_COLORS[colorIdx % CAT_COLORS.length];
                cardsContainer.add(createCategoryCard(cat, color));
                colorIdx++;
            }

            cardsContainer.revalidate();
            cardsContainer.repaint();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStats(List<CategoryData> categories) {
        totalCategoriesVal.setText(String.valueOf(categories.size()));
        if (categories.isEmpty()) {
            largestCategoryVal.setText("0");
            smallestCategoryVal.setText("0");
            avgPerCategoryVal.setText("0");
            return;
        }
        int maxSize = 0, minSize = Integer.MAX_VALUE, total = 0;
        for (CategoryData cat : categories) {
            int s = cat.products.size();
            total += s;
            if (s > maxSize)
                maxSize = s;
            if (s < minSize)
                minSize = s;
        }
        largestCategoryVal.setText(String.valueOf(maxSize));
        smallestCategoryVal.setText(minSize == Integer.MAX_VALUE ? "0" : String.valueOf(minSize));
        avgPerCategoryVal.setText(String.format("%.1f", (double) total / categories.size()));
    }

    private JPanel createCategoryCard(CategoryData cat, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(new Color(0x243824));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameRow.setOpaque(false);
        JLabel colorDot = new JLabel("●");
        colorDot.setForeground(accent);
        colorDot.setFont(new Font("Arial", Font.PLAIN, 14));
        JLabel nameLbl = new JLabel(cat.name);
        nameLbl.setFont(new Font("Arial", Font.BOLD, 15));
        nameLbl.setForeground(Color.WHITE);
        nameRow.add(colorDot);
        nameRow.add(nameLbl);

        JLabel countLbl = new JLabel(cat.products.size() + " products");
        countLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        countLbl.setForeground(new Color(0x8AAA8A));

        JButton editBtn = makeActionButton("Edit", new Color(0x3A5A3A));
        editBtn.setPreferredSize(new Dimension(60, 26));
        editBtn.addActionListener(e -> openEditCategoryDialog(cat.id, cat.name));

        JPanel headerLeft = new JPanel(new BorderLayout());
        headerLeft.setOpaque(false);
        headerLeft.add(nameRow, BorderLayout.NORTH);
        headerLeft.add(countLbl, BorderLayout.SOUTH);

        header.add(headerLeft, BorderLayout.WEST);
        header.add(editBtn, BorderLayout.EAST);

        // Products panel
        JPanel productsPanel = new JPanel();
        productsPanel.setLayout(new BoxLayout(productsPanel, BoxLayout.Y_AXIS));
        productsPanel.setOpaque(false);

        int limit = 4;
        int total = cat.products.size();
        int maxQty = total > 0 ? cat.products.stream().mapToInt(p -> p.qty).max().orElse(1) : 1;

        for (int i = 0; i < Math.min(limit, total); i++) {
            ProductInfo p = cat.products.get(i);
            productsPanel.add(createProductRow(p, accent, maxQty));
            productsPanel.add(Box.createVerticalStrut(6));
        }

        if (total > limit) {
            String moreText = "+ " + (total - limit) + " more";
            JLabel moreLbl = new JLabel(moreText);
            moreLbl.setFont(new Font("Arial", Font.ITALIC, 10));
            moreLbl.setForeground(new Color(0x6A8A6A));
            productsPanel.add(moreLbl);
        }

        card.add(header, BorderLayout.NORTH);
        card.add(productsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createProductRow(ProductInfo p, Color barColor, int maxQty) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel nameLbl = new JLabel(p.name);
        nameLbl.setFont(new Font("Arial", Font.PLAIN, 12));
        nameLbl.setForeground(new Color(0xCCCCCC));

        JLabel metaLbl = new JLabel(String.format("₱%.0f · %d", p.price, p.qty));
        metaLbl.setFont(new Font("Arial", Font.BOLD, 11));
        metaLbl.setForeground(new Color(0xAAAAAA));
        metaLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        // Stock bar (simple, no forced width)
        int finalMaxQty = maxQty;
        JPanel barWrapper = new JPanel(new BorderLayout());
        barWrapper.setOpaque(false);
        JPanel barBg = new JPanel(null) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0x1A2A1A));
                g.fillRect(0, 4, getWidth(), 6);
                g.setColor(barColor);
                int w = (int) ((double) p.qty / finalMaxQty * getWidth());
                if (w > 0)
                    g.fillRect(0, 4, w, 6);
            }
        };
        barBg.setOpaque(false);
        barWrapper.add(barBg, BorderLayout.CENTER);

        row.add(nameLbl, BorderLayout.WEST);
        row.add(barWrapper, BorderLayout.CENTER);
        row.add(metaLbl, BorderLayout.EAST);
        return row;
    }

    private void openAddCategoryDialog() {
        String name = JOptionPane.showInputDialog(this, "Enter new category name:", "Add Category",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty())
            return;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("INSERT INTO categories_table (categories_name) VALUES (?)")) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            refreshCategories();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openEditCategoryDialog(int id, String oldName) {
        String newName = JOptionPane.showInputDialog(this, "Edit category name:", oldName);
        if (newName == null || newName.trim().isEmpty() || newName.equals(oldName))
            return;
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("UPDATE categories_table SET categories_name = ? WHERE id = ?")) {
            ps.setString(1, newName.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
            refreshCategories();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================== Data Holders ==================
    private static class CategoryData {
        int id;
        String name;
        List<ProductInfo> products;

        CategoryData(int id, String name, List<ProductInfo> products) {
            this.id = id;
            this.name = name;
            this.products = products;
        }
    }

    private static class ProductInfo {
        String name;
        double price;
        int qty;

        ProductInfo(String name, double price, int qty) {
            this.name = name;
            this.price = price;
            this.qty = qty;
        }
    }
}