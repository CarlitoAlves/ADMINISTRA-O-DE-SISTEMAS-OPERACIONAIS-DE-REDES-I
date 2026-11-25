package br.com.mediclinic.repository;

import br.com.mediclinic.model.Consulta;
import br.com.mediclinic.model.Receita;
import br.com.mediclinic.model.StatusConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoRepository extends JpaRepository<Consulta, Long> {

    @Query("""
        SELECT c FROM Consulta c
        JOIN FETCH c.medico m
        JOIN FETCH m.especialidade e
        LEFT JOIN FETCH c.receita r
        WHERE c.paciente.id = :pacienteId
          AND c.status = :status
          AND (:dataInicio IS NULL OR c.dataHora >= :dataInicio)
        ORDER BY c.dataHora DESC
    """)
    List<Consulta> buscarConsultasRealizadas(
            @Param("pacienteId") Long pacienteId,
            @Param("status") StatusConsulta status,
            @Param("dataInicio") LocalDateTime dataInicio
    );

    @Query("""
        SELECT r FROM Receita r
        JOIN FETCH r.consulta c
        JOIN FETCH c.paciente p
        JOIN FETCH c.medico m
        WHERE p.id = :pacienteId
          AND c.id = :consultaId
          AND c.status = :status
    """)
    Optional<Receita> buscarReceitaPorPacienteEConsulta(
            @Param("pacienteId") Long pacienteId,
            @Param("consultaId") Long consultaId,
            @Param("status") StatusConsulta status
    );
}