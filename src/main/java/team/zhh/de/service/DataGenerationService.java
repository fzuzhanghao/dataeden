package team.zhh.de.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("randomDataEngine")
    private IDataEngine randomDataEngine;

    @Autowired
    @Qualifier("aiDataEngine")
    private IDataEngine aiDataEngine;


    @Autowired
    private TempDatasourcePool tempDatasourcePool;

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
        IDataEngine dataEngine;
        if ("AI".equalsIgnoreCase(engineType)) {
            dataEngine = aiDataEngine;
        } else {
            dataEngine = randomDataEngine;
        }

        // 获取数据源并创建JdbcTemplate
        DataSource dataSource = tempDatasourcePool.getDataSource(url, username, password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 准备SQL
        String sql = prepareInsertSql(tableName, columns);
        List<String[]> dataList=dataEngine.generateAndInsertData(tableName,rows,columns);
        // 生成并插入数据
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                int paramIndex = 1,valueIndex = 0;
                String[] values = dataList.get(i);
                for (ColumnMetadata col : columns) {
                    if (col.requiresGeneration()) {
                        ps.setObject(paramIndex++, values[valueIndex++]);
                    }
                }
            }

            @Override
            public int getBatchSize() {
                return rows;
            }
        });
    }

    private String prepareInsertSql(String tableName, List<ColumnMetadata> columns) {
        String columnNames = columns.stream()
            .map(ColumnMetadata::name)
            .collect(Collectors.joining(", "));
        String placeholders = columns.stream()
            .map(c -> "?")
            .collect(Collectors.joining(", "));
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnNames, placeholders);
    }
}