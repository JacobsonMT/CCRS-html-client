package com.jacobsonmt.ths.model;

import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.util.Date;

@Log4j2
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"jobId", "label", "hidden"})
@EqualsAndHashCode(of = {"jobId"})
public class THSJob  {

    // Information on creation of job
    private String jobId;
    private String clientId; //TODO: remove me
    private String label;
    private String status;
    private boolean running;
    private boolean failed;
    private boolean complete;
    private Integer position;
    private String email;
    private boolean hidden;
    private Date submittedDate;
    private Date startedDate;
    private Date finishedDate;
    private String inputFASTAContent;
    private CCRSJobResult result;
    private long executionTime;

    public THSJob obfuscate() {
        // We recreate CCRSJobResult without resultCSV to cut down on data transfer
        return new THSJob( jobId, clientId, label, status, running, failed, complete, position,
                email.replaceAll("(\\w{0,3})(\\w+.*)(@.*)", "$1****$3"),
                hidden, submittedDate, startedDate, finishedDate, inputFASTAContent,
                result == null ? null : new CCRSJobResult( "", result.getTaxaId(), result.getSequence() ), executionTime );
    }

}
