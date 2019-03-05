package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.exceptions.JobNotFoundException;
import com.jacobsonmt.ths.model.THSJob;
import com.jacobsonmt.ths.services.CCRSService;
import com.jacobsonmt.ths.utils.InputStreamUtils;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Log4j2
@Controller
public class JobController {

    @Autowired
    private CCRSService ccrsService;

    @PostMapping("/")
    public String submitJob(       @RequestParam(value = "fasta", required = false, defaultValue = "") String fasta,
                                   @RequestParam(value = "fastaFile", required = false) MultipartFile fastaFile,
                                   @RequestParam(value = "label", required = false, defaultValue = "") String label,
                                   @RequestParam(value = "email", required = false, defaultValue = "") String email,
//                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) throws IOException {

//        String ipAddress = request.getHeader( "X-FORWARDED-FOR" );
//        if ( ipAddress == null ) {
//            ipAddress = request.getRemoteAddr();
//        }

        String userId = RequestContextHolder.currentRequestAttributes().getSessionId();

        if ( fasta.isEmpty() ) {
            if (fastaFile != null) {
                fasta = InputStreamUtils.inputStreamToString( fastaFile.getInputStream() );
            } else {
                redirectAttributes.addFlashAttribute( "errorMessage", "FASTA not found" );
                return "redirect:/";
            }
        }

        if ( label.isEmpty() ) {
            String[] fastaLines = fasta.split( "\\r?\\n" );
            if (fastaLines.length == 2) {
                label = fastaLines[0];
            }

        }

        ResponseEntity<CCRSService.JobSubmissionResponse> jobSubmissionResponse = ccrsService.submitJob( userId,
                label,
                fasta,
                email,
                true );

        if ( jobSubmissionResponse.getStatusCodeValue() == 202 && jobSubmissionResponse.getBody() != null) {
            List<String> jobIds = jobSubmissionResponse.getBody().getJobIds();
            StringBuilder message;
            if ( jobIds.size() > 1 ) {
                message = new StringBuilder( "Multiple Jobs Submitted! View them in the <a href='queue/' target='_blank'>queue</a> or here: " );
//                        jobIds.stream().map( s -> "<a href='job/" + s + "' target='_blank'>here</a>" )
//                                .collect( Collectors.joining(", "));

                int idx = 1;
                for ( String jobid : jobIds ) {
                    message.append( "<a href='job/" ).append( jobid ).append( "' target='_blank'>" ).append( idx ).append( "</a> " );
                    idx++;
                }
            } else {
                message = new StringBuilder( "Job Submitted! View job <a href='job/" + jobIds.get( 0 ) + "' target='_blank'>here</a>." );
            }
            redirectAttributes.addFlashAttribute( "submitMessage", message.toString() );
            if (!jobSubmissionResponse.getBody().getMessage().isEmpty()) {
                redirectAttributes.addFlashAttribute( "warnMessage", jobSubmissionResponse.getBody().getMessage() );
            }
        } else {
            redirectAttributes.addFlashAttribute( "errorMessage",
                    jobSubmissionResponse.getBody() != null ? jobSubmissionResponse.getBody().getMessage() : "Server Error" );
        }

        return "redirect:/";
    }

    @GetMapping("/job/{jobId}")
    public String job( @PathVariable("jobId") String jobId,
                       Model model) throws IOException {

        THSJob job = ccrsService.getJob( jobId );

        if (job==null) {
            throw new JobNotFoundException();
        }

        model.addAttribute("job", job.obfuscate() ); // TODO: might have obfuscated already in CCRS

        return "job";
    }

    @GetMapping("/job2/{jobId}")
    public String job2( @PathVariable("jobId") String jobId,
                       Model model) throws IOException {

        THSJob job = ccrsService.getJob( jobId );

        if (job==null) {
            throw new JobNotFoundException();
        }

        model.addAttribute("job", job.obfuscate() ); // TODO: might have obfuscated already in CCRS

        return "job";
    }

    @GetMapping("/job/{jobId}/resultCSV")
    public ResponseEntity<String> jobResultCSV( @PathVariable("jobId") String jobId) {
        THSJob job = ccrsService.getJob( jobId );

        // test for not null and complete
        if ( job != null && job.isComplete() && !job.isFailed() ) {
            return ResponseEntity.ok()
                    .contentType( MediaType.parseMediaType("application/octet-stream"))
                    .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getLabel() + "-result.csv\"")
                    .body(job.getResult().getResultCSV());
        }
        return ResponseEntity.badRequest().body( "" );
    }

    @GetMapping("/job/{jobId}/inputFASTA")
    public ResponseEntity<String> jobInputFASTA( @PathVariable("jobId") String jobId) {
        THSJob job = ccrsService.getJob( jobId );

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
