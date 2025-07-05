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
            List<String> row = new ArrayList<>();
            for (ColumnMetadata col : columns) {
                if (col.requiresGeneration()) {
                    row.add(generateRandomValue(col).toString());
                }
            }
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }

    private Object generateRandomValue(ColumnMetadata col) {
        Random random = new Random();
        switch (col.dataTypeCategory()) {
            case INTEGER: 
                return random.nextInt(10000);
            case FLOAT: 
                // 处理NUMERIC类型的精度
                if ("NUMERIC".equalsIgnoreCase(col.typeName()) || "DECIMAL".equalsIgnoreCase(col.typeName())) {
                    return generateNumericValue(random, col.size(), col.decimalDigits());
                }
                return random.nextDouble() * 1000;
            case DATE: 
                java.sql.Date date = new java.sql.Date(System.currentTimeMillis() + random.nextInt(365 * 24 * 60 * 60 * 1000));
                return date.toString(); // 返回YYYY-MM-DD格式的字符串
            case BOOLEAN: 
                return random.nextBoolean();
            default: // STRING
                int len = col.size() > 0 && col.size() < 1000 ? col.size() : 20;
                return randomString(len);
        }
    }
    
    private String generateNumericValue(Random random, int precision, int scale) {
        // 生成符合精度要求的数值
        int maxIntegerDigits = precision - scale;
        long maxInteger = (long) Math.pow(10, maxIntegerDigits) - 1;
        long integerPart = random.nextLong() % maxInteger;
        if (integerPart < 0) integerPart = -integerPart;
        
        if (scale == 0) {
            return String.valueOf(integerPart);
        }
        
        // 生成小数部分
        long maxDecimal = (long) Math.pow(10, scale);
        long decimalPart = random.nextLong() % maxDecimal;
        if (decimalPart < 0) decimalPart = -decimalPart;
        
        String decimalStr = String.format("%0" + scale + "d", decimalPart);
        return integerPart + "." + decimalStr;
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
