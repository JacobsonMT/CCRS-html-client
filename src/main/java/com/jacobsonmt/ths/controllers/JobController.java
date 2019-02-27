package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.model.THSJob;
import com.jacobsonmt.ths.services.JobManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Log4j2
@Controller
public class JobController {

    @Autowired
    private JobManager jobManager;



    @PostMapping("/")
    public String submitJob(@RequestParam("pdbFile") MultipartFile pdbFile,
                                   @RequestParam("fastaFile") MultipartFile fastaFile,
                                   @RequestParam("proteinChainIds") String proteinChainIds,
                                   @RequestParam("label") String label,
                                   @RequestParam(value = "email", required = false, defaultValue = "") String email,
                                   @RequestParam(value = "hidden", required = false, defaultValue = "false") boolean hidden,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) throws IOException {

        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
        if ( ipAddress == null ) {
            ipAddress = request.getRemoteAddr();
        }

        THSJob job = jobManager.createJob( ipAddress,
                label,
                THSJob.inputStreamToString( pdbFile.getInputStream() ),
                THSJob.inputStreamToString( fastaFile.getInputStream() ),
                proteinChainIds,
                email,
                hidden );
        String msg = jobManager.submit( job );

        if (msg.isEmpty()) {
            redirectAttributes.addFlashAttribute( "message",
                    "Job Submitted! View job <a href='job/" + job.getJobId() + "' target='_blank'>here</a>." );
            redirectAttributes.addFlashAttribute( "warning", false );
        } else {
            redirectAttributes.addFlashAttribute( "message", msg );
            redirectAttributes.addFlashAttribute( "warning", true );
        }

        return "redirect:/";
    }

    @GetMapping("/job/{jobId}")
    public String job( @PathVariable("jobId") String jobId,
                       Model model) throws IOException {

        THSJob job = jobManager.getSavedJob( jobId );

        if (job==null) {
            return "/";
        }

        model.addAttribute("job", jobManager.getSavedJob( jobId ).toValueObject( true ) );

        return "job";
    }

    @GetMapping("/job/{jobId}/resultPDB")
    public ResponseEntity<String> jobResultPDB( @PathVariable("jobId") String jobId) {
        THSJob job = jobManager.getSavedJob( jobId );

        // test for not null and complete
        if ( job != null && job.isComplete() && !job.isFailed() ) {
            return ResponseEntity.ok()
                    .contentType( MediaType.parseMediaType("application/octet-stream"))
                    .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getLabel() + "-result.pdb\"")
                    .body(job.getResult().getResultPDB());
        }
        return ResponseEntity.badRequest().body( "" );
    }

    @GetMapping("/job/{jobId}/resultCSV")
    public ResponseEntity<String> jobResultCSV( @PathVariable("jobId") String jobId) {
        THSJob job = jobManager.getSavedJob( jobId );

        // test for not null and complete
        if ( job != null && job.isComplete() && !job.isFailed() ) {
            return ResponseEntity.ok()
                    .contentType( MediaType.parseMediaType("application/octet-stream"))
                    .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getLabel() + "-result.csv\"")
                    .body(job.getResult().getResultCSV());
        }
        return ResponseEntity.badRequest().body( "" );
    }

    @GetMapping("/job/{jobId}/inputPDB")
    public ResponseEntity<String> jobInputPDB( @PathVariable("jobId") String jobId) {
        THSJob job = jobManager.getSavedJob( jobId );

        // test for not null and complete
        if ( job != null ) {
            return ResponseEntity.ok()
                    .contentType( MediaType.parseMediaType("application/octet-stream"))
                    .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getLabel() + "-input.pdb\"")
                    .body(job.getInputPDBContent());
        }
        return ResponseEntity.badRequest().body( "" );
    }

    @GetMapping("/job/{jobId}/inputFASTA")
    public ResponseEntity<String> jobInputFASTA( @PathVariable("jobId") String jobId) {
        THSJob job = jobManager.getSavedJob( jobId );

        // test for not null and complete
        if ( job != null ) {
            return ResponseEntity.ok()
                    .contentType( MediaType.parseMediaType("application/octet-stream"))
                    .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getLabel() + "-input.fasta\"")
                    .body(job.getInputFASTAContent());
        }
        return ResponseEntity.badRequest().body( "" );
    }


}
