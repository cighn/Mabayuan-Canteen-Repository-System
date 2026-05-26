import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.Properties;
import javax.imageio.ImageIO;

public class Frame extends JFrame {

    // ─────────────────────────────────────────────────────────────────
    // Color Palette
    // ─────────────────────────────────────────────────────────────────
    private static final Color C_BG = new Color(0x0D1F14);
    private static final Color C_PANEL = new Color(0x102A18);
    private static final Color C_CARD = new Color(0x163320);
    private static final Color C_ACCENT = new Color(0x2E7D4F);
    private static final Color C_ACCENT_HVR = new Color(0x3DAA6A);
    private static final Color C_BORDER = new Color(0x2A4A35);
    private static final Color C_INPUT_BG = new Color(0x0F2218);
    private static final Color C_TEXT = new Color(0xECF5EC);
    private static final Color C_MUTED = new Color(0x7AAA8A);
    private static final Color C_PLACEHOLDER = new Color(0x4A7A5A);

    // ─────────────────────────────────────────────────────────────────
    // Remember Me file — saved in the project root folder
    // ─────────────────────────────────────────────────────────────────
    private static final String REMEMBER_FILE = "remember_me.properties";

    // ─────────────────────────────────────────────────────────────────
    // UI Components
    // ─────────────────────────────────────────────────────────────────
    JTextField usernameField;
    JTextField gmailField;
    JPasswordField passwordField;
    JCheckBox rememberMe;
    JButton loginButton;
    private JButton eyeBtn;
    private boolean passwordVisible = false;
    private JPanel passwordWrapper;

    // ─────────────────────────────────────────────────────────────────
    // Images
    // ─────────────────────────────────────────────────────────────────
    private BufferedImage logoImage;
    private BufferedImage schoolImage;

    // ─────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────
    Frame() {
        loadImages();
        setupFrame();
        setupSplitPane();
        loadRememberedCredentials(); // load after fields are built
    }

    // ─────────────────────────────────────────────────────────────────
    // Remember Me — Save
    // ─────────────────────────────────────────────────────────────────
    private void saveCredentials(String username, String email, String password) {
        Properties props = new Properties();
        props.setProperty("username", username);
        props.setProperty("email", email);
        props.setProperty("password", password);
        try (FileOutputStream fos = new FileOutputStream(REMEMBER_FILE)) {
            props.store(fos, "Saved login credentials");
        } catch (IOException e) {
            System.err.println("Could not save credentials: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Remember Me — Load
    // ─────────────────────────────────────────────────────────────────
    private void loadRememberedCredentials() {
        File file = new File(REMEMBER_FILE);
        if (!file.exists())
            return;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            String username = props.getProperty("username", "");
            String email = props.getProperty("email", "");
            String password = props.getProperty("password", "");

            if (!username.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                usernameField.setText(username);
                usernameField.setForeground(C_TEXT);

                gmailField.setText(email);
                gmailField.setForeground(C_TEXT);

                passwordField.setText(password);
                passwordField.setForeground(C_TEXT);
                passwordField.setEchoChar('●');

                rememberMe.setSelected(true);
            }
        } catch (IOException e) {
            System.err.println("Could not load credentials: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Remember Me — Clear
    // ─────────────────────────────────────────────────────────────────
    private void clearSavedCredentials() {
        File file = new File(REMEMBER_FILE);
        if (file.exists())
            file.delete();
    }

    // ─────────────────────────────────────────────────────────────────
    // Image Loading
    // ─────────────────────────────────────────────────────────────────
    private void loadImages() {
        logoImage = loadImage("/logo.jpg");
        schoolImage = loadImage("/school.png");
        if (logoImage == null)
            logoImage = loadImage("school.png");
        if (schoolImage == null)
            schoolImage = loadImage("logo.jpg");
    }

    private BufferedImage loadImage(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url != null)
                return ImageIO.read(url);
            java.io.File f = new java.io.File("src" + (path.startsWith("/") ? path : "/" + path));
            if (!f.exists())
                f = new java.io.File(path.startsWith("/") ? path.substring(1) : path);
            if (f.exists())
                return ImageIO.read(f);
        } catch (IOException e) {
            System.err.println("Could not load image: " + path + " — " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────
    // Frame Setup
    // ─────────────────────────────────────────────────────────────────
    private void setupFrame() {
        setTitle("Mabayuan Canteen - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 500));
        setSize(1050, 580);
        setLocationRelativeTo(null);
    }

    private void setupSplitPane() {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createLeftPanel(),
                createRightPanel());
        split.setResizeWeight(0.45);
        split.setDividerSize(0);
        split.setBorder(null);
        setContentPane(split);
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────
    // Left Panel
    // ─────────────────────────────────────────────────────────────────
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                int w = getWidth(), h = getHeight();
                g2.setColor(C_PANEL);
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.setStroke(new BasicStroke(1f));
                for (int i = -h; i < w + h; i += 20)
                    g2.drawLine(i, 0, i + h, h);
                if (logoImage != null) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
                    drawCover(g2, logoImage, w, h);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                if (schoolImage != null)
                    drawCenteredImage(g2, schoolImage, w, h);
                drawBottomGradient(g2, w, h);
                GradientPaint fade = new GradientPaint(w - 50, 0, new Color(0, 0, 0, 0), w, 0, C_BG);
                g2.setPaint(fade);
                g2.fillRect(w - 50, 0, 50, h);
                g2.dispose();
            }
        };
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        panel.add(createSchoolLabel(), getBottomAnchorConstraints());
        return panel;
    }

    private void drawCenteredImage(Graphics2D g2, BufferedImage img, int w, int h) {
        if (img == null)
            return;
        int padding = (int) (w * 0.08);
        int maxWidth = w - (2 * padding);
        int maxHeight = (int) (h * 0.65);
        double scale = Math.min((double) maxWidth / img.getWidth(), (double) maxHeight / img.getHeight());
        int drawW = (int) (img.getWidth() * scale);
        int drawH = (int) (img.getHeight() * scale);
        int drawX = (w - drawW) / 2;
        int drawY = (h - drawH) / 2 - 30;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2.drawImage(img, drawX, drawY, drawW, drawH, null);
    }

    private void drawBottomGradient(Graphics2D g2, int w, int h) {
        GradientPaint grad = new GradientPaint(0, h * 0.60f, new Color(0, 0, 0, 0), 0, h, C_BG);
        g2.setPaint(grad);
        g2.fillRect(0, (int) (h * 0.60), w, h);
    }

    private JLabel createSchoolLabel() {
        JLabel label = new JLabel(
                "<html><div style='text-align:center; line-height:1.6;'>" +
                        "<b>MABAYUAN ELEMENTARY SCHOOL</b><br>" +
                        "<span style='color:#7AAA8A; font-size:11px;'>CANTEEN INVENTORY SYSTEM</span>" +
                        "</div></html>",
                JLabel.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(C_TEXT);
        return label;
    }

    // ─────────────────────────────────────────────────────────────────
    // Right Panel
    // ─────────────────────────────────────────────────────────────────
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(C_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = gbc.weighty = 1.0;
        gbc.insets = new Insets(40, 45, 40, 45);
        panel.add(createLoginCard(), gbc);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────────
    // Login Card
    // ─────────────────────────────────────────────────────────────────
    private JPanel createLoginCard() {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                int w = getWidth(), h = getHeight(), r = 16;
                g2.setColor(new Color(0, 0, 0, 55));
                g2.fillRoundRect(4, 6, w - 4, h - 4, r, r);
                g2.setColor(C_CARD);
                g2.fillRoundRect(0, 0, w - 5, h - 5, r, r);
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 6, h - 6, r, r);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(30, 34, 34, 34));

        int row = 0;
        card.add(createTitleBar(), rowConstraints(row++, 0, 0, 24, 0));

        usernameField = createTextField(PH_USERNAME);
        row = addFormField(card, row, "USERNAME", usernameField, 14);

        gmailField = createTextField(PH_EMAIL);
        row = addFormField(card, row, "EMAIL", gmailField, 14);

        row = addFormField(card, row, "PASSWORD", createPasswordRow(), 10);

        card.add(createRememberRow(), rowConstraints(row++, 0, 0, 20, 0));
        card.add(createLoginButton(), rowConstraints(row, 0, 0, 0, 0));
        return card;
    }

    private int addFormField(JPanel card, int row, String labelText, JComponent field, int bottomGap) {
        card.add(createLabel(labelText), rowConstraints(row++, 0, 0, 5, 0));
        card.add(field, rowConstraints(row++, 0, 0, bottomGap, 0));
        return row;
    }

    private GridBagConstraints rowConstraints(int y, int top, int left, int bottom, int right) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(top, left, bottom, right);
        return gbc;
    }

    // ─────────────────────────────────────────────────────────────────
    // Title Bar
    // ─────────────────────────────────────────────────────────────────
    private JPanel createTitleBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setOpaque(false);

        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                g2.setPaint(new GradientPaint(0, 0, C_ACCENT, getWidth(), 0, C_ACCENT_HVR));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(44, 4);
            }
        };
        pill.setOpaque(false);
        pill.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lock = new JLabel("🔒");
        lock.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        lock.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Welcome Back");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(C_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to access the inventory system");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(C_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        bar.add(pill);
        bar.add(Box.createVerticalStrut(10));
        bar.add(lock);
        bar.add(Box.createVerticalStrut(6));
        bar.add(title);
        bar.add(Box.createVerticalStrut(3));
        bar.add(sub);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────
    // Text Field
    // ─────────────────────────────────────────────────────────────────
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                g2.setColor(C_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(isFocusOwner() ? C_ACCENT : C_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        field.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        field.setForeground(C_PLACEHOLDER);
        field.setCaretColor(C_ACCENT_HVR);
        field.setText(placeholder);
        field.setPreferredSize(new Dimension(0, 46));

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(C_TEXT);
                }
                field.repaint();
            }

            public void focusLost(FocusEvent e) {
                if (field.getText().isBlank()) {
                    field.setForeground(C_PLACEHOLDER);
                    field.setText(placeholder);
                }
                field.repaint();
            }
        });
        return field;
    }

    // ─────────────────────────────────────────────────────────────────
    // Password Row
    // ─────────────────────────────────────────────────────────────────
    private JPanel createPasswordRow() {
        passwordField = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                g2.setColor(C_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        passwordField.setOpaque(false);
        passwordField.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        passwordField.setForeground(C_PLACEHOLDER);
        passwordField.setCaretColor(C_ACCENT_HVR);
        passwordField.setEchoChar((char) 0);
        passwordField.setText(PH_PASSWORD);
        passwordField.setPreferredSize(new Dimension(0, 46));
        passwordField.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));

        passwordField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (new String(passwordField.getPassword()).equals(PH_PASSWORD)) {
                    passwordField.setText("");
                    passwordField.setForeground(C_TEXT);
                    passwordField.setEchoChar(passwordVisible ? (char) 0 : '●');
                }
                if (passwordWrapper != null)
                    passwordWrapper.repaint();
            }

            public void focusLost(FocusEvent e) {
                if (new String(passwordField.getPassword()).isBlank()) {
                    passwordField.setForeground(C_PLACEHOLDER);
                    passwordField.setEchoChar((char) 0);
                    passwordField.setText(PH_PASSWORD);
                }
                if (passwordWrapper != null)
                    passwordWrapper.repaint();
            }
        });

        eyeBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                Color ic = getModel().isRollover() ? C_ACCENT_HVR : C_MUTED;
                g2.setColor(ic);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int ew = 9, eh = 6;
                g2.drawArc(cx - ew, cy - eh, ew * 2, eh * 2, 0, 180);
                g2.drawArc(cx - ew, cy - eh, ew * 2, eh * 2, 180, 180);
                g2.fillOval(cx - 3, cy - 3, 6, 6);
                if (!passwordVisible) {
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - ew + 2, cy + eh - 1, cx + ew - 2, cy - eh + 1);
                }
                g2.dispose();
            }
        };
        eyeBtn.setPreferredSize(new Dimension(42, 46));
        eyeBtn.setFocusable(false);
        eyeBtn.setBorderPainted(false);
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setOpaque(false);
        eyeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeBtn.addActionListener(e -> {
            String pw = new String(passwordField.getPassword());
            if (!pw.equals(PH_PASSWORD) && !pw.isBlank()) {
                passwordVisible = !passwordVisible;
                passwordField.setEchoChar(passwordVisible ? (char) 0 : '●');
                eyeBtn.repaint();
            }
        });

        passwordWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                g2.setColor(C_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                boolean focused = passwordField.isFocusOwner();
                g2.setColor(focused ? C_ACCENT : C_BORDER);
                g2.setStroke(new BasicStroke(focused ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        passwordWrapper.setOpaque(false);
        passwordWrapper.setPreferredSize(new Dimension(0, 46));
        passwordWrapper.add(passwordField, BorderLayout.CENTER);
        passwordWrapper.add(eyeBtn, BorderLayout.EAST);
        return passwordWrapper;
    }

    // ─────────────────────────────────────────────────────────────────
    // Remember Row — with working Remember Me + Forgot Password message
    // ─────────────────────────────────────────────────────────────────
    private JPanel createRememberRow() {
        rememberMe = new JCheckBox("Remember me");
        rememberMe.setOpaque(false);
        rememberMe.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rememberMe.setForeground(C_MUTED);
        rememberMe.setFocusable(false);
        rememberMe.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // If unchecked, clear the saved file immediately
        rememberMe.addActionListener(e -> {
            if (!rememberMe.isSelected()) {
                clearSavedCredentials();
            }
        });

        // Forgot password — shows contact admin message (no email system in local app)
        JLabel forgotLabel = new JLabel("Forgot password?");
        forgotLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        forgotLabel.setForeground(C_ACCENT_HVR);
        forgotLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(
                        Frame.this,
                        "<html><div style='width:220px;'>" +
                                "<b>Forgot your password?</b><br><br>" +
                                "Please contact your <b>Administrator</b> to reset your passcode.<br><br>" +
                                "<span style='color:#888;'>The admin can update your credentials<br>" +
                                "through the Identity / Users panel.</span>" +
                                "</div></html>",
                        "Password Reset",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            public void mouseEntered(MouseEvent e) {
                forgotLabel.setForeground(C_TEXT);
            }

            public void mouseExited(MouseEvent e) {
                forgotLabel.setForeground(C_ACCENT_HVR);
            }
        });

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(rememberMe, BorderLayout.WEST);
        row.add(forgotLabel, BorderLayout.EAST);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────
    // Login Button — saves credentials if Remember Me is checked
    // ─────────────────────────────────────────────────────────────────
    private JButton createLoginButton() {
        loginButton = new JButton("Sign In ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableQualityHints(g2);
                Color top = getModel().isRollover() ? C_ACCENT_HVR : C_ACCENT;
                Color bot = getModel().isRollover() ? C_ACCENT : new Color(0x1E5C38);
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginButton.setOpaque(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setBorderPainted(false);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setFocusable(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(0, 50));

        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String email = gmailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            // Reject placeholder text as real input
            if (username.equals(PH_USERNAME))
                username = "";
            if (email.equals(PH_EMAIL))
                email = "";
            if (password.equals(PH_PASSWORD))
                password = "";

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill in all fields.", "Login Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "SELECT entity_id, entity_name, entity_role FROM entity_table " +
                        "WHERE entity_name = ? AND entity_email = ? AND entity_passcode = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int entityId = rs.getInt("entity_id");
                    String role = rs.getString("entity_role");

                    // Save or clear credentials based on checkbox
                    if (rememberMe.isSelected()) {
                        saveCredentials(username, email, password);
                    } else {
                        clearSavedCredentials();
                    }

                    SessionManager.set(username, role, entityId);
                    dispose();
                    new Dashboard(username, role, entityId);

                } else {
                    JOptionPane.showMessageDialog(this,
                            "Invalid username, email, or password.\nPlease try again.",
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Database error: " + ex.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        loginButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                loginButton.repaint();
            }

            public void mouseExited(MouseEvent e) {
                loginButton.repaint();
            }
        });
        return loginButton;
    }

    // ─────────────────────────────────────────────────────────────────
    // Field Label
    // ─────────────────────────────────────────────────────────────────
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(C_MUTED);
        return label;
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────
    private void enableQualityHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    }

    private void drawCover(Graphics2D g2, BufferedImage img, int w, int h) {
        double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int drawW = (int) Math.round(img.getWidth() * scale);
        int drawH = (int) Math.round(img.getHeight() * scale);
        g2.drawImage(img, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
    }

    private GridBagConstraints getBottomAnchorConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    // ─────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────
    private static final String PH_USERNAME = "👤  ENTER YOUR USERNAME";
    private static final String PH_EMAIL = "✉  ENTER YOUR EMAIL";
    private static final String PH_PASSWORD = "🔑  ENTER YOUR PASSWORD";
}