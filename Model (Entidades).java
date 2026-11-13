// Representação simplificada das entidades de domínio
package com.clinica.agendamento.model;

import java.time.LocalDateTime;

public class Consulta {
    private Long id;
    private Long medicoId;
    private Long pacienteId;
    private LocalDateTime dataHora;
    private String status; // AGENDADA, CANCELADA
    // Construtores, Getters e Setters
}   