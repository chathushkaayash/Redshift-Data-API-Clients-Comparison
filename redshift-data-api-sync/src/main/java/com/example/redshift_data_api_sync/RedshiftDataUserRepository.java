package com.example.redshift_data_api_sync;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient;
import software.amazon.awssdk.services.redshiftdata.model.*;

import java.util.List;

@Repository
public class RedshiftDataUserRepository implements UserRepository {
    private final RedshiftDataClient dataClient;

    @Value("${redshift.clusterId}")
    private String clusterId;
    @Value("${redshift.database.name}")
    private String databaseName;
    @Value("${redshift.database.user}")
    private String databaseUser;

    @Autowired
    public RedshiftDataUserRepository(RedshiftDataClient dataClient) {
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
        ExecuteStatementResponse executeStatementResponse = this.dataClient.executeStatement(executeStatementRequest);

        String statementId = executeStatementResponse.id();

        DescribeStatementRequest describeStatementRequest = DescribeStatementRequest.builder().id(statementId).build();
        boolean isCompleted = false;
        DescribeStatementResponse describeStatementResponse = null;

        while (!isCompleted) {
            describeStatementResponse = this.dataClient.describeStatement(describeStatementRequest);

            if (StatusString.FINISHED.equals(describeStatementResponse.status())) {
                isCompleted = true;
            } else if (StatusString.FAILED.equals(describeStatementResponse.status())) {
                throw new RuntimeException("Failed to retrieve results for the batch-execute, hence aborting");
            }
        }

        GetStatementResultRequest resultRequest = GetStatementResultRequest.builder()
                .id(statementId)
                .build();

        GetStatementResultResponse resultResponse = this.dataClient.getStatementResult(resultRequest);

        return resultResponse.records().stream().map(this::constructUserDetails).toList();
    }

    private User constructUserDetails(List<Field> row) {
        return new User(
                row.get(0).longValue(), row.get(1).stringValue(), row.get(2).stringValue(), row.get(3).stringValue());
    }
}