package mstc.cloud.worker.job;

/**
 * @author dreedy
 */
public class JobException extends Exception {
    public JobException(String message) {
        super(message);
    }

    public JobException(String message,
                        Throwable cause) {
        super(message, cause);
    }
}
