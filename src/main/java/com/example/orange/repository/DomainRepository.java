package com.example.orange.repository;

import com.example.orange.entities.DomainList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends JpaRepository<DomainList, Integer> {

}
