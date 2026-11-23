package br.com.mediclinic.util;

import br.com.mediclinic.model.Receita;
import org.springframework.stereotype.Component;

/**
 * PLACEHOLDER: a geração real do PDF ainda não foi implementada.
 * Os testes do HistoricoService simulam (mock) esta classe.
 */
@Component
public class PdfGeneratorUtil {

    public byte[] gerarBytesReceita(Receita receita) {
        throw new UnsupportedOperationException("Geração de PDF ainda não implementada.");
    }
}
