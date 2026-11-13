package com.clinica.agendamento.repository;

import com.clinica.agendamento.model.Medico;
import com.clinica.agendamento.model.AgendaSlot;
import java.time.LocalDate;
import java.util.List;

public interface MedicoRepository {
    List<Medico> findByEspecialidadeAndConvenio(String especialidade, String convenio);
}

public interface ConsultaRepository {
    List<AgendaSlot> findHorariosLivres(Long medicoId, LocalDate data);
    void salvar(Consulta consulta);
}