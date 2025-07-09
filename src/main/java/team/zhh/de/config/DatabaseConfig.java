package team.zhh.de.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DatabaseConfig {
    private static final Map<String, String> DRIVER_MAP = new HashMap<>();
    
    static {
        DRIVER_MAP.put("mysql", "com.mysql.cj.jdbc.Driver");
        DRIVER_MAP.put("postgresql", "org.postgresql.Driver");
        DRIVER_MAP.put("oracle", "oracle.jdbc.OracleDriver");
        DRIVER_MAP.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    
    public DataSource createDataSource(String url, String username, String password) {
        String dbType = detectDbType(url);
        String driver = DRIVER_MAP.get(dbType);
        
        if (driver == null) {
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        
        return dataSource;
    }
    
    private String detectDbType(String url) {
        if (url.contains("mysql")) return "mysql";
        if (url.contains("postgresql")) return "postgresql";
        if (url.contains("oracle")) return "oracle";
        if (url.contains("sqlserver")) return "sqlserver";
        throw new IllegalArgumentException("Could not detect database type from URL");
    }
}