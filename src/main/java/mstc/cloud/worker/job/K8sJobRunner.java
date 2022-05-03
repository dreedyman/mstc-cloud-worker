package mstc.cloud.worker.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author dreedy
 */
public class K8sJobRunner {
    private KubernetesClient client;
    private static final Logger logger = LoggerFactory.getLogger(K8sJobRunner.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public void setClient(KubernetesClient client) {
        this.client = client;
    }

    public String submit(K8sJob k8sJob) throws JobException {
        String namespace = k8sJob.getNamespace();
        JobMetrics jobMetrics = new JobMetrics();
        String output;
        if (client == null) {
            Config config = new ConfigBuilder().build();
            client = new DefaultKubernetesClient(config);
        }
        try {
            Job job = k8sJob.getJob();
            String jobName = k8sJob.getJobNameUnique();
            logJobCreation(jobName, namespace, job);
            jobMetrics.setSubmitted(Instant.now());
            Job createdJob = client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);
            logger.info(String.format("Job \"%s\" is created in namespace %s, timeout of %s minutes, waiting for result...",
                                      jobName,
                                      namespace,
                                      k8sJob.getTimeOut()));
            output = watch(client, createdJob, k8sJob);

        } finally {
            client.close();
            jobMetrics.setReturned(Instant.now());
            logger.info(String.format("Job %s duration: %d ms", k8sJob.getJobNameUnique(), jobMetrics.getJobDuration()));
        }
        return output;
    }

    private String watch(KubernetesClient client, Job job, K8sJob k8sJob) throws JobException {
        String namespace = k8sJob.getNamespace();
        logger.info("Watching job: " + k8sJob.getJobNameUnique());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        client.pods()
              .inNamespace(namespace)
              .withLabel("job-name", k8sJob.getJobNameUnique())
              .watch(new PodWatcher(countDownLatch));
        boolean returned;
        try {
            returned = countDownLatch.await(k8sJob.getTimeOut(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new JobException("Job interrupted", e);
        }
        if (!returned) {
            throw new JobException("Job timed out");
        }
        return client.batch().v1().jobs().inNamespace(namespace).withName(job.getMetadata().getName()).getLog();
    }

    private void logJobCreation(String jobName, String namespace, Job job) {
        if (logger.isDebugEnabled()) {
            try {
                logger.debug(String.format("Creating job \"%s\" in namespace %s.%n%s",
                                           jobName,
                                           namespace,
                                           mapper.writerWithDefaultPrettyPrinter().writeValueAsString(job)));
            } catch (JsonProcessingException e) {
                logger.warn(
                        String.format("Caught [JsonProcessingException: %s] " +
                                              "while formatting job \"%s\", continue job submission",
                                      e.getMessage(),
                                      jobName));
            }
        } else {
            logger.info(String.format("Creating job \"%s\" in namespace %s.", jobName, namespace));
        }
    }

    private static class PodWatcher implements Watcher<Pod> {
        CountDownLatch countDownLatch;

        public PodWatcher(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public boolean reconnecting() {
            return Watcher.super.reconnecting();
        }

        @Override
        public void eventReceived(Action action, Pod resource) {
            switch(resource.getStatus().getPhase()) {
                case "Succeeded":
                    logger.info("Job Succeeded");
                    countDownLatch.countDown();
                    break;
                case "Failed":
                    logger.info("Job Failed");
                    countDownLatch.countDown();
                    break;
                default:
                    logger.info("Job phase: " + resource.getStatus().getPhase());
            }

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
}
