package team.zhh.de.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class DataSourceShutdownHook {
    @Autowired
    private TempDatasourcePool tempDatasourcePool;

    @PreDestroy
    public void destroy() {
        tempDatasourcePool.closeAll();
    }
} 