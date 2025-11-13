package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMktGameOverviewResponse {
    private StaffMktGameSummaryResponse lottery;
    private StaffMktGameSummaryResponse xocdia;
    private StaffMktGameSummaryResponse sicbo;
    private List<StaffMktBetResponse> recentBets;
}

