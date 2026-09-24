package common;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class DatabaseConfig {

    public enum DatabaseType {
        MYSQL,
        POSTGRESQL,
        H2_TEST
    }

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String POSTGRES_URL = "jdbc:postgresql://localhost:5432/ecommerce_db";
    private static final String H2_URL = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1";

    public static DataSource getDataSource(DatabaseType type) {
        switch (type) {
            case POSTGRESQL:
                return new SimpleDataSource(POSTGRES_URL, "postgres", "postgres_password");
            case H2_TEST:
                return new SimpleDataSource(H2_URL, "sa", "");
            case MYSQL:
            default:
                return new SimpleDataSource(MYSQL_URL, "root", "mysql_password");
        }
    }

    public static class SimpleDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;

        public SimpleDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
