package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmartContractEventDto {
    private String eventName;
    private String transactionHash;
    private Long blockNumber;
    private Object eventData;
    private LocalDateTime timestamp;
}
