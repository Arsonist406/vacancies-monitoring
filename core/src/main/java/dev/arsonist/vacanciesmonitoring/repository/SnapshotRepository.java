package dev.arsonist.vacanciesmonitoring.repository;

import dev.arsonist.vacanciesmonitoring.model.Snapshot;
import org.springframework.data.mongodb.repository.DeleteQuery;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;

public interface SnapshotRepository extends MongoRepository<Snapshot, String> {

    @DeleteQuery(value = "{ 'fetchTime': { $lt: ?0 } }")
    void deleteOldSnapshots(LocalDateTime cutoff);
}
