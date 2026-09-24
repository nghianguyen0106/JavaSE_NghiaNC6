package repository;

import model.Payment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public class JdbcPaymentRepository implements PaymentRepository {

    private final DataSource dataSource;

    public JdbcPaymentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Payment payment) {
        String query = "INSERT INTO payments (amount, method) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDouble(1, payment.getAmount());
            stmt.setString(2, payment.getMethod().name());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu thông tin thanh toán vào DB", e);
        }
    }
}
