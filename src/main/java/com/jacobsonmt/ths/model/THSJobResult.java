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
public final class THSJobResult {
    private Taxa taxa;
    private List<Base> bases;
    private String accession;
}
