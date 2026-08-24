package dev.arsonist.vacanciesmonitoring.repository;

import dev.arsonist.vacanciesmonitoring.model.Snapshot;
import org.springframework.data.mongodb.repository.DeleteQuery;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;

public interface SnapshotRepository extends MongoRepository<Snapshot, String> {

    @DeleteQuery(value = "{ 'test': ?0, 'fetchTime': { $lt: ?1 } }")
    void deleteOldSnapshots(boolean isTest, LocalDateTime cutoff);
}
