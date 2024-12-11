package com.example.redshift_data_api_async;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.redshiftdata.RedshiftDataAsyncClient;
import software.amazon.awssdk.services.redshiftdata.model.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;

@Repository
public class RedshiftDataUserRepository implements UserRepository {
    private final RedshiftDataAsyncClient dataClient;

    @Value("${redshift.clusterId}")
    private String clusterId;
    @Value("${redshift.database.name}")
    private String databaseName;
    @Value("${redshift.database.user}")
    private String databaseUser;

    @Autowired
    public RedshiftDataUserRepository(RedshiftDataAsyncClient dataClient) {
        this.dataClient = dataClient;
    }

    @PreDestroy
    public void destroy() {
        this.dataClient.close();
    }

    @Override
    public List<User> listUsers() {
        ExecuteStatementRequest executeStatementRequest = ExecuteStatementRequest.builder()
                .clusterIdentifier(clusterId)
                .database(databaseName)
                .dbUser(databaseUser)
                .sql("SELECT * FROM Users;")
                .build();

        GetStatementResultResponse statementResult = this.dataClient.executeStatement(executeStatementRequest)
                .thenCompose(response -> pollForStatus(DescribeStatementRequest.builder().id(response.id()).build()))
                .thenCompose(response -> {
                    String resultSetId = response.id();
                    GetStatementResultRequest request = GetStatementResultRequest.builder().id(resultSetId).build();
                    return this.dataClient.getStatementResult(request);
                }).join();
        if (Objects.isNull(statementResult)) {
            return Collections.emptyList();
        }
        return statementResult.records().stream().map(this::constructUserDetails).toList();
    }

    private CompletableFuture<DescribeStatementResponse> pollForStatus(DescribeStatementRequest request) {
        return this.dataClient.describeStatement(request)
                .thenCompose(response -> {
                    StatusString status = response.status();
                    if (StatusString.FINISHED.equals(status)) {
                        return CompletableFuture.completedFuture(response);
                    }
                    if (StatusString.FAILED.equals(status)) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException(
                                        "Failed to retrieve results for the batch-execute, hence aborting"));
                    }
                    return pollForStatus(request);
                });
    }

    private User constructUserDetails(List<Field> row) {
        return new User(
                row.get(0).longValue(), row.get(1).stringValue(), row.get(2).stringValue(), row.get(3).stringValue());
    }
}
