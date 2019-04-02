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
//                                   @RequestParam(value = "label", required = false, defaultValue = "") String label,
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
                redirectAttributes.addFlashAttribute( "errorMessage", "FASTA Not Found" );
                return "redirect:/";
            }
        }

        ResponseEntity<CCRSService.JobSubmissionResponse> jobSubmissionResponse = ccrsService.submitJob( userId,
                "",
                fasta,
                email,
                true );

        if ( jobSubmissionResponse.getStatusCodeValue() == 202 && jobSubmissionResponse.getBody() != null) {
            List<String> jobIds = jobSubmissionResponse.getBody().getJobIds();
            StringBuilder message;
            if ( jobIds.size() > 1 ) {
                message = new StringBuilder( "Multiple Jobs Submitted" );
            } else {
                message = new StringBuilder( "Job Submitted" );
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

    @GetMapping("/job/{jobId}/delete")
    public ResponseEntity<String> deleteJob( @PathVariable("jobId") String jobId ) {

        String result = ccrsService.deleteJob( jobId );

        if (result==null) {
            throw new JobNotFoundException();
        }

        return ResponseEntity.ok( "Job Deleted" );
    }

    @GetMapping("/job/{jobId}/resultCSV")
    public ResponseEntity<String> jobResultCSV( @PathVariable("jobId") String jobId) {
        THSJob job = ccrsService.getJob( jobId );

        // test for not null and complete
        if ( job != null && job.isComplete() && !job.isFailed() ) {
            return ResponseEntity.ok()
                    .contentType( MediaType.parseMediaType("application/octet-stream"))
                    .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getLabel() + ".list\"")
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
                    .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getLabel() + ".fasta\"")
                    .body(job.getInputFASTAContent());
        }
        return ResponseEntity.badRequest().body( "" );
    }


}
