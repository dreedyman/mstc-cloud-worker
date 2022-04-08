package mstc.cloud.worker.job;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

import java.util.concurrent.CountDownLatch;

import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dreedy
 */
public class K8sJobWatcher implements Watcher<Pod> {
    private static final Logger logger = LoggerFactory.getLogger(K8sJobWatcher.class);
    private static final String FAILED_GETTING_LOG = "Failed getting log for: {}";
    private final Job job;
    private final CountDownLatch watchLatch;
    private final String namespace;
    private final KubernetesClient client;
    private final JobMetrics jobMetrics;
    private final String jobName;
    private boolean succeeded = false;
    private boolean closing = false;
    private String output;

    /**
     * Create an K8sJobWatcher.
     *
     * @param job        The job to watch.
     * @param watchLatch A CountDownLatch to countDown when success event happens.
     * @param namespace  The name space to use.
     * @param client     The Kubernetes client to use.
     * @param jobMetrics Used to track job metrics
     * @param jobName    The name of the submitted job.
     */
    public K8sJobWatcher(Job job,
                         CountDownLatch watchLatch,
                         String namespace,
                         KubernetesClient client,
                         JobMetrics jobMetrics,
                         String jobName) {
        this.job = job;
        this.watchLatch = watchLatch;
        this.namespace = namespace;
        this.client = client;
        this.jobMetrics = jobMetrics;
        this.jobName = jobName;
    }

    public String getOutput() {
        return output;
    }

    /**
     * get whether the job has been successful.
     *
     * @return Whether the job succeeded.
     */
    public boolean getSucceeded() {
        return succeeded;
    }

    @Override
    public void eventReceived(Watcher.Action action, Pod pod) {
        if (closing) {
            return;
        }
        if (pod.getStatus()
               .getPhase()
               .equals("Succeeded")) {
            try {
                logger.info("Job {} Succeeded!", jobName);
                String output = getLog(pod);
                jobMetrics.setJobTiming(output);
                if (logger.isDebugEnabled() && output != null) {
                    logger.debug("Job output:\n{}", output);
                }
            } catch (KubernetesClientException e) {
                logger.warn(FAILED_GETTING_LOG, jobName, e);
            }
            succeeded = true;
            watchLatch.countDown();
        } else if (pod.getStatus()
                      .getPhase()
                      .equals("Failed")) {
            try {
                logger.warn("Job {} Failed!", jobName);
                String output = getLog(pod);
                jobMetrics.setJobTiming(output);
                if (output != null) {
                    logger.warn("Job output:\n{}", output);
                }
                watchLatch.countDown();
            } catch (KubernetesClientException e) {
                logger.warn(FAILED_GETTING_LOG, jobName, e);
            }
        } else {
            logger.info("Job {} {}!", jobName, pod.getStatus().getPhase());
        }
    }

    @Override
    public void onClose(WatcherException e) {
        closing = true;

        if (e != null) {
            logger.info("Deleting job {}, namespace: {}.", jobName, namespace, e);
        } else {
            logger.info("Deleting job {}, namespace: {}.", jobName, namespace);
        }
        client.batch()
              .v1()
              .jobs()
              .inNamespace(namespace)
              .delete(job);
        client.pods()
              .inNamespace(namespace)
              .withLabel("job-name",
                         jobName)
              .withGracePeriod(5L)
              .delete();
    }

    private String getLog(Pod pod) {
        try {
            return client.pods()
                         .inNamespace(namespace)
                         .withName(pod.getMetadata()
                                      .getName())
                         .inContainer(containerName())
                         .getLog();
        } catch (KubernetesClientException e) {
            logger.warn(FAILED_GETTING_LOG, jobName, e);
        }
        return null;
    }

    private String containerName() {
        return job.getSpec()
                  .getTemplate()
                  .getSpec()
                  .getContainers()
                  .get(0)
                  .getName();
    }
}
