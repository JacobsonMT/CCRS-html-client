package com.jacobsonmt.ths.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public final class CCRSJobResult {

    private String resultCSV; //TODO: Kind of clunky storing two representations of the same thing
    private List<Base> sequence;

}
