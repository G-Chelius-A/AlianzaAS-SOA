package com.api.alianza.repositories;

import com.api.alianza.entities.Menor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA específico para la entidad Menor.
 */
@Repository
public interface MenorRepository extends JpaRepository<Menor, String> {
    List<Menor> findByIdTutor(String idTutor);
}
