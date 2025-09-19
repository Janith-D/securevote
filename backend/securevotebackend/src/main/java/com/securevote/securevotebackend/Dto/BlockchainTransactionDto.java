package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainTransactionDto {
    private String transactionHash;
    private String blockHash;
    private Long blockNumber;
    private String status;
    private Long gasUsed;
    private String gasPrice;
    private LocalDateTime timestamp;
}
