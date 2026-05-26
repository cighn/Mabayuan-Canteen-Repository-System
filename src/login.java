import javax.swing.*;

public class login {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Frame frame = new Frame();
            frame.setSize(780, 620);
            frame.setTitle("Mabayuan Canteen - Login");
            frame.setResizable(true);
        });
    }
}