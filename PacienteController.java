package br.com.mediclinic.controller;

import br.com.mediclinic.dto.PacienteRequestDTO;
import br.com.mediclinic.dto.PacienteResponseDTO;
import br.com.mediclinic.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    public ResponseEntity<PacienteResponseDTO> cadastrar(@Valid @RequestBody PacienteRequestDTO request) {
        PacienteResponseDTO pacienteSalvo = pacienteService.cadastrarPaciente(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(pacienteSalvo.id())
                .toUri();

        return ResponseEntity.created(location).body(pacienteSalvo);
    }
}   