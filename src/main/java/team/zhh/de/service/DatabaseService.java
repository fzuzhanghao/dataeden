package team.zhh.de.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.zhh.de.config.DatabaseConfig;
import team.zhh.de.core.TempDatasourcePool;
import team.zhh.base.model.ColumnMetadata;

import javax.sql.DataSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

@Service
public class DatabaseService {

    private final DatabaseConfig databaseConfig;

    @Autowired
    private TempDatasourcePool tempDatasourcePool;

    @Autowired
    public DatabaseService(DatabaseConfig databaseConfig, TempDatasourcePool tempDatasourcePool) {
        this.databaseConfig = databaseConfig;
        this.tempDatasourcePool = tempDatasourcePool;
    }

    public boolean connect(String url, String username, String password) {
        try {
            tempDatasourcePool.getDataSource(url, username, password);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String extractSchemaFromUrl(String url) {
        try {
            String[] parts = url.split("\\?");
            if (parts.length > 1) {
                String query = parts[1];
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2 && kv[0].equalsIgnoreCase("currentSchema")) {
                        return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getCurrentSchema(DataSource dataSource, String url) throws SQLException {
        String schema = extractSchemaFromUrl(url);
        if (schema != null) return schema;
        try (Connection conn = dataSource.getConnection()) {
            schema = conn.getCatalog();
            if (schema == null) {
                schema = conn.getSchema();
            }
        }
        return schema;
    }

    public List<String> getTables(String url, String username, String password) {
        List<String> tables = new ArrayList<>();
        DataSource dataSource = tempDatasourcePool.getDataSource(url, username, password);
        try {
            String schema = getCurrentSchema(dataSource, url);
            try (Connection conn = dataSource.getConnection();
                 ResultSet rs = conn.getMetaData().getTables(null, schema, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }

    public List<ColumnMetadata> getTableColumns(String url, String username, String password, String tableName) {
        List<ColumnMetadata> columns = new ArrayList<>();
        DataSource dataSource = tempDatasourcePool.getDataSource(url, username, password);
        try {
            String schema = getCurrentSchema(dataSource, url);
            try (Connection conn = dataSource.getConnection();
                 ResultSet rs = conn.getMetaData().getColumns(null, schema, tableName, null)) {
                List<String> primaryKeys = getPrimaryKeys(conn, schema, tableName);
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    int size = rs.getInt("COLUMN_SIZE");
                    int digit = rs.getInt("DIGIT_SIZE");
                    int nullableFlag = rs.getInt("NULLABLE");
                    boolean isNullable = (nullableFlag == DatabaseMetaData.columnNullable);
                    boolean isAutoIncrement = "YES".equals(rs.getString("IS_AUTOINCREMENT"));
                    boolean isPrimaryKey = primaryKeys.contains(colName);

                    columns.add(new ColumnMetadata(
                        colName, typeName, size,digit,
                        isPrimaryKey, isAutoIncrement, isNullable
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columns;
    }

    private List<String> getPrimaryKeys(Connection conn, String schema, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(null, schema, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        return primaryKeys;
    }
}