package com.jacobsonmt.ths.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private String clientId;
    private String label;
    private String status;
    private boolean running;
    private boolean failed;
    private boolean complete;
    private Integer position;
    private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private boolean hidden;
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

    public void migrateCSVResultToSequence() {
        if (this.result != null) {

            try ( BufferedReader reader = new BufferedReader(new StringReader(this.result.getResultCSV()))) {
                List<Base> sequence = reader.lines()
                        .skip( 2 ) // Skip OX taxa id and header
                        .map( mapBase )
                        .collect( Collectors.toList() );
                this.result.setSequence( sequence );
                this.result.setResultCSV( "" );
            } catch ( IOException exc) {
                // quit
            }

        }
    }

    private static Function<String, Base> mapBase = ( rawLine ) -> {
        List<String> line = Arrays.asList( rawLine.split( "\t" ) );

        Base base = new Base( line.get( 2 ), Integer.valueOf( line.get( 3 ) ), Double.valueOf( line.get( 4 ) ) );

        if ( line.size() > 5 ) {
            base.setList( line.stream().skip( 5 ).map( Double::parseDouble ).collect( Collectors.toList() ) );
        }
        return base;
    };

}
