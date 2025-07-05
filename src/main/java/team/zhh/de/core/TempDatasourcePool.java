package team.zhh.de.core;


import org.springframework.stereotype.Component;
import team.zhh.de.config.DatabaseConfig;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TempDatasourcePool {
    private final Map<String, DataSource> pool = new ConcurrentHashMap<>();
    private final DatabaseConfig databaseConfig;

    public TempDatasourcePool(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public DataSource getDataSource(String url, String username, String password) {
        String key = url + "|" + username + "|" + password;
        return pool.computeIfAbsent(key, k -> databaseConfig.createDataSource(url, username, password));
    }

    public void closeAll() {
        for (DataSource ds : pool.values()) {
            try {
                ds.getConnection().close(); // 关闭连接池（如果是连接池实现）
            } catch (Exception ignored) {}
        }
        pool.clear();
    }
} 