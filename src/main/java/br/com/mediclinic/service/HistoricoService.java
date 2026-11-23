package br.com.mediclinic.service;

import br.com.mediclinic.dto.HistoricoConsultaDTO;
import br.com.mediclinic.model.Consulta;
import br.com.mediclinic.model.Receita;
import br.com.mediclinic.model.StatusConsulta;
import br.com.mediclinic.repository.HistoricoRepository;
import br.com.mediclinic.util.PdfGeneratorUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoricoService {

    private final HistoricoRepository historicoRepository;
    private final PdfGeneratorUtil pdfGeneratorUtil;

    public HistoricoService(HistoricoRepository historicoRepository, PdfGeneratorUtil pdfGeneratorUtil) {
        this.historicoRepository = historicoRepository;
        this.pdfGeneratorUtil = pdfGeneratorUtil;
    }

    @Transactional(readOnly = true)
    public List<HistoricoConsultaDTO> obterHistoricoConsultas(Long pacienteId, String periodo) {
        LocalDateTime dataInicio = calcularDataCorte(periodo);

        List<Consulta> consultas = historicoRepository.buscarConsultasRealizadas(
                pacienteId,
                StatusConsulta.FINALIZADA,
                dataInicio
        );

        return consultas.stream()
                .map(consulta -> mapearParaDTO(pacienteId, consulta))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] gerarPdfReceita(Long pacienteId, Long consultaId) {
        Receita receita = historicoRepository
                .buscarReceitaPorPacienteEConsulta(pacienteId, consultaId, StatusConsulta.FINALIZADA)
                .orElseThrow(() -> new EntityNotFoundException("Receita médica não encontrada para esta consulta."));

        return pdfGeneratorUtil.gerarBytesReceita(receita);
    }

    private LocalDateTime calcularDataCorte(String periodo) {
        if (periodo == null || periodo.equalsIgnoreCase("todos")) {
            return null;
        }

        LocalDateTime agora = LocalDateTime.now();
        return switch (periodo.toLowerCase()) {
            case "1m" -> agora.minusMonths(1);
            case "3m" -> agora.minusMonths(3);
            case "6m" -> agora.minusMonths(6);
            case "1a" -> agora.minusYears(1);
            default -> throw new IllegalArgumentException("Período inválido. Valores aceitos: 1m, 3m, 6m, 1a ou todos.");
        };
    }

    private HistoricoConsultaDTO mapearParaDTO(Long pacienteId, Consulta consulta) {
        boolean possuiReceita = consulta.getReceita() != null;
        String urlPdf = possuiReceita
                ? String.format("/api/pacientes/%d/consultas/%d/receita/pdf", pacienteId, consulta.getId())
                : null;

        return new HistoricoConsultaDTO(
                consulta.getId(),
                consulta.getDataHora(),
                consulta.getMedico().getNome(),
                consulta.getMedico().getCrm(),
                consulta.getMedico().getEspecialidade().getNome(),
                possuiReceita,
                urlPdf
        );
    }
}