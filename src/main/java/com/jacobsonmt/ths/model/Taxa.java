package com.jacobsonmt.ths.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ApiModel( description = "Supplied or guessed OX Taxa." )
@Getter
@Setter
@NoArgsConstructor
public class Taxa {

    @ApiModelProperty( notes = "Indication of how the given Taxa ID was determined. 'OX' if successful, otherwise a more specific key.",
            example = "invalid_OX" )
    private String key;

    @ApiModelProperty( notes = "Supplied or guessed OX Taxa ID for sequence, -1 if unknown or not supported",
            example = "9749" )
    private int id;

    @ApiModelProperty( notes = "Species name for OX Taxa ID, empty if unknown or not supported",
            example = "Delphinapterus leucas" )
    private String name;
}
