package team.zhh.de.exception;

public class DeepSeekException extends RuntimeException {
    public DeepSeekException() {
        super();
    }

    public DeepSeekException(String message) {
        super(message);
    }

    public DeepSeekException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeepSeekException(Throwable cause) {
        super(cause);
    }
}
