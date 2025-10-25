// Add these checks in the DataStore class:

public static class DataStore {
    // ... existing code ...

    public static Car findCarById(String carId) {
        if (carId == null) return null;
        
        String sql = "SELECT * FROM cars WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, carId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapCar(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void updateBookingStatus(String bookingId, String newStatus) {
        if (bookingId == null || newStatus == null) return;
        
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, bookingId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Database error: Could not update booking status.", 
                "Database Write Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
