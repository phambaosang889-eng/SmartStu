package com.smartstudy.persistence;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.InitialContext;

/** JDBC boundary. No fallback from a failed database to sample data. */
public final class Database {
    private Database() {}

    private static final ThreadLocal<Connection> TRANSACTION = new ThreadLocal<>();
    private static final ThreadLocal<java.util.Map<String, Object>> READS = new ThreadLocal<>();

    public static void requestScope() {
        READS.set(new java.util.HashMap<>());
    }

    public static void closeScope() {
        READS.remove();
    }

    public static void invalidate() {
        if (READS.get() != null) READS.get().clear();
    }

    @SuppressWarnings("unchecked")
    public static <T> T cached(String key, java.util.function.Supplier<T> query) {
        var cache = READS.get();
        if (cache == null) return query.get();
        if (cache.containsKey(key)) return (T) cache.get(key);
        T value = query.get();
        cache.put(key, value);
        return value;
    }

    private static final java.util.Properties LOCAL = loadConfig();

    private static java.util.Properties loadConfig() {
        var p = new java.util.Properties();
        String path =
                System.getProperty("SMARTSTU_CONFIG_FILE", System.getenv("SMARTSTU_CONFIG_FILE"));
        boolean localWorkspace = false;
        if ((path == null || path.isBlank())
                && !"memory"
                        .equals(
                                System.getProperty(
                                        "SMARTSTU_STORAGE", System.getenv("SMARTSTU_STORAGE")))) {
            // NetBeans deploys this project's build/web directory without inheriting the test
            // server's VM flags.
            // Only discover the private file for an exact exploded NetBeans build, never for
            // arbitrary WARs.
            try {
                var classes =
                        java.nio.file.Path.of(
                                        Database.class
                                                .getProtectionDomain()
                                                .getCodeSource()
                                                .getLocation()
                                                .toURI())
                                .toAbsolutePath()
                                .normalize();
                var project = classes.getParent().getParent().getParent().getParent();
                var expected = project.resolve("build/web/WEB-INF/classes");
                var config = project.resolve(".runtime/db.local.properties");
                if (classes.equals(expected)
                        && java.nio.file.Files.isRegularFile(
                                project.resolve("nbproject/project.xml"))
                        && java.nio.file.Files.isRegularFile(config)) {
                    path = config.toString();
                    localWorkspace = true;
                }
            } catch (java.net.URISyntaxException | RuntimeException ignored) {
                /* External deployment requires explicit configuration. */
            }
        }
        if (path != null && !path.isBlank())
            try (var in = java.nio.file.Files.newInputStream(java.nio.file.Path.of(path))) {
                p.load(in);
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Không đọc được cấu hình bên ngoài ứng dụng.", e);
            }
        if (localWorkspace)
            p.remove(
                    "SMARTSTU_DB_JNDI"); // This resource belongs to the separate test Tomcat, not
                                         // NetBeans.
        return p;
    }

    public static boolean memory() {
        return "memory".equals(setting("SMARTSTU_STORAGE", "mysql"));
    }

    public static String setting(String name, String fallback) {
        String value = System.getProperty(name);
        if (value == null) value = System.getenv(name);
        if (value == null) value = LOCAL.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static Connection open() throws SQLException {
        String jndi = setting("SMARTSTU_DB_JNDI", "");
        if (!jndi.isEmpty()) {
            try {
                return utc(((DataSource) new InitialContext().lookup(jndi)).getConnection());
            } catch (javax.naming.NamingException e) {
                throw new SQLException("JNDI datasource unavailable", e);
            }
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Connector/J missing", e);
        }
        return utc(
                DriverManager.getConnection(
                        setting(
                                "SMARTSTU_DB_URL",
                                "jdbc:mysql://127.0.0.1:3306/smartstu?connectionTimeZone=UTC&characterEncoding=UTF-8&connectTimeout=5000&socketTimeout=10000"),
                        setting("SMARTSTU_DB_USER", "smartstu"),
                        setting("SMARTSTU_DB_PASSWORD", "")));
    }

    private static Connection utc(Connection c) throws SQLException {
        try (var s = c.prepareStatement("SET time_zone = '+00:00'")) {
            s.setQueryTimeout(5);
            s.execute();
            return c;
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    public static <T> T use(Work<T> work) {
        try {
            Connection current = TRANSACTION.get();
            if (current != null) return work.run(current);
            try (Connection c = open()) {
                return work.run(c);
            }
        } catch (SQLException e) {
            // SQL state is useful for diagnosis; never log passwords or connection URLs.
            java.util.logging.Logger.getLogger(Database.class.getName())
                    .warning("Database operation failed; SQLState=" + e.getSQLState());
            throw new StorageException(e);
        }
    }

    public static void begin() throws SQLException {
        if (TRANSACTION.get() != null) throw new SQLException("Nested request transaction");
        Connection c = open();
        try {
            c.setAutoCommit(false);
            TRANSACTION.set(c);
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }

    public static void end(boolean commit) throws SQLException {
        Connection c = TRANSACTION.get();
        TRANSACTION.remove();
        if (c != null)
            try (c) {
                if (commit) c.commit();
                else c.rollback();
            }
    }

    public static final class StorageException extends RuntimeException {
        public StorageException(Throwable cause) {
            super(
                    "Không thể lưu hoặc đọc dữ liệu. Vui lòng thử lại; nếu lỗi tiếp diễn hãy liên"
                        + " hệ quản trị viên.",
                    cause);
        }
    }
}
