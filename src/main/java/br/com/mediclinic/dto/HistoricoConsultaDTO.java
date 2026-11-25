package br.com.mediclinic.dto;

import java.time.LocalDateTime;

public record HistoricoConsultaDTO(
    Long consultaId,
    LocalDateTime dataConsulta,
    String nomeMedico,
    String crmMedico,
    String especialidade,
    boolean possuiReceita,
    String urlDownloadPdf
) {}