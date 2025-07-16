package team.zhh.de.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import team.zhh.base.model.ColumnMetadata;
import team.zhh.de.core.IDataEngine;
import team.zhh.de.core.TempDatasourcePool;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DataGenerationService {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ApplicationContext applicationContext;


    @Autowired
    private TempDatasourcePool tempDatasourcePool;

    private static final Logger logger = LoggerFactory.getLogger(DataGenerationService.class);

    /**
     * 生成数据并插入表
     * @param url 数据库连接url
     * @param username 用户名
     * @param password 密码
     * @param tableName 表名
     * @param rows 行数
     * @param engineType 引擎类型（"AI" 或 "RANDOM"）
     */
    public void generateData(String url, String username, String password, String tableName, int rows, String engineType) throws Exception {
        List<ColumnMetadata> columns = databaseService.getTableColumns(url, username, password, tableName);

        // 选择数据生成引擎
        IDataEngine dataEngine = (IDataEngine) applicationContext.getBean(engineType);

        // 获取数据源并创建JdbcTemplate
        DataSource dataSource = tempDatasourcePool.getDataSource(url, username, password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        // 准备SQL
        String sql = prepareInsertSql(tableName, columns, url);
        logger.debug("Generated SQL = {}", sql);
        logger.debug("Columns requiring generation = {}", columns.stream().filter(ColumnMetadata::requiresGeneration).map(ColumnMetadata::name).collect(Collectors.joining(", ")));
        
        List<String[]> dataList = dataEngine.generateAndInsertData(tableName, rows, columns);
        logger.debug("Generated {} rows of data", dataList.size());
        
        // 打印前几行数据用于调试
        if (!dataList.isEmpty()) {
            logger.debug("First row data = {}", String.join(", ", dataList.get(0)));
        }else{
            throw new Exception("无数据生成");
        }
        
        // 生成并插入数据
        List<ColumnMetadata> columnsToGenerate = columns.stream()
            .filter(ColumnMetadata::requiresGeneration)
            .collect(Collectors.toList());
            
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String[] values = dataList.get(i);
                for (int j = 0; j < values.length; j++) {
                    setValueWithTypeConversion(ps, j + 1, values[j], columnsToGenerate.get(j));
                }
            }

            @Override
            public int getBatchSize() {
                return rows;
            }
        });
    }

    private String prepareInsertSql(String tableName, List<ColumnMetadata> columns, String url) {
        List<ColumnMetadata> columnsToGenerate = columns.stream()
            .filter(ColumnMetadata::requiresGeneration)
            .collect(Collectors.toList());
        
        if (columnsToGenerate.isEmpty()) {
            throw new IllegalStateException("No columns require generation for table: " + tableName);
        }
        
        String dbType = detectDbTypeFromUrl(url);
        String quoteChar = getQuoteChar(dbType);
        
        String columnNames = columnsToGenerate.stream()
            .map(col -> quoteChar + col.name() + quoteChar)
            .collect(Collectors.joining(", "));
        String placeholders = columnsToGenerate.stream()
            .map(c -> "?")
            .collect(Collectors.joining(", "));
        
        return String.format("INSERT INTO %s%s%s (%s) VALUES (%s)", 
            quoteChar, tableName, quoteChar, columnNames, placeholders);
    }
    
    private String detectDbTypeFromUrl(String url) {
        if (url.contains("mysql")) return "mysql";
        if (url.contains("postgresql")) return "postgresql";
        if (url.contains("oracle")) return "oracle";
        if (url.contains("sqlserver")) return "sqlserver";
        return "mysql"; // 默认
    }
    
    private String getQuoteChar(String dbType) {
        switch (dbType.toLowerCase()) {
            case "postgresql":
            case "oracle":
                return "\"";
            case "sqlserver":
                return "[";
            default: // mysql
                return "`";
        }
    }
    

    
    private void setValueWithTypeConversion(PreparedStatement ps, int index, String value, ColumnMetadata column) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            ps.setNull(index, java.sql.Types.NULL);
            return;
        }
        
        try {
            switch (column.dataTypeCategory()) {
                case INTEGER:
                    ps.setInt(index, Integer.parseInt(value));
                    break;
                case FLOAT:
                    // 特殊处理NUMERIC/DECIMAL类型
                    if ("NUMERIC".equalsIgnoreCase(column.typeName()) || "DECIMAL".equalsIgnoreCase(column.typeName())) {
                        ps.setBigDecimal(index, new java.math.BigDecimal(value));
                    } else {
                        ps.setDouble(index, Double.parseDouble(value));
                    }
                    break;
                case DATE:
                    ps.setDate(index, java.sql.Date.valueOf(value));
                    break;
                case BOOLEAN:
                    ps.setBoolean(index, Boolean.parseBoolean(value));
                    break;
                default: // STRING, BINARY, UNKNOWN
                    ps.setString(index, value);
                    break;
            }
        } catch (NumberFormatException e) {
            // 如果类型转换失败，尝试作为字符串处理
            logger.warn("Failed to convert value '{}' to {} for column {}. Using as string.", value, column.dataTypeCategory(), column.name());
            ps.setString(index, value);
        }
    }
}