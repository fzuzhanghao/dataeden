package team.zhh.de.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import team.zhh.base.model.ColumnMetadata;
import team.zhh.de.exception.DeepSeekException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("aiDataEngine")
public class AIDataEngine implements IDataEngine {
    private static final Logger logger = LoggerFactory.getLogger(AIDataEngine.class);
    
    // 配置参数
    @Value("${deepsk.api_url}")
    private String DEEPSEEK_API_URL;
    @Value("${deepsk.mykey}")
    private String API_KEY;
    @Value("${deepsk.max_tokens}")
    private int maxTokens;
    @Value("${deepsk.temperature}")
    private double temperature;
    @Value("${deepsk.model}")
    private String model;
    private static final int BATCH_SIZE = 1000;

    @Override
    public List<String[]> generateAndInsertData(String tableName, int rowCount, List<ColumnMetadata> columns) throws Exception {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        List<String[]> dataList = new ArrayList<>();
        
        try {
            // 过滤出需要生成的列
            List<ColumnMetadata> columnsToGenerate = columns.stream()
                .filter(ColumnMetadata::requiresGeneration)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            if (columnsToGenerate.isEmpty()) {
                return dataList;
            }
            
            // 构建一次性生成所有数据的提示词
            String prompt = buildBulkDataPrompt(tableName, columnsToGenerate, rowCount);
            logger.debug("prompt: {}", prompt);
            logger.info("AI Engine - 开始生成 {} 行数据，共 {} 列", rowCount, columnsToGenerate.size());
            logger.info("AI Engine - 表名: {}", tableName);
            
            // 调用API生成所有数据
            String csvData = generateBulkDataViaAPI(prompt, httpClient);
            
            logger.info("AI Engine - 接收到CSV数据，长度: {} 字符", csvData.length());
            logger.debug("csvData: {}", csvData);
            
            // 解析CSV数据
            dataList = parseCSVData(csvData, rowCount, columnsToGenerate.size());
            
        } finally {
            httpClient.close();
        }
        
        return dataList;
    }

    // 调用DeepSeek API生成批量数据
    private String generateBulkDataViaAPI(String prompt, 
                                               CloseableHttpClient httpClient) throws Exception {
        HttpPost request = new HttpPost(DEEPSEEK_API_URL);
        request.setHeader("Authorization", "Bearer " + API_KEY);
        request.setHeader("Content-Type", "application/json");

        Map<String, Object> body = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        body.put("messages", Arrays.asList(message));
        body.put("max_tokens", maxTokens); // 增加token数量以支持更多数据
        body.put("temperature", temperature);
        body.put("stream", false);
        body.put("model", model);

        request.setEntity(new StringEntity(JSONObject.toJSONString(body)));
        CloseableHttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        
        // 检查HTTP状态码
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            handleHttpError(statusCode, responseBody);
        }
        
        JSONObject jsonObject = JSONObject.parseObject(responseBody);
        
        // 检查API响应中的错误
        if (jsonObject.containsKey("error")) {
            JSONObject error = jsonObject.getJSONObject("error");
            String errorType = error.getString("type");
            String errorMessage = error.getString("message");
            handleApiError(errorType, errorMessage);
        }
        
        // 提取生成的文本内容
        if (jsonObject.containsKey("choices")) {
            JSONObject firstChoice = jsonObject.getJSONArray("choices").getJSONObject(0);
            JSONObject messageObj = firstChoice.getJSONObject("message");
            return messageObj.getString("content").trim();
        } else {
            throw new DeepSeekException("Invalid API response format: missing choices");
        }
    }

    // 构建批量数据生成提示词
    private static String buildBulkDataPrompt(String tableName, List<ColumnMetadata> columns, int rowCount) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为下表生成 ").append(rowCount).append(" 行真实数据，禁止添加其他列\n\n");
        prompt.append("表结构：\n");
        
        for (ColumnMetadata col : columns) {
            prompt.append("- ").append(col.name()).append(": ").append(col.typeName());
            if (col.size() > 0) {
                prompt.append("(").append(col.size());
                if (col.decimalDigits() > 0) {
                    prompt.append(",").append(col.decimalDigits());
                }
                prompt.append(")");
            }
            prompt.append(" - ").append(inferDataPurpose(col)).append("\n");
        }
        
        prompt.append("\n严格要求：\n");
        prompt.append("1. 生成恰好 ").append(rowCount).append(" 行数据\n");
        prompt.append("2. 以CSV格式返回数据，使用逗号作为分隔符\n");
        prompt.append("3. 不要包含列标题，不需要多余内容，直接输出数据\n");
        prompt.append("4. 如果值包含逗号，请用双引号包围\n");
        prompt.append("5. 认真核对表结构，表总共有").append(columns.size()).append("列，禁止添加其他列。此外还需要确保数据类型与列规格匹配\n");
        prompt.append("6. 日期使用 YYYY-MM-DD 格式、布尔值使用 true/false、数值类型请遵守精度和小数位数\n");
        
        return prompt.toString();
    }

    // 解析CSV数据
    private static List<String[]> parseCSVData(String csvData, int expectedRows, int expectedColumns) {
        // 处理以```包裹的情况
        String trimmed = csvData.trim();
        if (trimmed.startsWith("```") ) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.lastIndexOf("```", trimmed.length() - 1);
            if (end > start) {
                trimmed = trimmed.substring(start, end).trim();
            } else {
                trimmed = trimmed.substring(start).trim();
            }
        }
        List<String[]> dataList = new ArrayList<>();
        String[] lines = trimmed.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // 简单的CSV解析（处理逗号分隔，支持双引号包围的值）
            List<String> values = new ArrayList<>();
            StringBuilder currentValue = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    values.add(currentValue.toString().trim());
                    currentValue = new StringBuilder();
                } else {
                    currentValue.append(c);
                }
            }
            // 添加最后一个值
            values.add(currentValue.toString().trim());
            // 严格保障列数，多余的列丢弃，少于则警告并跳过
            if (values.size() > expectedColumns) {
                logger.warn("CSV解析警告: 行有 {} 列，期望 {} 列，多余部分已丢弃", values.size(), expectedColumns);
                values = values.subList(0, expectedColumns);
                dataList.add(values.toArray(new String[0]));
            } else if (values.size() == expectedColumns) {
                dataList.add(values.toArray(new String[0]));
            } else {
                logger.warn("CSV解析警告: 行有 {} 列，期望 {} 列，已跳过该行", values.size(), expectedColumns);
            }
        }
        // 确保行数匹配
        if (dataList.size() != expectedRows) {
            logger.warn("CSV解析警告: 生成了 {} 行，期望 {} 行", dataList.size(), expectedRows);
        }
        return dataList;
    }

    // 处理HTTP错误
    private static void handleHttpError(int statusCode, String responseBody) throws DeepSeekException {
        String errorMessage;
        switch (statusCode) {
            case 400:
                errorMessage = "格式错误 - 请求体格式错误，请根据错误信息提示修改请求体";
                break;
            case 401:
                errorMessage = "认证失败 - API key错误，请检查您的API key是否正确";
                break;
            case 402:
                errorMessage = "余额不足 - 账号余额不足，请前往充值页面进行充值";
                break;
            case 422:
                errorMessage = "参数错误 - 请求体参数错误，请根据错误信息提示修改相关参数";
                break;
            case 429:
                errorMessage = "请求速率达到上限 - 请合理规划您的请求速率";
                break;
            case 500:
                errorMessage = "服务器故障 - 服务器内部故障，请等待后重试";
                break;
            case 503:
                errorMessage = "服务器繁忙 - 服务器负载过高，请稍后重试";
                break;
            default:
                errorMessage = "未知错误 - HTTP状态码: " + statusCode + ", 响应: " + responseBody;
                break;
        }
        throw new DeepSeekException(errorMessage);
    }

    // 处理API错误
    private static void handleApiError(String errorType, String errorMessage) throws DeepSeekException {
        String detailedMessage = "API错误 - 类型: " + errorType + ", 消息: " + errorMessage;
        throw new DeepSeekException(detailedMessage);
    }

    // 字段类型推断逻辑（扩展版）
    private static String inferDataPurpose(ColumnMetadata column) {
        String colName = column.name().toLowerCase();
        String typeName = column.typeName().toLowerCase();
        
        // 基于列名的推断
        if (colName.contains("email")) return "邮箱地址";
        if (colName.contains("phone") || colName.contains("tel")) return "电话号码";
        if (colName.contains("name")) return "姓名";
        if (colName.contains("address")) return "地址";
        if (colName.contains("city")) return "城市名";
        if (colName.contains("country")) return "国家名";
        if (colName.contains("code") || colName.contains("id")) return "唯一标识符";
        if (colName.contains("date") || colName.contains("time")) return "日期时间";
        if (colName.contains("price") || colName.contains("cost") || colName.contains("amount")) return "金额";
        if (colName.contains("age")) return "年龄数字";
        if (colName.contains("score") || colName.contains("rating")) return "评分";
        if (colName.contains("description") || colName.contains("comment")) return "文本描述";
        if (colName.contains("url") || colName.contains("link")) return "网址";
        if (colName.contains("status")) return "状态标识";
        if (colName.contains("active") || colName.contains("enabled")) return "布尔标志";
        
        // 基于数据类型的推断
        switch (column.dataTypeCategory()) {
            case INTEGER:
                return "整数";
            case FLOAT:
                if ("NUMERIC".equalsIgnoreCase(typeName) || "DECIMAL".equalsIgnoreCase(typeName)) {
                    return "小数，精度 " + column.size() + "," + column.decimalDigits();
                }
                return "小数";
            case DATE:
                return "日期，格式 YYYY-MM-DD";
            case BOOLEAN:
                return "布尔值 (true/false)";
            case STRING:
                if (column.size() > 0 && column.size() < 50) {
                    return "短文本";
                } else if (column.size() >= 50) {
                    return "长文本";
                }
                return "文本";
            default:
                return "真实值";
        }
    }
}