package team.zhh.de.controller;

import team.zhh.base.model.ApiResponse;
import team.zhh.de.service.DatabaseService;
import team.zhh.de.service.DataGenerationService;
import team.zhh.base.model.ColumnMetadata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DataGenController {

    @Autowired
    private DatabaseService databaseService;
    
    @Autowired
    private DataGenerationService dataGenerationService;

    @PostMapping("/connect")
    public ApiResponse<Boolean> connect(@RequestBody Map<String, String> credentials) {
        String url = credentials.get("url");
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        boolean success = databaseService.connect(url, username, password);
        return ApiResponse.success(success);
    }

    @PostMapping("/tables/getTables")
    public ApiResponse<List<String>> getTables(@RequestBody Map<String, String> credentials) {
        String url = credentials.get("url");
        String username = credentials.get("username");
        String password = credentials.get("password");

        return ApiResponse.success(databaseService.getTables(url, username, password));
    }

    @PostMapping("/tables/getTableColumns")
    public ApiResponse<List<ColumnMetadata>> getTableColumns(@RequestBody Map<String, String> tableInfos) {

        String url = tableInfos.get("url");
        String username = tableInfos.get("username");
        String password = tableInfos.get("password");
        String tableName = tableInfos.get("tableName");
        
        
        return ApiResponse.success(databaseService.getTableColumns(url, username, password, tableName));
    }

    @PostMapping("/generateData")
    public ApiResponse<String> generateData(
            @RequestBody Map<String, String> tableInfos
    ) throws Exception {
        String url = tableInfos.get("url");
        String username = tableInfos.get("username");
        String password = tableInfos.get("password");
        String tableName = tableInfos.get("tableName");
        int rows = Integer.parseInt(tableInfos.get("rows"));
        String engineType = tableInfos.get("engineType");


        dataGenerationService.generateData(url, username, password, tableName, rows, engineType);
        return ApiResponse.success("数据生成成功！");
    }
}