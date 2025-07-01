package team.zhh.de.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import team.zhh.base.model.ColumnMetadata;
import team.zhh.de.service.DatabaseService;
import java.util.*;

@Component("randomDataEngine")
public class RandomDataEngine implements IDataEngine {

    @Autowired
    private DatabaseService databaseService;

    @Override
    public List<String[]> generateAndInsertData(String tableName, int rowCount, List<ColumnMetadata> columns) {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            StringBuffer stringBuffer = new StringBuffer();
            for (ColumnMetadata col : columns) {
                stringBuffer.append(",").append(generateRandomValue(col));
            }
            rows.add(stringBuffer.toString().replaceFirst(",","").split(","));
        }
        return rows;
    }

    private Object generateRandomValue(ColumnMetadata col) {
        Random random = new Random();
        switch (col.dataTypeCategory()) {
            case INTEGER: return random.nextInt(10000);
            case FLOAT: return random.nextDouble() * 1000;
            case DATE: return new java.sql.Date(System.currentTimeMillis());
            case BOOLEAN: return random.nextBoolean();
            default: // STRING
                int len = col.size() > 0 && col.size() < 1000 ? col.size() : 20;
                return randomString(len);
        }
    }

    private String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
