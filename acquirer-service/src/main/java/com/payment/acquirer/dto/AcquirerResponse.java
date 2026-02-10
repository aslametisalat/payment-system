package com.payment.acquirer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquirerResponse {
    private Boolean approved;
    private String message;
    private String acquirerId;
    private Integer fraudScore;
}