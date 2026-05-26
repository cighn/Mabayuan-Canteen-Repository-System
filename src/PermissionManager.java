import java.sql.*;

public class PermissionManager {

    // Check role-level permission from role_permissions table
    public static boolean hasRolePermission(String role, String permission) {
        String sql = "SELECT is_allowed FROM role_permissions " +
                "WHERE role_name = ? AND permission_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, permission);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("is_allowed") == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if a specific user was individually granted a permission by Admin
    public static boolean hasGrantedPermission(int entityId, String permission) {
        String sql = "SELECT is_granted FROM entity_permissions " +
                "WHERE entity_id = ? AND permission_name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entityId);
            ps.setString(2, permission);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("is_granted") == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Master check — Admin always yes, Staff always no, Senior_Staff checks grant
    // first
    public static boolean canDo(String role, int entityId, String permission) {
        if ("Admin".equals(role))
            return true;
        if ("Staff".equals(role))
            return false;
        if (hasGrantedPermission(entityId, permission))
            return true;
        return hasRolePermission(role, permission);
    }
}