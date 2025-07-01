package team.zhh.de.core;

import team.zhh.base.model.ColumnMetadata;

import java.util.List;
import java.util.Map;

public interface IDataEngine {
    /**
     * 生成并插入数据
     * @param tableName 表名
     * @param rowCount 行数
     * @param columns 列元数据
     * @return 实际插入行数
     */
    List<String[]> generateAndInsertData(String tableName, int rowCount, List<ColumnMetadata> columns) throws Exception;
}