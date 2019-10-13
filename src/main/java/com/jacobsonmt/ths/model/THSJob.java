package com.jacobsonmt.ths.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.Date;

@Log4j2
@ApiModel( description = "Submitted job." )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"jobId", "label", "hidden"})
@EqualsAndHashCode(of = {"jobId"})
public class THSJob  {

    // Information on creation of job
    @ApiModelProperty( notes = "Unique ID of the job",
            example = "87288174-1cf8-4139-99e3-8648889ff29f" )
    private String jobId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private String clientId;
    @ApiModelProperty( notes = "Job label/title",
            example = "P07766 OX=9606" )
    private String label;
    @ApiModelProperty( notes = "Current status of the job",
            example = "Completed in 564s" )
    private String status;
    @ApiModelProperty( notes = "True if job is currently being processed",
            example = "false" )
    private boolean running;
    @ApiModelProperty( notes = "True if job has failed",
            example = "false" )
    private boolean failed;
    @ApiModelProperty( notes = "True if job has completed successfully or not",
            example = "true" )
    private boolean complete;
    @ApiModelProperty( notes = "Position in queue, null if complete or not yet in process queue",
            example = "42" )
    private Integer position;
    @ApiModelProperty( notes = "If supplied, an email will be sent to this address to notify you of status changes. Will display obfuscated for privacy reasons.",
            example = "em****@example.com" )
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private boolean hidden;
    @ApiModelProperty( notes = "Date when job was submitted to process queue, null otherwise",
            example = "2019-07-21T19:00:41.126+0000" )
    private Date submittedDate;
    @ApiModelProperty( notes = "Date when job started processing, null otherwise",
            example = "2019-07-21T19:00:41.126+0000" )
    private Date startedDate;
    @ApiModelProperty( notes = "Date when job finished processing, null otherwise",
            example = "2019-07-21T19:00:41.126+0000" )
    private Date finishedDate;
    @ApiModelProperty( notes = "Supplied input FASTA for this job",
            example = ">P07766 OX=9606\nMQSGTHWRVLGLCLLSVGVWGQDGNEEMGGITQTPYKVSISGTTVILTCPQYPGSEILWQHNDKNI" )
    private String inputFASTAContent;
    @ApiModelProperty( notes = "Object containing the processed results, null if failed or not complete" )
    private THSJobResult result;
    @ApiModelProperty( notes = "Time in seconds it took to from startedDate to finishedDate, 0 otherwise",
            example = "2019-07-21T19:00:41.126+0000" )
    private long executionTime;

    public static String obfuscateEmail( String email ) {
        if ( email == null ) {
            return null;
        }
        return email.replaceAll( "(\\w{0,3})(\\w+.*)(@.*)", "$1****$3" );
    }
}
