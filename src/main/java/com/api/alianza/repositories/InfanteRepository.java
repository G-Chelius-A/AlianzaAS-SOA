package com.api.alianza.repositories;

import com.api.alianza.entities.Infante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA específico para la entidad Infante.
 */
@Repository
public interface InfanteRepository extends JpaRepository<Infante, String> {
    List<Infante> findByIdTutor(String idTutor);
}
