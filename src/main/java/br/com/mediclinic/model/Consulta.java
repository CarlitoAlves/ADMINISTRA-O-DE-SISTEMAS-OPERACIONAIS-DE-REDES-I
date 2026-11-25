package br.com.mediclinic.model;

import java.time.LocalDateTime;

/**
 * PLACEHOLDER: versão mínima só para o projeto compilar e os testes do
 * HistoricoService rodarem. Substituir pela entidade real do time.
 */
public class Consulta {

    private Long id;
    private LocalDateTime dataHora;
    private Medico medico;
    private Receita receita;

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Medico getMedico() {
        return medico;
    }

    public Receita getReceita() {
        return receita;
    }
}
