package com.jacobsonmt.ths.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public final class THSJobResult {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String resultCSV;
    private int taxaId;
    private List<Base> sequence;

}
