package team.zhh.de.core;

import java.util.List;

import team.zhh.base.model.ColumnMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.alibaba.fastjson2.JSONObject;


import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionContentPart;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("volcengine")
public class Volcengine implements IDataEngine{
    private static final Logger logger = LoggerFactory.getLogger(Volcengine.class);

    @Value("${aiengine.api_url}")
    private String API_URL;
    @Value("${aiengine.mykey}")
    private String API_KEY;
    @Value("${aiengine.max_tokens}")
    private int maxTokens;
    @Value("${aiengine.temperature}")
    private double temperature;
    @Value("${aiengine.model}")
    private String model;

    @Override
    public List<String[]> generateAndInsertData(String tableName, int rowCount, List<ColumnMetadata> columns)
            throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        List<String[]> dataList = new java.util.ArrayList<>();
        try {
            List<ColumnMetadata> columnsToGenerate = columns.stream()
                .filter(ColumnMetadata::requiresGeneration)
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
            if (columnsToGenerate.isEmpty()) {
                return dataList;
            }
            String prompt = buildBulkDataPrompt(tableName, columnsToGenerate, rowCount);
            logger.debug("prompt: {}", prompt);
            logger.info("Volcengine - 开始生成 {} 行数据，共 {} 列", rowCount, columnsToGenerate.size());
            logger.info("Volcengine - 表名: {}", tableName);
            String csvData = generateBulkDataViaAPI(prompt, httpClient);
            logger.info("Volcengine - 接收到CSV数据，长度: {} 字符", csvData.length());
            logger.debug("csvData: {}", csvData);
            dataList = parseCSVData(csvData, rowCount, columnsToGenerate.size());
        } finally {
            httpClient.close();
        }
        return dataList;
    }

    private String generateBulkDataViaAPI(String prompt, CloseableHttpClient httpClient) throws Exception {
        Dispatcher dispatcher = new Dispatcher();
        ArkService service = ArkService.builder()
                .dispatcher(dispatcher)
                .baseUrl(API_URL)
                .apiKey(API_KEY)
                .build();

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userMessage = ChatMessage.builder()
                .role(ChatMessageRole.USER)
                .content(prompt)
                .build();
        messages.add(userMessage);

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        StringBuilder resultContent = new StringBuilder();
        try {
            service.createChatCompletion(chatCompletionRequest).getChoices().forEach(choice -> 
                resultContent.append(choice.getMessage().getContent()).append("\n")
            );

        } finally {
            service.shutdownExecutor();
        }
        return resultContent.toString().trim();
    }

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
            prompt.append(" - ").append(col.remarks()).append("\n");
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

    private static List<String[]> parseCSVData(String csvData, int expectedRows, int expectedColumns) {
        String trimmed = csvData.trim();

        List<String[]> dataList = new java.util.ArrayList<>();
        String[] lines = trimmed.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            List<String> values = new java.util.ArrayList<>();
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
            values.add(currentValue.toString().trim());
            if (values.size() > expectedColumns) {
                values = values.subList(0, expectedColumns);
                dataList.add(values.toArray(new String[0]));
            } else if (values.size() == expectedColumns) {
                dataList.add(values.toArray(new String[0]));
            }
        }
        return dataList;
    }


}
