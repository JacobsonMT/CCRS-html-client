package com.jacobsonmt.ths.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class THSJobResult {

    private final String resultPDB;
    private final String resultCSV;

}
