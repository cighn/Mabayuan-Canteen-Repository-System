public class SessionManager {
    private static String currentUsername;
    private static String currentRole;
    private static int currentEntityId;

    public static void set(String username, String role, int entityId) {
        currentUsername = username;
        currentRole = role;
        currentEntityId = entityId;
    }

    public static String getRole() {
        return currentRole;
    }

    public static String getUsername() {
        return currentUsername;
    }

    public static int getEntityId() {
        return currentEntityId;
    }

    public static boolean isAdmin() {
        return "Admin".equals(currentRole);
    }

    public static boolean isSeniorStaff() {
        return "Senior_Staff".equals(currentRole);
    }

    public static boolean isStaff() {
        return "Staff".equals(currentRole);
    }
}