package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.model.Base;
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
@Api( value = "Jobs API" )
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
    @ApiModel( description = "All details about the job submission." )
    private static final class SubmissionContent {
        @ApiModelProperty( notes = "Input FASTA to be processed",
                required = true,
                example = ">P07766 OX=9606\nMQSGTHWRVLGLCLLSVGVWGQDGNEEMGGITQTPYKVSISGTTVILTCPQYPGSEILWQHNDKNI" )
        private String fasta;
        @ApiModelProperty( notes = "Unique identifier for you the user. If not supplied one will be created and returned inside a 'WWW-Authenticate' header.",
                required = true,
                example = "59268BF313712A137594345B72A56E40" )
        private String userId = "";
        @ApiModelProperty( notes = "If supplied an email will be sent to notify you of status changes",
                example = "email@example.com" )
        private String email = "";
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @ApiModel( description = "All details about the job(s) submission." )
    private static final class APIJobSubmissionResult extends CCRSService.JobSubmissionResponse {

        public APIJobSubmissionResult( CCRSService.JobSubmissionResponse jsr, String userId, String fasta, String email){
            super(jsr.getMessages(), jsr.getAcceptedJobs(), jsr.getRejectedJobHeaders(), jsr.getTotalRequestedJobs());
            this.userId = userId;
            this.fasta = fasta;
            this.email = email;
        }

        public APIJobSubmissionResult( String userId, String fasta, String email){
            super();
            this.userId = userId;
            this.fasta = fasta;
            this.email = email;
        }

        @ApiModelProperty( notes = "Unique identifier for you the user. This should be included in all submissions.",
                example = "59268BF313712A137594345B72A56E40" )
        private String userId;
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
            @ApiResponse( code = 401, message = "userId not supplied. Please create a unique userId or use the userId located in the supplied 'WWW-Authenticate' header" ),
            @ApiResponse( code = 403, message = "Accessing the resource you were trying to reach is forbidden" ),
            @ApiResponse( code = 404, message = "The resource you were trying to reach is not found" )
    } )
    @RequestMapping( value = "/submit", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE} )
    public ResponseEntity<APIJobSubmissionResult> submitJob(
            @ApiParam( value = "All details about the job submission", required = true )
            @RequestBody SubmissionContent submissionContent
    ) {

        String userId = submissionContent.getUserId();

        if ( userId == null || userId.isEmpty() ) {
            return ResponseEntity.status( HttpStatus.UNAUTHORIZED )
                    .header( "WWW-Authenticate", "userId=" + UUID.randomUUID().toString() )
                    .body( null );
        }

        ResponseEntity<CCRSService.JobSubmissionResponse> jobSubmissionResponse = ccrsService.submitJob( userId,
                "",
                submissionContent.getFasta(),
                submissionContent.getEmail(),
                true );

        if (jobSubmissionResponse.getBody() != null) {
            return ResponseEntity
                    .status( jobSubmissionResponse.getStatusCodeValue() )
                    .location( UriBuilder
                            .fromPath( siteSettings.getFullUrl() )
                            .path( "api/user/{userId}/jobs" ).build( userId )
                    )
                    .body( new APIJobSubmissionResult(
                            jobSubmissionResponse.getBody(),
                            userId,
                            submissionContent.getFasta(),
                            submissionContent.getEmail()
                    ) );
        } else {
            return ResponseEntity
                    .status( HttpStatus.INTERNAL_SERVER_ERROR )
                    .body( new APIJobSubmissionResult(
                            userId,
                            submissionContent.getFasta(),
                            submissionContent.getEmail()
                    ) );
        }
    }

    @ApiOperation( value = "Retrieve all jobs for a specific user" )
    @ResponseStatus( value = HttpStatus.OK )
    @ApiResponses( value = {
            @ApiResponse( code = 200, message = "Successfully retrieved jobs" ),
            @ApiResponse( code = 401, message = "You are not authorized to view the resource" )
    } )
    @RequestMapping( value = "/user/{userId}/jobs", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE} )
    public ResponseEntity<List<THSJob>> jobsForUser(
            @ApiParam( value = "ID of user for which to retrieve jobs", required = true )
            @PathVariable( "userId" ) String userId,
            @ApiParam( value = "True if returned jobs should include results", defaultValue = "false")
            @RequestParam( value = "withResults", required = false, defaultValue = "false" ) boolean withResults ) {
        return ccrsService.getJobsForUser( userId, withResults );
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

    @ApiOperation( value = "Retrieve result bases scores for a specific job ID" )
    @ResponseStatus( value = HttpStatus.OK )
    @ApiResponses( value = {
            @ApiResponse( code = 200, message = "Successfully retrieved bases scores for job" ),
            @ApiResponse( code = 102, message = "Job is not yet complete, try again later" ),
            @ApiResponse( code = 401, message = "You are not authorized to view the resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID or job has failed" )
    } )
    @RequestMapping( value = "/job/{jobId}/bases", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE} )
    public ResponseEntity<List<Base>> getJobResultBases(
            @ApiParam( value = "ID of job for which to retrieve result bases scores", required = true )
            @PathVariable( "jobId" ) String jobId ) {
        THSJob job = ccrsService.getJob( jobId ).getBody();

        if ( job == null ) {
            return ResponseEntity.notFound().build();
        }

        if ( !job.isComplete() ) {
            return ResponseEntity.status( HttpStatus.PROCESSING ).build();
        }

        if ( job.isFailed() ) {
            return ResponseEntity.status( HttpStatus.NOT_FOUND ).body( null );
        }

        return ResponseEntity.ok( job.getResult().getBases() );
    }

    @ApiOperation( value = "Delete job for a specific job ID" )
    @ResponseStatus( value = HttpStatus.ACCEPTED )
    @ApiResponses( value = {
            @ApiResponse( code = 202, message = "Job is deleted, if possible"),
            @ApiResponse( code = 401, message = "You are not authorized to use this resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID" )
    } )
    @RequestMapping( value = "/job/{jobId}/delete", method = RequestMethod.DELETE )
    public ResponseEntity<String> deleteJob(
            @ApiParam( value = "ID of job to delete", required = true )
            @PathVariable( "jobId" ) String jobId ) {
        return ccrsService.deleteJob( jobId );
    }

    @ApiOperation( value = "Delete all jobs for a specific user ID" )
    @ResponseStatus( value = HttpStatus.ACCEPTED )
    @ApiResponses( value = {
            @ApiResponse( code = 202, message = "Jobs are deleted, if possible"),
            @ApiResponse( code = 401, message = "You are not authorized to use this resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID" )
    } )
    @RequestMapping( value = "/user/{userId}/jobs/delete", method = RequestMethod.DELETE )
    public ResponseEntity<String> deleteJobs(
            @ApiParam( value = "ID of user for which to delete all jobs", required = true )
            @PathVariable( "userId" ) String userId ) {
        return ccrsService.deleteJobs( userId );
    }

    @ApiOperation( value = "Retrieve raw result CSV for a specific job ID" )
    @ResponseStatus( value = HttpStatus.OK )
    @ApiResponses( value = {
            @ApiResponse( code = 200, message = "Successfully retrieved raw result CSV for job" ),
            @ApiResponse( code = 102, message = "Job is not yet complete, try again later" ),
            @ApiResponse( code = 401, message = "You are not authorized to view the resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID or job has failed" )
    } )
    @RequestMapping( value = "/job/{jobId}/resultCSV", method = RequestMethod.GET )
    public ResponseEntity<String> jobResultCSV(
            @ApiParam( value = "ID of job for which to retrieve raw result CSV", required = true )
            @PathVariable( "jobId" ) String jobId ) {
        return ResponseEntity.ok( ccrsService.downloadJobResultContent( jobId ).getBody() );
    }

}
