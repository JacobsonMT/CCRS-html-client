package com.jacobsonmt.ths.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@ApiModel( description = "Results describing the potential deleteriousness effect at a particular amino acid position." )
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"reference"})
@ToString
public class Base {
    @ApiModelProperty( notes = "The reference amino acid.",
            example = "M" )
    private String reference;
    @ApiModelProperty( notes = "Alignment depth.",
            example = "41" )
    private int depth;
    @ApiModelProperty( notes = "The average deleteriousness score of all possible mutations at that position",
            example = "0.785" )
    private double conservation;
    @ApiModelProperty( notes = "The potential deleteriousness score for mutating the reference amino acid to a given amino acid, 20 columns in order of 'ARNDCQEGHILKMFPSTWYV'.",
            example = "[0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.0,0.785,0.785,0.785,0.785,0.785,0.785,0.785]" )
    private List<Double> list = new ArrayList<>();
}
