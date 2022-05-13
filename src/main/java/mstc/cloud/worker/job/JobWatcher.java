package mstc.cloud.worker.job;

import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

/**
 * @author dreedy
 */
public class JobWatcher implements Watcher<Pod> {
    private final CountDownLatch countDownLatch;
    private ContainerStateWaiting podError;
    private final static String[] ERROR_REASONS = {
            "ErrImagePull",
            "ImagePullBackOff",
            "CrashLoopBackOff",
            "RunContainerError"
    };
    private static final Logger logger = LoggerFactory.getLogger(JobWatcher.class);

    public JobWatcher(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void eventReceived(Action action, Pod resource) {
        switch (resource.getStatus().getPhase()) {
            case "Succeeded":
                logger.info("Job Succeeded");
                countDownLatch.countDown();
                break;
            case "Failed":
                logger.info("Job Failed");
                countDownLatch.countDown();
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Job phase: ").append(resource.getStatus().getPhase());
                if (!resource.getStatus().getContainerStatuses().isEmpty()) {
                    ContainerStatus containerStatus = resource.getStatus().getContainerStatuses().get(0);
                    if (containerStatus.getState().getWaiting() != null) {
                        ContainerStateWaiting waiting = containerStatus.getState().getWaiting();
                        if (waiting.getMessage() != null) {
                            stringBuilder.append(", Message: ").append(waiting.getMessage());
                        }
                        if (waiting.getReason() != null) {
                            stringBuilder.append(", Reason: ").append(waiting.getReason());
                            if (Arrays.asList(ERROR_REASONS).contains(waiting.getReason())) {
                                podError = waiting;
                                countDownLatch.countDown();
                            }
                        }
                    }
                }
                logger.info(stringBuilder.toString());
        }

    }

    String getMessage() {
        return podError == null ? null : podError.getMessage();
    }

    String getReason() {
        return podError == null ? null : podError.getReason();
    }

    boolean podError() {
        return podError != null;
    }

    @Override
    public void onClose() {
        logger.info("On Close");
        Watcher.super.onClose();
    }

    @Override
    public void onClose(WatcherException cause) {
        logger.info("On Close, exception? " + (cause == null));
    }
}
