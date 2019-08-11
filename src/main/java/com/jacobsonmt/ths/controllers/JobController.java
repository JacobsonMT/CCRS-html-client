package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.exceptions.JobNotFoundException;
import com.jacobsonmt.ths.model.Base;
import com.jacobsonmt.ths.model.THSJob;
import com.jacobsonmt.ths.services.CCRSService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Controller
public class JobController {

    @Autowired
    private CCRSService ccrsService;

    @GetMapping("/job/{jobId}")
    public String job( @PathVariable("jobId") String jobId,
                       Model model) throws IOException {

        ResponseEntity<THSJob> entity = ccrsService.getJob( jobId );

        if (entity.getStatusCode().equals( HttpStatus.NOT_FOUND ) || entity.getBody() == null ) {
            throw new JobNotFoundException();
        }

        model.addAttribute("job", entity.getBody());

        return "job";
    }

    @GetMapping("/job/{jobId}/content")
    public String getJobViewContent( @PathVariable("jobId") String jobId,
                                     Model model) {
        ResponseEntity<THSJob> entity = ccrsService.getJob( jobId );

        if (entity.getStatusCode().equals( HttpStatus.NOT_FOUND ) || entity.getBody() == null ) {
            throw new JobNotFoundException();
        }

        model.addAttribute("job", entity.getBody());

        return "job :: #job-view-content";
    }

    @GetMapping(value = "/job/{jobId}/bases", produces = "application/json")
    @ResponseBody
    public List<Base> getJobResultBases( @PathVariable("jobId") String jobId) {
        ResponseEntity<THSJob> entity = ccrsService.getJob( jobId );

        if (entity.getStatusCode().equals( HttpStatus.NOT_FOUND ) || entity.getBody() == null ) {
            throw new JobNotFoundException();
        }

        THSJob job = entity.getBody();

        if ( job.isComplete() && !job.isFailed() ) {
            return job.getResult().getBases();
        }

        return new ArrayList<>();
    }

    @GetMapping("/job/{jobId}/resultCSV")
    public ResponseEntity<String> jobResultCSV( @PathVariable("jobId") String jobId) {
        return ccrsService.downloadJobResultContent( jobId );
    }

    @GetMapping("/job/{jobId}/inputFASTA")
    public ResponseEntity<String> jobInputFASTA( @PathVariable("jobId") String jobId) {
        return ccrsService.downloadJobInputFASTA( jobId );
    }


}
