package team.zhh.base.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

/**
 * 数据库列元数据信息载体
 */
public record ColumnMetadata(
    String name,                 // 列名
    String typeName,             // 数据库类型名称（如 VARCHAR, INT）
    int size,                   // 列长度/精度
    int decimalDigits,          // 小数位数（针对数字类型）
    boolean nullable,           // 是否允许NULL
    String defaultValue,        // 默认值
    boolean autoIncrement,      // 是否自增列
    boolean primaryKey,         // 是否主键
    boolean foreignKey,         // 是否外键
    DataTypeCategory dataTypeCategory // 数据类型分类
) {


    public ColumnMetadata(String colName, String typeName, int size, int digit, boolean isPrimaryKey, boolean isAutoIncrement, boolean isNullable) {
        this(colName, typeName, size, digit, isNullable, null, isAutoIncrement, isPrimaryKey, false, determineDataTypeCategory(typeName));
    }

    // 数据类型分类枚举
    public enum DataTypeCategory {
        STRING, INTEGER, FLOAT, DATE, BOOLEAN, BINARY, UNKNOWN
    }

    /**
     * 从 ResultSet 解析列元数据（适用于 information_schema.columns 查询）
     */
    public static class ColumnsRowMapper implements RowMapper<ColumnMetadata> {
        @Override
        public ColumnMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ColumnMetadata(
                rs.getString("COLUMN_NAME"),
                rs.getString("TYPE_NAME"),
                rs.getInt("COLUMN_SIZE"),
                rs.getInt("DECIMAL_DIGITS"),
                rs.getString("IS_NULLABLE").equalsIgnoreCase("YES"),
                rs.getString("COLUMN_DEF"),
                isAutoIncrement(rs.getString("IS_AUTOINCREMENT")),
                false, // 需通过额外查询设置
                false, // 需通过额外查询设置
                determineDataTypeCategory(rs.getString("TYPE_NAME"))
            );
        }

        private boolean isAutoIncrement(String value) {
            return "YES".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
        }
    }

    /**
     * 主键元数据映射（单独查询）
     */
    public static class PrimaryKeyRowMapper implements RowMapper<String> {
        @Override
        public String mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getString("COLUMN_NAME");
        }
    }

    /**
     * 数据类型分类推断逻辑
     */
    private static DataTypeCategory determineDataTypeCategory(String typeName) {
        if (typeName == null) return DataTypeCategory.UNKNOWN;
        
        String upperType = typeName.toUpperCase();
        return switch (upperType) {
            case "VARCHAR", "CHAR", "TEXT", "NVARCHAR", "STRING", "BPCHAR" -> DataTypeCategory.STRING;
            case "INT", "INTEGER", "BIGINT", "SMALLINT", "TINYINT", "INT2", "INT4", "INT8", "SERIAL", "BIGSERIAL" -> DataTypeCategory.INTEGER;
            case "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE", "REAL", "MONEY" -> DataTypeCategory.FLOAT;
            case "DATE", "TIME", "DATETIME", "TIMESTAMP", "TIMESTAMPTZ" -> DataTypeCategory.DATE;
            case "BIT", "BOOLEAN", "BOOL" -> DataTypeCategory.BOOLEAN;
            case "BLOB", "BINARY", "VARBINARY", "BYTEA" -> DataTypeCategory.BINARY;
            default -> DataTypeCategory.UNKNOWN;
        };
    }

    /**
     * 是否为数值类型（用于生成范围值）
     */
    public boolean isNumericType() {
        return dataTypeCategory == DataTypeCategory.INTEGER 
            || dataTypeCategory == DataTypeCategory.FLOAT;
    }

    /**
     * 是否需要生成数据（非自增列且非主键）
     */
    public boolean requiresGeneration() {
        return !autoIncrement && !primaryKey;
    }
}