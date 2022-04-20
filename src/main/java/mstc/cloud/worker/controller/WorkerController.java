package mstc.cloud.worker.controller;

import mstc.cloud.worker.domain.Request;
import mstc.cloud.worker.service.WorkerRequestProcessor;
import mstc.cloud.worker.service.WorkerRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping("/mstc")
public class WorkerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerController.class);
    @Inject
    private WorkerRequestProcessor workerRequestProcessor;

    @RequestMapping(value = "/worker",
            method = RequestMethod.GET,
            produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get(/*@RequestBody String request*/) {
        return new ResponseEntity<>("hello", HttpStatus.OK);
    }

    @RequestMapping(value = "/worker/execute",
            method = RequestMethod.POST,
            produces= MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exec(@RequestBody Request request) {
        if (request == null) {
            LOGGER.warn("Bad request: NULL");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .header(HttpHeaders.WARNING, "Wrong number of arguments")
                                 .build();
        }
        LOGGER.info("Received request to process" + request);
        try {
            String result =  workerRequestProcessor.processRequest(request);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.warn("Error running : " + request.getJobName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .header(HttpHeaders.WARNING,
                                         e.getClass().getName() +", " + e.getMessage())
                                 .build();
        }
    }
}


