package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.model.THSJob;
import com.jacobsonmt.ths.services.CCRSService;
import com.jacobsonmt.ths.settings.SiteSettings;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.UUID;

@Log4j2
@Api(value = "Jobs API", description = "REST API for Job Submissions", tags = { "Jobs" })
@RequestMapping( "/api" )
@RestController
public class ApiController {

    @Autowired
    private CCRSService ccrsService;

    @Autowired
    private SiteSettings siteSettings;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel( value="Submission", description = "All details about the job submission." )
    private static final class SubmissionContent {
        @ApiModelProperty( notes = "Input FASTA to be processed",
                required = true,
                example = ">P07766 OX=9606\nMQSGTHWRVLGLCLLSVGVWGQDGNEEMGGITQTPYKVSISGTTVILTCPQYPGSEILWQHNDKNI" )
        private String fasta;
        @ApiModelProperty( notes = "Unique identifier for a batch of jobs. If not supplied one will be created and returned inside a 'WWW-Authenticate' header.",
                required = true,
                example = "59268BF313712A137594345B72A56E40" )
        private String batchId = "";
        @ApiModelProperty( notes = "If supplied an email will be sent to notify you of status changes",
                example = "email@example.com" )
        private String email = "";
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @ApiModel( value="SubmissionResponse", description = "All details about the job(s) submission." )
    private static final class APIJobSubmissionResult extends CCRSService.JobSubmissionResponse {

        public APIJobSubmissionResult( CCRSService.JobSubmissionResponse jsr, String batchId, String fasta, String email){
            super(jsr.getMessages(), jsr.getAcceptedJobs(), jsr.getRejectedJobHeaders(), jsr.getTotalRequestedJobs());
            this.batchId = batchId;
            this.fasta = fasta;
            this.email = email;
        }

        public APIJobSubmissionResult( String batchId, String fasta, String email){
            super();
            this.batchId = batchId;
            this.fasta = fasta;
            this.email = email;
        }

        @ApiModelProperty( notes = "Unique identifier for a batch of jobs. This should be included in all submissions.",
                example = "59268BF313712A137594345B72A56E40" )
        private String batchId;
        @ApiModelProperty( notes = "Supplied input FASTA",
                example = ">P07766 OX=9606\nMQSGTHWRVLGLCLLSVGVWGQDGNEEMGGITQTPYKVSISGTTVILTCPQYPGSEILWQHNDKNI" )
        private String fasta;
        @ApiModelProperty( notes = "If supplied, an email will be sent to this address to notify you of status changes",
                example = "email@example.com" )
        private String email;

    }

    @ApiOperation( value = "Submit job to be processed",
            response = APIJobSubmissionResult.class,
            notes = "Check on status of particular job using /job/{jobId} endpoint" )
    @ResponseStatus( value = HttpStatus.ACCEPTED )
    @ApiResponses( value = {
            @ApiResponse( code = 202, message = "Successfully submitted job" ),
            @ApiResponse( code = 400, message = "Malformed content, usually an unrecoverable validation error with the input FASTA" ),
            @ApiResponse( code = 401, message = "batchId not supplied. Please create a unique batchId or use the batchId located in the supplied 'WWW-Authenticate' header" ),
            @ApiResponse( code = 403, message = "Accessing the resource you were trying to reach is forbidden" ),
            @ApiResponse( code = 404, message = "The resource you were trying to reach is not found" )
    } )
    @RequestMapping( value = "/job", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE} )
    public ResponseEntity<APIJobSubmissionResult> submitJob(
            @ApiParam( value = "All details about the job submission", required = true )
            @RequestBody SubmissionContent submissionContent
    ) {

        String batchId = submissionContent.getBatchId();

        if ( batchId == null || batchId.isEmpty() ) {
            return ResponseEntity.status( HttpStatus.UNAUTHORIZED )
                    .header( "WWW-Authenticate", "batchId=" + UUID.randomUUID().toString() )
                    .body( null );
        }

        ResponseEntity<CCRSService.JobSubmissionResponse> jobSubmissionResponse = ccrsService.submitJob( batchId,
                "",
                submissionContent.getFasta(),
                submissionContent.getEmail(),
                true );

        if (jobSubmissionResponse.getBody() != null) {
            return ResponseEntity
                    .status( jobSubmissionResponse.getStatusCodeValue() )
                    .location( UriBuilder
                            .fromPath( siteSettings.getFullUrl() )
                            .path( "api/batch/{batchId}/jobs" ).build( batchId )
                    )
                    .body( new APIJobSubmissionResult(
                            jobSubmissionResponse.getBody(),
                            batchId,
                            submissionContent.getFasta(),
                            submissionContent.getEmail()
                    ) );
        } else {
            return ResponseEntity
                    .status( HttpStatus.INTERNAL_SERVER_ERROR )
                    .body( new APIJobSubmissionResult(
                            batchId,
                            submissionContent.getFasta(),
                            submissionContent.getEmail()
                    ) );
        }
    }

    @ApiOperation( value = "Retrieve all jobs for a specific batch" )
    @ResponseStatus( value = HttpStatus.OK )
    @ApiResponses( value = {
            @ApiResponse( code = 200, message = "Successfully retrieved jobs" ),
            @ApiResponse( code = 401, message = "You are not authorized to view the resource" )
    } )
    @RequestMapping( value = "/batch/{batchId}/jobs", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE} )
    public ResponseEntity<List<THSJob>> jobsForBatch(
            @ApiParam( value = "Batch ID for which to retrieve jobs", required = true )
            @PathVariable( "batchId" ) String batchId,
            @ApiParam( value = "True if returned jobs should include results", defaultValue = "false")
            @RequestParam( value = "withResults", required = false, defaultValue = "false" ) boolean withResults ) {
        return ccrsService.getJobsForUser( batchId, withResults );
    }

    @ApiOperation( value = "Retrieve job for a specific job ID" )
    @ResponseStatus( value = HttpStatus.OK )
    @ApiResponses( value = {
            @ApiResponse( code = 200, message = "Successfully retrieved job" ),
            @ApiResponse( code = 401, message = "You are not authorized to view the resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID" )
    } )
    @RequestMapping( value = "/job/{jobId}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE} )
    public ResponseEntity<THSJob> job(
            @ApiParam( value = "ID of job to retrieve", required = true )
            @PathVariable( "jobId" ) String jobId,
            @ApiParam( value = "True if returned job should include results", defaultValue = "true")
            @RequestParam( value = "withResults", required = false, defaultValue = "true" ) boolean withResults ) {
        return ccrsService.getJob( jobId, withResults );
    }

    @ApiOperation( value = "Delete job for a specific job ID" )
    @ResponseStatus( value = HttpStatus.ACCEPTED )
    @ApiResponses( value = {
            @ApiResponse( code = 202, message = "Job is deleted, if possible"),
            @ApiResponse( code = 401, message = "You are not authorized to use this resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID" )
    } )
    @RequestMapping( value = "/job/{jobId}", method = RequestMethod.DELETE )
    public ResponseEntity<String> deleteJob(
            @ApiParam( value = "ID of job to delete", required = true )
            @PathVariable( "jobId" ) String jobId ) {
        return ccrsService.deleteJob( jobId );
    }

    @ApiOperation( value = "Delete all jobs for a specific batch ID" )
    @ResponseStatus( value = HttpStatus.ACCEPTED )
    @ApiResponses( value = {
            @ApiResponse( code = 202, message = "Jobs are deleted, if possible"),
            @ApiResponse( code = 401, message = "You are not authorized to use this resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID" )
    } )
    @RequestMapping( value = "/batch/{batchId}/jobs", method = RequestMethod.DELETE )
    public ResponseEntity<String> deleteJobs(
            @ApiParam( value = "Batch ID for which to delete all jobs", required = true )
            @PathVariable( "batchId" ) String batchId ) {
        return ccrsService.deleteJobs( batchId );
    }

}
