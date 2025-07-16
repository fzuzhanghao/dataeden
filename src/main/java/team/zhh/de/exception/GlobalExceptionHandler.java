package team.zhh.de.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import team.zhh.base.model.ApiResponse;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DeepSeekException.class)
    public ApiResponse<String> handleDeepSeekException(DeepSeekException ex) {
        return ApiResponse.error(5001, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception ex) {
        log.error(ex.getMessage(),ex);
        return ApiResponse.error(5000, "服务器内部错误: " + ex.getMessage());
    }
}
