package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDto {
    private boolean blockchainConnected;
    private boolean databaseConnected;
    private boolean aiServiceOnline;
    private Long currentBlockNumber;
    private Integer pendingTransactions;
    private String systemHealth;
    private LocalDateTime lastChecked;
}
