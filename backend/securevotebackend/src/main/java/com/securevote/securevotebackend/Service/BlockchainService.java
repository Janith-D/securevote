package com.securevote.securevotebackend.Service;

import com.securevote.securevotebackend.Config.EthereumProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class BlockchainService {

    private final EthereumProperties properties;
    private Web3j web3j;
    private Credentials credentials;
    private BigInteger gasPrice;
    private BigInteger gasLimit;
    private String contractAddress;

    public BlockchainService(EthereumProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.web3j = Web3j.build(new HttpService(properties.getRpcUrl()));
        this.credentials = Credentials.create(properties.getPrivateKey());
        this.gasPrice = properties.getGasPrice();
        this.gasLimit = properties.getGasLimit();
        this.contractAddress = properties.getContractAddress();
        log.info("Blockchain service initialized with wallet: {}", credentials.getAddress());
    }

    public boolean isConnected() {
        try {
            Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
            return web3ClientVersion.getWeb3ClientVersion() != null;
        } catch (Exception e) {
            log.error("Failed to connect to blockchain network", e);
            return false;
        }
    }

    public BigInteger getBalance(String address) {
        try {
            EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            return balance.getBalance();
        } catch (Exception e) {
            log.error("Failed to get balance for address: {}", address, e);
            return BigInteger.ZERO;
        }
    }

    public CompletableFuture<TransactionReceipt> castVote(Long candidateId, String voterAddress) {
        log.info("Casting vote for candidateId: {}, voterAddress: {}", candidateId, voterAddress);
        return CompletableFuture.supplyAsync(() -> {
            try {
                EthGetTransactionCount ethGetTransactionCount = web3j
                        .ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                        .send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();
                log.info("Nonce retrieved: {}", nonce);

                String functionData = encodeVoteFunction(candidateId);
                log.info("Encoded function data: {}", functionData);

                RawTransaction rawTransaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice,
                        gasLimit,
                        contractAddress,
                        functionData
                );
                log.info("Raw transaction created: nonce={}, gasPrice={}, gasLimit={}", nonce, gasPrice, gasLimit);

                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);
                log.info("Transaction signed, hexValue length: {}", hexValue.length());

                EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
                if (ethSendTransaction.hasError()) {
                    log.error("Transaction error: {}", ethSendTransaction.getError().getMessage());
                    throw new RuntimeException("Transaction failed: " + ethSendTransaction.getError().getMessage());
                }

                String transactionHash = ethSendTransaction.getTransactionHash();
                log.info("Vote cast successfully. Transaction hash: {}", transactionHash);

                return waitForTransactionReceipt(transactionHash);
            } catch (Exception e) {
                log.error("Failed to cast vote for candidateId: {}, voterAddress: {}", candidateId, voterAddress, e);
                throw new RuntimeException("Failed to cast vote: " + e.getMessage(), e);
            }
        });
    }

    public BigInteger getVotesFor(Long candidateId) {
        try {
            // Encode getVotesFor(uint256) function call (simplified)
            String functionData = encodeGetVotesForFunction(candidateId);

            EthCall response = web3j.ethCall(
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                            credentials.getAddress(),
                            contractAddress,
                            functionData
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new RuntimeException("Call error: " + response.getError().getMessage());
            }

            return Numeric.toBigInt(response.getValue()); // Simplified decoding
        } catch (Exception e) {
            log.error("Failed to get votes", e);
            throw new RuntimeException("Failed to get votes: " + e.getMessage());
        }
    }

    private TransactionReceipt waitForTransactionReceipt(String transactionHash) {
        try {
            for (int i = 0; i < 30; i++) {
                EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(transactionHash).send();
                if (receiptResponse.getTransactionReceipt().isPresent()) {
                    return receiptResponse.getTransactionReceipt().get();
                }
                Thread.sleep(1000);
            }
            throw new RuntimeException("Transaction receipt not found after 30 seconds");
        } catch (Exception e) {
            log.error("Failed to get transaction receipt", e);
            throw new RuntimeException("Failed to get transaction receipt: " + e.getMessage());
        }
    }

    // Encode vote(uint256) function
    private String encodeVoteFunction(Long candidateId) {
        // vote(uint256) signature: 0xa9059cbb (actual signature from ABI)
        String paddedCandidateId = Numeric.toHexStringWithPrefixZeroPadded(
                BigInteger.valueOf(candidateId), // Use BigInteger directly
                32 // 32 bytes for uint256
        );
        return "0xa9059cbb" + paddedCandidateId.substring(2); // Remove "0x" prefix from padded value
    }

    // Encode getVotesFor(uint256) function
    private String encodeGetVotesForFunction(Long candidateId) {
        // getVotesFor(uint256) signature: 0xface1e8e (example, adjust with actual ABI)
        // Note: This is a placeholder. Use the correct function signature from Voting.sol ABI.
        String paddedCandidateId = Numeric.toHexStringWithPrefixZeroPadded(
                BigInteger.valueOf(candidateId),
                32
        );
        return "0xface1e8e" + paddedCandidateId.substring(2); // Adjust signature as needed
    }
}