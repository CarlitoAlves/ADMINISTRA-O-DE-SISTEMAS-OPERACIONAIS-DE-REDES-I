package br.com.mediclinic.service;

import br.com.mediclinic.dto.HistoricoConsultaDTO;
import br.com.mediclinic.model.Consulta;
import br.com.mediclinic.model.Especialidade;
import br.com.mediclinic.model.Medico;
import br.com.mediclinic.model.Receita;
import br.com.mediclinic.model.StatusConsulta;
import br.com.mediclinic.repository.HistoricoRepository;
import br.com.mediclinic.util.PdfGeneratorUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do HistoricoService.
 *
 * Cenários planejados (Arrange–Act–Assert em todos):
 *  1. Caminho feliz: consulta finalizada com receita vira DTO com URL do PDF.
 *  2. Consulta sem receita: possuiReceita=false e URL nula.
 *  3. Períodos 1m/3m/6m/1a calculam a data de corte correta (fronteira).
 *  4. Período "todos" ou nulo não aplica filtro de data (fronteira).
 *  5. Período inválido lança IllegalArgumentException e não consulta o banco (erro).
 *  6. Histórico vazio devolve lista vazia; várias consultas mantêm a ordem.
 *  7. PDF da receita: encontrada devolve os bytes gerados (caminho feliz).
 *  8. PDF da receita: inexistente lança EntityNotFoundException (erro).
 *
 * Repository e gerador de PDF são simulados (mock) porque dependem de banco
 * e de biblioteca externa. As entidades são simuladas só nos getters que o
 * Service realmente usa, para o teste não depender de construtores/setters.
 */
@ExtendWith(MockitoExtension.class)
class HistoricoServiceTest {

    private static final Long PACIENTE_ID = 7L;

    @Mock
    private HistoricoRepository historicoRepository;

    @Mock
    private PdfGeneratorUtil pdfGeneratorUtil;

    // ------------------------------------------------------------------
    // obterHistoricoConsultas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Deve mapear consulta finalizada com receita para o DTO com a URL de download do PDF")
    void deveMapearConsultaComReceitaParaDto() {
        // Arrange
        LocalDateTime dataConsulta = LocalDateTime.of(2026, 3, 10, 14, 30);
        Consulta consulta = criarConsulta(42L, dataConsulta, "Dra. Helena Souza", "CRM-MG 12345", "Cardiologia", true);
        when(historicoRepository.buscarConsultasRealizadas(eq(PACIENTE_ID), eq(StatusConsulta.FINALIZADA), any()))
                .thenReturn(List.of(consulta));
        HistoricoService service = criarService();

        // Act
        List<HistoricoConsultaDTO> historico = service.obterHistoricoConsultas(PACIENTE_ID, "6m");

        // Assert
        assertThat(historico).hasSize(1);
        HistoricoConsultaDTO dto = historico.get(0);
        assertThat(dto.consultaId()).isEqualTo(42L);
        assertThat(dto.dataConsulta()).isEqualTo(dataConsulta);
        assertThat(dto.nomeMedico()).isEqualTo("Dra. Helena Souza");
        assertThat(dto.crmMedico()).isEqualTo("CRM-MG 12345");
        assertThat(dto.especialidade()).isEqualTo("Cardiologia");
        assertThat(dto.possuiReceita()).isTrue();
        assertThat(dto.urlDownloadPdf()).isEqualTo("/api/pacientes/7/consultas/42/receita/pdf");
    }

    @Test
    @DisplayName("Deve indicar que não há receita (e não gerar URL) quando a consulta não tem receita")
    void deveMarcarSemReceitaQuandoConsultaNaoPossuiReceita() {
        // Arrange
        Consulta consulta = criarConsulta(43L, LocalDateTime.of(2026, 4, 2, 9, 0), "Dr. Paulo Lima", "CRM-MG 999", "Clínica Geral", false);
        when(historicoRepository.buscarConsultasRealizadas(eq(PACIENTE_ID), eq(StatusConsulta.FINALIZADA), any()))
                .thenReturn(List.of(consulta));
        HistoricoService service = criarService();

        // Act
        List<HistoricoConsultaDTO> historico = service.obterHistoricoConsultas(PACIENTE_ID, "6m");

        // Assert
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).possuiReceita()).isFalse();
        assertThat(historico.get(0).urlDownloadPdf()).isNull();
    }

    @ParameterizedTest(name = "período {0} deve filtrar pela data de corte correspondente")
    @MethodSource("periodosValidosEDataDeCorte")
    void deveCalcularDataDeCortePeloPeriodo(String periodo, UnaryOperator<LocalDateTime> calculoEsperado) {
        // Arrange
        when(historicoRepository.buscarConsultasRealizadas(any(), any(), any())).thenReturn(List.of());
        HistoricoService service = criarService();
        ArgumentCaptor<LocalDateTime> dataCorteCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // Act (o Service lê o relógio real, então medimos uma janela antes/depois da chamada)
        LocalDateTime antes = LocalDateTime.now();
        service.obterHistoricoConsultas(PACIENTE_ID, periodo);
        LocalDateTime depois = LocalDateTime.now();

        // Assert
        verify(historicoRepository).buscarConsultasRealizadas(
                eq(PACIENTE_ID), eq(StatusConsulta.FINALIZADA), dataCorteCaptor.capture());
        assertThat(dataCorteCaptor.getValue())
                .isBetween(calculoEsperado.apply(antes), calculoEsperado.apply(depois));
    }

    static Stream<Arguments> periodosValidosEDataDeCorte() {
        return Stream.of(
                Arguments.of("1m", (UnaryOperator<LocalDateTime>) d -> d.minusMonths(1)),
                Arguments.of("3m", (UnaryOperator<LocalDateTime>) d -> d.minusMonths(3)),
                Arguments.of("6m", (UnaryOperator<LocalDateTime>) d -> d.minusMonths(6)),
                Arguments.of("1a", (UnaryOperator<LocalDateTime>) d -> d.minusYears(1)),
                // o período não diferencia maiúsculas de minúsculas
                Arguments.of("6M", (UnaryOperator<LocalDateTime>) d -> d.minusMonths(6)),
                Arguments.of("1A", (UnaryOperator<LocalDateTime>) d -> d.minusYears(1))
        );
    }

    @ParameterizedTest(name = "período \"{0}\" não deve aplicar filtro de data")
    @ValueSource(strings = {"todos", "TODOS", "Todos"})
    @NullSource
    void naoDeveFiltrarPorDataQuandoPeriodoForTodosOuNulo(String periodo) {
        // Arrange
        when(historicoRepository.buscarConsultasRealizadas(any(), any(), any())).thenReturn(List.of());
        HistoricoService service = criarService();

        // Act
        service.obterHistoricoConsultas(PACIENTE_ID, periodo);

        // Assert: data de início nula = sem filtro (a query trata ":dataInicio IS NULL")
        verify(historicoRepository).buscarConsultasRealizadas(PACIENTE_ID, StatusConsulta.FINALIZADA, null);
    }

    @ParameterizedTest(name = "período inválido \"{0}\" deve lançar IllegalArgumentException")
    @ValueSource(strings = {"2m", "12m", "1d", "abc", "", " "})
    void deveLancarExcecaoQuandoPeriodoForInvalido(String periodoInvalido) {
        // Arrange
        HistoricoService service = criarService();

        // Act
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class,
                () -> service.obterHistoricoConsultas(PACIENTE_ID, periodoInvalido));

        // Assert
        assertThat(excecao.getMessage()).contains("Período inválido");
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("Deve devolver lista vazia quando o paciente não tem consultas finalizadas")
    void deveRetornarListaVaziaQuandoNaoHaConsultas() {
        // Arrange
        when(historicoRepository.buscarConsultasRealizadas(any(), any(), any())).thenReturn(List.of());
        HistoricoService service = criarService();

        // Act
        List<HistoricoConsultaDTO> historico = service.obterHistoricoConsultas(PACIENTE_ID, "todos");

        // Assert
        assertThat(historico).isEmpty();
    }

    @Test
    @DisplayName("Deve manter a ordem devolvida pelo repositório (mais recente primeiro) e mapear cada consulta")
    void deveManterOrdemDasConsultasDoRepositorio() {
        // Arrange
        Consulta maisRecente = criarConsulta(2L, LocalDateTime.of(2026, 5, 20, 10, 0), "Dra. Ana", "CRM 1", "Dermatologia", true);
        Consulta maisAntiga = criarConsulta(1L, LocalDateTime.of(2026, 1, 15, 8, 0), "Dr. Bruno", "CRM 2", "Ortopedia", false);
        when(historicoRepository.buscarConsultasRealizadas(any(), any(), any()))
                .thenReturn(List.of(maisRecente, maisAntiga));
        HistoricoService service = criarService();

        // Act
        List<HistoricoConsultaDTO> historico = service.obterHistoricoConsultas(PACIENTE_ID, "1a");

        // Assert
        assertThat(historico).extracting(HistoricoConsultaDTO::consultaId).containsExactly(2L, 1L);
        assertThat(historico).extracting(HistoricoConsultaDTO::possuiReceita).containsExactly(true, false);
        assertThat(historico).extracting(HistoricoConsultaDTO::especialidade).containsExactly("Dermatologia", "Ortopedia");
    }

    // ------------------------------------------------------------------
    // gerarPdfReceita
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Deve devolver os bytes do PDF gerado quando a receita da consulta existe")
    void deveGerarPdfQuandoReceitaExiste() {
        // Arrange
        Receita receita = mock(Receita.class);
        byte[] pdfEsperado = {'%', 'P', 'D', 'F'};
        when(historicoRepository.buscarReceitaPorPacienteEConsulta(PACIENTE_ID, 42L, StatusConsulta.FINALIZADA))
                .thenReturn(Optional.of(receita));
        when(pdfGeneratorUtil.gerarBytesReceita(receita)).thenReturn(pdfEsperado);
        HistoricoService service = criarService();

        // Act
        byte[] pdf = service.gerarPdfReceita(PACIENTE_ID, 42L);

        // Assert
        assertThat(pdf).isEqualTo(pdfEsperado);
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException e não gerar PDF quando não há receita para a consulta")
    void deveLancarExcecaoQuandoReceitaNaoExiste() {
        // Arrange
        when(historicoRepository.buscarReceitaPorPacienteEConsulta(PACIENTE_ID, 99L, StatusConsulta.FINALIZADA))
                .thenReturn(Optional.empty());
        HistoricoService service = criarService();

        // Act
        EntityNotFoundException excecao = assertThrows(EntityNotFoundException.class,
                () -> service.gerarPdfReceita(PACIENTE_ID, 99L));

        // Assert
        assertThat(excecao.getMessage()).contains("Receita médica não encontrada");
        verify(pdfGeneratorUtil, never()).gerarBytesReceita(any());
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private HistoricoService criarService() {
        return new HistoricoService(historicoRepository, pdfGeneratorUtil);
    }

    /**
     * Monta uma consulta simulando apenas os getters que o Service lê.
     * Se as entidades reais do projeto mudarem, só este método precisa ser ajustado.
     */
    private Consulta criarConsulta(Long id, LocalDateTime dataHora, String nomeMedico, String crm,
                                   String nomeEspecialidade, boolean comReceita) {
        // Cada teste usa só parte dos getters, então estes mocks são "lenient"
        // (não falham por stubs não utilizados).
        Especialidade especialidade = mock(Especialidade.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        when(especialidade.getNome()).thenReturn(nomeEspecialidade);

        Medico medico = mock(Medico.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        when(medico.getNome()).thenReturn(nomeMedico);
        when(medico.getCrm()).thenReturn(crm);
        when(medico.getEspecialidade()).thenReturn(especialidade);

        // Criado antes do when(...) para não aninhar chamadas do Mockito dentro do thenReturn
        Receita receita = comReceita ? mock(Receita.class) : null;

        Consulta consulta = mock(Consulta.class, Mockito.withSettings().strictness(Strictness.LENIENT));
        when(consulta.getId()).thenReturn(id);
        when(consulta.getDataHora()).thenReturn(dataHora);
        when(consulta.getMedico()).thenReturn(medico);
        when(consulta.getReceita()).thenReturn(receita);
        return consulta;
    }
}
