package mstc.cloud.worker.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * @author dreedy
 */
public class K8sJobRunner {
    private static final Logger logger = LoggerFactory.getLogger(K8sJobRunner.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public String submit(K8sJob k8sJob) {
        Job job = k8sJob.getJob();
        Config config = new ConfigBuilder().build();
        String namespace = k8sJob.getNamespace();
        JobMetrics jobMetrics = new JobMetrics();
        String output;
        try (KubernetesClient client = new DefaultKubernetesClient(config)) {
            String jobName = k8sJob.getJobName();
            logJobCreation(jobName, namespace, job);
            jobMetrics.setSubmitted(Instant.now());
            client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);
            logger.info(String.format("Job \"%s\" is created in namespace %s, timeout of %s minutes, waiting for result...",
                                      jobName,
                                      namespace,
                                      k8sJob.getTimeOut()));
            output = watch(client, job, namespace, k8sJob.getTimeOut());
        } finally {
            jobMetrics.setReturned(Instant.now());
            logger.info(String.format("Job %s duration: %d ms", k8sJob.getJobName(), jobMetrics.getJobDuration()));
        }
        return output;
    }

    private String watch(KubernetesClient client, Job job, String namespace, int timeoutMinutes) {
        PodList podList = client.pods().inNamespace(namespace).withLabel("job-name", job.getMetadata().getName()).list();
        client.pods().inNamespace(namespace).withName(podList.getItems().get(0).getMetadata().getName())
              .waitUntilCondition(pod -> pod.getStatus().getPhase().equals("Succeeded") || pod.getStatus().getPhase().equals("Failed"),
                                  timeoutMinutes, TimeUnit.MINUTES);
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

}
