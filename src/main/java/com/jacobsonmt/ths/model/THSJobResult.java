package com.jacobsonmt.ths.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@ApiModel( description = "Results of completed job." )
@Getter
@Setter
@NoArgsConstructor
public final class THSJobResult {

    @ApiModelProperty( notes = "Supplied or guessed OX Taxa ID for sequence, -1 if unknown or not supported",
            example = "9749" )
    private Taxa taxa;
    @ApiModelProperty( notes = "Result row for each amino acid position in supplied FASTA, in order",
            example = "[{\"reference\":\"M\",\"depth\":41,\"conservation\":0.785,\"list\":[0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.0,0.785,0.785,0.785,0.785,0.785,0.785,0.785]}]" )
    private List<Base> bases;

    private String accession;

}
