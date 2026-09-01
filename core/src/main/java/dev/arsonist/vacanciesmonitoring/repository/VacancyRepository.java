package dev.arsonist.vacanciesmonitoring.repository;

import dev.arsonist.vacanciesmonitoring.model.JobBoard;
import dev.arsonist.vacanciesmonitoring.model.Vacancy;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface VacancyRepository extends MongoRepository<Vacancy, String> {

    @Query("{ 'companyName': ?0, 'jobBoard': ?1, 'filter': ?2, 'location': ?3, 'title': ?4 }")
    List<Vacancy> existsByKeys(String companyName, JobBoard jobBoard, String filter, String location, String title);
}
