package com.clinica.agendamento.controller;

import com.clinica.agendamento.dto.AgendamentoRequestDTO;
import com.clinica.agendamento.model.Consulta;
import com.clinica.agendamento.model.Medico;
import com.clinica.agendamento.model.AgendaSlot;
import com.clinica.agendamento.service.ConsultaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping("/medicos")
    public ResponseEntity<List<Medico>> listarMedicos(
            @RequestParam String especialidade, 
            @RequestParam String convenio) {
        List<Medico> medicos = consultaService.filtrarMedicos(especialidade, convenio);
        return ResponseEntity.ok(medicos);
    }

    @GetMapping("/medicos/{medicoId}/horarios")
    public ResponseEntity<List<AgendaSlot>> listarHorariosLivres(
            @PathVariable Long medicoId, 
            @RequestParam LocalDate data) {
        List<AgendaSlot> slots = consultaService.obterHorariosLivres(medicoId, data);
        return ResponseEntity.ok(slots);
    }

    @PostMapping
    public ResponseEntity<Consulta> agendarConsulta(@RequestBody AgendamentoRequestDTO request) {
        // O Controller apenas repassa os dados para o Service e retorna a resposta HTTP adequada
        Consulta novaConsulta = consultaService.realizarAgendamento(request);
        return ResponseEntity.status(201).body(novaConsulta);
    }
}