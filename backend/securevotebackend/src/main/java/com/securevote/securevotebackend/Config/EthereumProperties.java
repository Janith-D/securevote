package com.securevote.securevotebackend.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
@ConfigurationProperties(prefix = "blockchain.ethereum")
@Data
public class EthereumProperties {
    private String rpcUrl;
    private String privateKey;
    private String contractAddress;
    private BigInteger gasLimit = BigInteger.valueOf(300_000);
    private BigInteger gasPrice =BigInteger.valueOf(20_000_000_000L);
}
