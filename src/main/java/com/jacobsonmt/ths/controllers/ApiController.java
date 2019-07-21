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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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
        @NotBlank( message = "FASTA content missing!" )
        private String fasta;
        @ApiModelProperty( notes = "Unique identifier for you the user. If not supplied one will be created and returned for you to use in future submissions.",
                example = "59268BF313712A137594345B72A56E40" )
        private String userId = "";
        @ApiModelProperty( notes = "If supplied an email will be sent to notify you of status changes",
                example = "example@email.com" )
        private String email = "";
    }


    @Getter
    @AllArgsConstructor
    @ApiModel( description = "All details about the job(s) submission." )
    private final class SubmissionResult {

        @ApiModelProperty( notes = "Job ids for submitted jobs, in order of submission",
                example = "[\"8205ace5-8fa7-4f90-b0b1-ae02d5f6081d\", \"e48f1dfc-2acc-4f2f-b176-e5d13c46f7c9\"]" )
        private final List<String> jobIds;
        @ApiModelProperty( notes = "Unique identifier for you the user. This should be included in all submissions.",
                example = "59268BF313712A137594345B72A56E40" )
        private final String userId;
        @ApiModelProperty( notes = "Information related to the submissions. Can be validation warnings/errors.",
                example = "Multiple Jobs Submitted." )
        private final String message;
        @ApiModelProperty( notes = "Supplied input FASTA",
                example = ">P07766 OX=9606\nMQSGTHWRVLGLCLLSVGVWGQDGNEEMGGITQTPYKVSISGTTVILTCPQYPGSEILWQHNDKNI" )
        private final String fasta;
        @ApiModelProperty( notes = "If supplied, an email will be sent to this address to notify you of status changes",
                example = "example@email.com" )
        private final String email;

    }

    @ApiOperation( value = "Submit job to be processed",
            response = SubmissionResult.class,
            notes = "Check on status of particular job using /job/{jobId} endpoint" )
    @ResponseStatus( value = HttpStatus.ACCEPTED )
    @ApiResponses( value = {
            @ApiResponse( code = 202, message = "Successfully submitted job" ),
            @ApiResponse( code = 400, message = "Malformed content, usually an unrecoverable validation error with the input FASTA" ),
            @ApiResponse( code = 401, message = "You are not authorized to view the resource" ),
            @ApiResponse( code = 403, message = "Accessing the resource you were trying to reach is forbidden" ),
            @ApiResponse( code = 404, message = "The resource you were trying to reach is not found" )
    } )
    @RequestMapping( value = "/submit", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE} )
    public ResponseEntity<SubmissionResult> submitJob(
            @ApiParam( value = "All details about the job submission", required = true )
            @Valid @RequestBody SubmissionContent submissionContent
    ) {

        String userId = submissionContent.getUserId();

        if ( userId == null || userId.isEmpty() ) {
            userId = UUID.randomUUID().toString();
        }

        if ( submissionContent.getFasta() == null || submissionContent.getFasta().isEmpty() ) {
            return ResponseEntity.badRequest().body( new SubmissionResult( null,
                    userId,
                    "Empty FASTA",
                    "",
                    submissionContent.getEmail()
            ) );
        }

        ResponseEntity<CCRSService.JobSubmissionResponse> jobSubmissionResponse = ccrsService.submitJob( userId,
                "",
                submissionContent.getFasta(),
                submissionContent.getEmail(),
                true );

        if ( jobSubmissionResponse.getStatusCodeValue() == 202 && jobSubmissionResponse.getBody() != null ) {
            List<String> jobIds = jobSubmissionResponse.getBody().getJobIds();
            StringBuilder message;
            if ( jobIds.size() > 1 ) {
                message = new StringBuilder( "Multiple Jobs Submitted." );
            } else {
                message = new StringBuilder( "Job Submitted." );
            }

            if ( jobSubmissionResponse.getBody().getMessage().isEmpty() ) {
                message.append( " " ).append( jobSubmissionResponse.getBody().getMessage() );
            }

            return ResponseEntity
                    .status( jobSubmissionResponse.getStatusCodeValue() )
                    .location( UriBuilder
                            .fromPath( siteSettings.getFullUrl() )
                            .scheme( "http" )
                            .path( "api/user/{userId}/jobs" ).build( userId )
                    )
                    .body( new SubmissionResult(
                            jobIds,
                            userId,
                            message.toString(),
                            submissionContent.getFasta(),
                            submissionContent.getEmail()
                    ) );

        } else {
            return ResponseEntity
                    .status( jobSubmissionResponse.getStatusCodeValue() )
                    .body( new SubmissionResult( null,
                            userId,
                            jobSubmissionResponse.getBody() != null ? jobSubmissionResponse.getBody().getMessage() : "Server Error",
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
            @PathVariable( "jobId" ) String jobId ) {
        return ccrsService.getJob( jobId );
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
            @ApiResponse( code = 401, message = "You are not authorized to view the resource" ),
            @ApiResponse( code = 404, message = "No job exists with specified ID" )
    } )
    @RequestMapping( value = "/job/{jobId}/delete", method = RequestMethod.DELETE )
    public ResponseEntity<String> deleteJob(
            @ApiParam( value = "ID of job to delete", required = true )
            @PathVariable( "jobId" ) String jobId ) {
        return ccrsService.deleteJob( jobId );
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
