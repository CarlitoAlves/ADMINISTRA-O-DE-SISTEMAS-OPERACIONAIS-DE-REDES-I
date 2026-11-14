package br.com.mediclinic.controller;

import br.com.mediclinic.dto.HistoricoConsultaDTO;
import br.com.mediclinic.service.HistoricoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes/{pacienteId}")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/historico")
    public ResponseEntity<List<HistoricoConsultaDTO>> listarHistorico(
            @PathVariable Long pacienteId,
            @RequestParam(defaultValue = "6m") String periodo) {

        List<HistoricoConsultaDTO> historico = historicoService.obterHistoricoConsultas(pacienteId, periodo);
        return ResponseEntity.ok(historico);
    }

    @GetMapping("/consultas/{consultaId}/receita/pdf")
    public ResponseEntity<byte[]> baixarReceitaPdf(
            @PathVariable Long pacienteId,
            @PathVariable Long consultaId) {

        byte[] pdfBytes = historicoService.gerarPdfReceita(pacienteId, consultaId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receita_consulta_" + consultaId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}