package team.zhh.de.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import team.zhh.base.model.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DeepSeekException.class)
    public ApiResponse<String> handleDeepSeekException(DeepSeekException ex) {
        return ApiResponse.error(5001, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception ex) {
        ex.printStackTrace();
        return ApiResponse.error(5000, "服务器内部错误: " + ex.getMessage());
    }
}
