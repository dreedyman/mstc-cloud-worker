package mstc.cloud.worker.domain;

import org.springframework.stereotype.Component;

/**
 * @author dreedy
 */
@Component
public class RequestValidator {
    public boolean isValidRequest(Request request) {
        if (request == null) {
            return false;
        }
        return !(request.getImage() == null ||
                request.getJobName() == null ||
                request.getInputBucket() == null);
    }
}
