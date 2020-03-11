package com.jacobsonmt.ths.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@ApiModel( value="Result", description = "Results of completed job." )
@Getter
@Setter
@NoArgsConstructor
public final class THSJobResult {

    private static List<String> alleleOrder = Lists.newArrayList("A","R","N","D","C","Q","E","G","H","I","L","K","M","F","P","S","T","W","Y","V");

    private Taxa taxa;
    @ApiModelProperty( notes = "Result row for each amino acid position in supplied FASTA, in order",
            example = "[{\"reference\":\"M\",\"depth\":41,\"conservation\":0.785,\"list\":[0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.785,0.0,0.785,0.785,0.785,0.785,0.785,0.785,0.785]}]" )
    private List<Base> bases;

    private String accession;

    @ApiModelProperty( notes = "Order of alleles in Base.list scores",
            example = "[\"A\",\"R\",\"N\",\"D\",\"C\",\"Q\",\"E\",\"G\",\"H\",\"I\",\"L\",\"K\",\"M\",\"F\",\"P\",\"S\",\"T\",\"W\",\"Y\",\"V\"]" )
    @JsonProperty("alleleOrder")
    private List<String> getAlleleOrder() {
        return alleleOrder;
    }

}
