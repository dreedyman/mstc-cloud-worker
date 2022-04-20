# MSTC Cloud Worker
This project provides a Worker that consumes work requests from a queue, submits Kubernetes jobs and writes responses back. The worker is written in Java, a test client has been provided with Python in `src/test/python`. This is an embedded project that uses `poetry`.

The `cloud-worker` is a Spring Boot service, and also uses the [Fabric8's Java Kubernetes Client](https://github.com/fabric8io/kubernetes-client).

## Overview
![](assets/arch.png)

**Client perform the following:**

1. Uploads input data to a MinIO `input bucket`
2. Submits request to RabbitMQ, providing the `image` name, `job name`, `input-bucket`, optional `output-bucket` (if not proivided the `input bucket` will be used to send output(s)).

**Cloud-Worker then:**

1. Consumes work
2. Unwraps request, obtains the `image` to run, a name for the `job`, an optional `timeout`, the `input bucket URL` and optional `output bucket URL`. 
3. These values are then configured into a Kubernetes Job. The `input bucket URL` and optional `output bucket URL` are set as values for environment variables `INPUT_BUCKET_URL` and `OUTPUT_BUCKET_URL` respectively.
4. The Job is submitted and observed. Once complete the output of the Job is provided.

**K8S Job**

1. Is expected to download all files in the `input bucket`
2. Do it's thing, and
3. Upload all files to the `output bucket`. Once the client receives notification, it can then go pick up all files. NOTE: If there are errors that the K8s Job runs into, they will be put into files and copied to the `output bucket` as well.


## Setting up Kubernetes
In order for the `mstc-cloud-worker` to create, monitor and work with Kubernetes jobs, it needs to have a service account. A service account provides an identity for a process that runs in a pod.

The `config/service-account.yaml` file provides the setup to do this. In order to enable this run:

`kubectl apply -f config/service-account.yaml`

This will also setup the `mstc-dev` namespace (if not already created). If you want to switch to the `mstc-dev` namespace as the default, run:

switch to use that namespace by default:
`$ kubectl config set-context --current --namespace=mstc-dev`

TODO: There is still work to be dont to setup a secret to hold your username and password.

## Deploying Services to Kubernetes
We'll need to deploy both RabbitMQ and MinIO to Kubernetes, as well as the mstc-cloud-worker. So far charts have been setup for `mstc-cloud-worker` and `rabbitmq` in the charts directory.

There are tasks in the build.gradle file:

* `helmInstall`
* `helmUninstall`

If you want to install/unistall separately run the followinf:

`helm (un)install mstc-work-queue charts/rabbitmq`
`helm (un)install mstc-cloud-worker charts/mstc-cloud-worker`

Once everything is deployed, you can get a view of whats running by looksing at

`kubectl get all -n mstc-dev`

```
NAME                                     READY   STATUS    RESTARTS   AGE
pod/mstc-cloud-worker-6ff966fdf9-959s2   1/1     Running   0          54m
pod/mstc-work-queue-744986cb5c-nr47l     1/1     Running   0          54m

NAME                        TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
service/mstc-cloud-worker   NodePort   10.111.33.122   <none>        8080:31008/TCP   54m
service/mstc-work-queue     NodePort   10.97.101.165   <none>        5672:30435/TCP   54m

NAME                                READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/mstc-cloud-worker   1/1     1            1           54m
deployment.apps/mstc-work-queue     1/1     1            1           54m

NAME                                           DESIRED   CURRENT   READY   AGE
replicaset.apps/mstc-cloud-worker-6ff966fdf9   1         1         1       54m
replicaset.apps/mstc-work-queue-744986cb5c     1         1         1       54m
```

If you've got to here, you'll want to run something. You'll first need to `port-forward` for RabbitMQ:

```
kubectl -n mstc-dev port-forward mstc-work-queue-744986cb5c-nr47l 5672:5672                                                         
Forwarding from 127.0.0.1:5672 -> 5672
Forwarding from [::1]:5672 -> 5672
```

This maps `localhost` port `5672` to the pods port `5672`. If you want to access RabbitMQ's admin page, you'll also need to do the same for port `15672`. Note that you'll need different terminals for the `port-forward` command, or you can run it in the background `&`.

## Running the Python Client
The Python client is a test case. Just run `pytest`. However, you first need to build the test Docker image. To do that:

1. cd to the `src/test/python/mstc-cloud-worker` directory
2. Run `poetry shell`
3. Run `make dist`. This will create a Docker image containing the `mstc_cloud_worker/main.py` file. That files grabs environment variables, logs some messages, sleeps for 5 seconds and returns.

Before running `pytest` it is helpful to get a new terminal and follow the logs of the `mstc-cloud-worker`:

`kubectl logs -n mstc-dev mstc-cloud-worker-6ff966fdf9-b27ml --follow`

You'll then be following the log, you'll see something like this:

```
__  __ ____ _____ ____    ____ _                 _  __        __         _
 |  \/  / ___|_   _/ ___|  / ___| | ___  _   _  __| | \ \      / /__  _ __| | _____ _ __
 | |\/| \___ \ | || |     | |   | |/ _ \| | | |/ _` |  \ \ /\ / / _ \| '__| |/ / _ \ '__|
 | |  | |___) || || |___  | |___| | (_) | |_| | (_| |   \ V  V / (_) | |  |   <  __/ |
 |_|  |_|____/ |_| \____|  \____|_|\___/ \__,_|\__,_|    \_/\_/ \___/|_|  |_|\_\___|_|

2022-04-19 21:30:07.672  INFO 1 --- [           main] mstc.cloud.worker.WorkerApplication      : Starting WorkerApplication using Java 11.0.14 on mstc-cloud-worker-5f784bcf88-889z6 with PID 1 (/app.jar started by root in /)
2022-04-19 21:30:07.679 DEBUG 1 --- [           main] mstc.cloud.worker.WorkerApplication      : Running with Spring Boot v2.6.4, Spring v5.3.16
2022-04-19 21:30:07.681  INFO 1 --- [           main] mstc.cloud.worker.WorkerApplication      : The following 1 profile is active: "local"
2022-04-19 21:30:10.593  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2022-04-19 21:30:10.594  INFO 1 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.58]
2022-04-19 21:30:10.729  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2022-04-19 21:30:12.395  INFO 1 --- [           main] mstc.cloud.worker.WorkerApplication      : Started WorkerApplication in 5.964 seconds (JVM running for 7.381)
2022-04-19 21:30:34.973  INFO 1 --- [nio-8080-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2022-04-19 21:30:35.234  INFO 1 --- [nio-8080-exec-1] m.c.worker.controller.WorkerController   : Received request to processRequest(image=mstc/python-test, jobName=test-job, timeOut=0, inputBucketUrl=http://localhost:9000/test-bucket, outputBucketUrl=null)
2022-04-19 21:30:35.245  INFO 1 --- [nio-8080-exec-1] m.c.w.service.WorkerRequestProcessor     : Submitting job: test-job
2022-04-19 21:30:36.154 DEBUG 1 --- [nio-8080-exec-1] mstc.cloud.worker.job.K8sJobRunner       : Creating job "test-job-99c0812e-03c5-459c-95db-dd95251b14d0" in namespace mstc-dev.
{
  "apiVersion" : "batch/v1",
  "kind" : "Job",
  "metadata" : {
    "labels" : {
      "foo" : "bar"
    },
    "name" : "test-job-f0f43e62-3f40-422c-a199-346390dcc37c"
  },
  "spec" : {
    "template" : {
      "spec" : {
        "containers" : [ {
          "env" : [ {
            "name" : "INPUT_BUCKET_URL",
            "value" : "http://localhost:9000/test-bucket"
          } ],
          "image" : "mstc/python-test",
          "imagePullPolicy" : "IfNotPresent",
          "name" : "test-job"
        } ],
        "restartPolicy" : "Never"
      }
    },
    "ttlSecondsAfterFinished" : 30
  }
}
2022-04-19 21:30:36.995  INFO 1 --- [nio-8080-exec-1] mstc.cloud.worker.job.K8sJobRunner       : Job "test-job-99c0812e-03c5-459c-95db-dd95251b14d0" is created in namespace mstc-dev, timeout of 15 minutes, waiting for result...
2022-04-19 21:30:46.531  INFO 1 --- [nio-8080-exec-1] mstc.cloud.worker.job.K8sJobRunner       : Job test-job-d307cd41-b017-48f0-8e53-19302717f6dd duration: 10375 ms
```
Then run `pytest`.

The client submits the folloing data (as JSON):

```json
{"image": "mstc/python-test:latest",
 "jobName": "test-job",
 "inputBucketUrl" : "http://hostname:9000/bucket",
} 
```

Optionally you can submit:

```json
{"image": "mstc/python-test:latest",
 "timeOut": 5,
 "jobName": "test-job",
 "inputBucketUrl" : "http://hostname:9000/bucket",
 "outputBucketUrl" : "http://hostname:9000/out-bucket",
} 
```

* If the `timeOut` property is not ptovided, the default is 15 minutes.
* If the `output bucket URL` is not provied, the input bucket is used.



