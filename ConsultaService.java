package com.clinica.agendamento.service;

import com.clinica.agendamento.dto.AgendamentoRequestDTO;
import com.clinica.agendamento.model.Consulta;
import com.clinica.agendamento.model.Medico;
import com.clinica.agendamento.model.AgendaSlot;
import com.clinica.agendamento.repository.ConsultaRepository;
import com.clinica.agendamento.repository.MedicoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ConsultaService {

    private final MedicoRepository medicoRepository;
    private final ConsultaRepository consultaRepository;
    private final EmailService emailService; // Serviço externo simulado

    public ConsultaService(MedicoRepository medicoRepository, ConsultaRepository consultaRepository, EmailService emailService) {
        this.medicoRepository = medicoRepository;
        this.consultaRepository = consultaRepository;
        this.emailService = emailService;
    }

    public List<Medico> filtrarMedicos(String especialidade, String convenio) {
        return medicoRepository.findByEspecialidadeAndConvenio(especialidade, convenio);
    }

    public List<AgendaSlot> obterHorariosLivres(Long medicoId, LocalDate data) {
        return consultaRepository.findHorariosLivres(medicoId, data);
    }

    public Consulta realizarAgendamento(AgendamentoRequestDTO request) {
        // Validação de regra de negócio: garantir que o horário está livre
        List<AgendaSlot> livres = consultaRepository.findHorariosLivres(request.getMedicoId(), request.getDataHora().toLocalDate());
        boolean horarioDisponivel = livres.stream().anyMatch(slot -> slot.getDataHora().equals(request.getDataHora()));
        
        if (!horarioDisponivel) {
            throw new IllegalStateException("O horário selecionado não está mais disponível.");
        }

        Consulta consulta = new Consulta();
        consulta.setMedicoId(request.getMedicoId());
        consulta.setPacienteId(request.getPacienteId());
        consulta.setDataHora(request.getDataHora());
        consulta.setStatus("AGENDADA");

        consultaRepository.salvar(consulta);

        // Critério de Aceitação 3: Envio de notificação/e-mail
        emailService.enviarEmailConfirmacao(consulta);

        return consulta;
    }
}