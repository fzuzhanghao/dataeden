package team.zhh.de.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import team.zhh.de.core.TempDatasourcePool;
import team.zhh.base.model.ColumnMetadata;

import javax.sql.DataSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class DatabaseService {


    @Autowired
    private TempDatasourcePool tempDatasourcePool;


    public boolean connect(String url, String username, String password) {
        try {
            tempDatasourcePool.getDataSource(url, username, password);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(),e);
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
            log.debug("Debug: Extracted schema = " + schema);
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
            log.error(e.getMessage(),e);
        }
        return tables;
    }

    public List<ColumnMetadata> getTableColumns(String url, String username, String password, String tableName) {
        List<ColumnMetadata> columns = new ArrayList<>();
        DataSource dataSource = tempDatasourcePool.getDataSource(url, username, password);
        try {
            String schema = getCurrentSchema(dataSource, url);
            log.debug("Debug: Schema = " + schema + ", Table = " + tableName);
            
            try (Connection conn = dataSource.getConnection();
                 ResultSet rs = conn.getMetaData().getColumns(null, schema, tableName, null)) {
                List<String> primaryKeys = getPrimaryKeys(conn, schema, tableName);
                log.debug("Debug: Primary keys = " + primaryKeys);
                
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    int size = rs.getInt("COLUMN_SIZE");
                    int digit = rs.getInt("DECIMAL_DIGITS");
                    int nullableFlag = rs.getInt("NULLABLE");
                    String remarks = rs.getString("REMARKS");
                    boolean isNullable = (nullableFlag == DatabaseMetaData.columnNullable);
                    boolean isAutoIncrement = "YES".equals(rs.getString("IS_AUTOINCREMENT"));
                    boolean isPrimaryKey = primaryKeys.contains(colName);

                    log.debug("Debug: Column = " + colName + ", Type = " + typeName + ", Size = " + size + ", Digits = " + digit);
                    
                    columns.add(new ColumnMetadata(
                        colName, typeName, size, digit,
                        isPrimaryKey, isAutoIncrement, isNullable,remarks
                    ));
                }
            }
        } catch (SQLException e) {
            log.error("Error getting table columns: " + e.getMessage(),e);
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