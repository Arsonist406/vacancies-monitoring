package dev.arsonist.vacanciesmonitoring.repository;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import dev.arsonist.vacanciesmonitoring.model.Vacancy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface VacancyRepository extends MongoRepository<Vacancy, String> {

    @Query("{ 'companyName': ?0, 'jobBoard': ?1, 'location': ?2, 'title': ?3 }")
    boolean existsByKeys(String companyName, JobBoard jobBoard, String location, String title);
}
