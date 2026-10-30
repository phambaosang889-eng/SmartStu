package com.smartstudy.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import java.sql.*;

/** Release only drivers owned by this webapp; container-managed pools remain container-owned. */
@WebListener
public final class JdbcLifecycle implements ServletContextListener {
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        var loader = getClass().getClassLoader();
        var drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() != loader) continue;
            try {
                if (driver.getClass().getName().startsWith("com.mysql.")) {
                    Class<?> cleanup =
                            Class.forName(
                                    "com.mysql.cj.jdbc.AbandonedConnectionCleanupThread",
                                    false,
                                    loader);
                    cleanup.getMethod("checkedShutdown").invoke(null);
                }
                DriverManager.deregisterDriver(driver);
            } catch (ReflectiveOperationException | SQLException e) {
                event.getServletContext()
                        .log(
                                "JDBC webapp cleanup could not finish: "
                                        + e.getClass().getSimpleName());
            }
        }
    }
}
