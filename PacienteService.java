package br.com.mediclinic.service;

import br.com.mediclinic.dto.PacienteRequestDTO;
import br.com.mediclinic.dto.PacienteResponseDTO;
import br.com.mediclinic.exception.CpfDuplicadoException;
import br.com.mediclinic.model.Paciente;
import br.com.mediclinic.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    @Transactional
    public PacienteResponseDTO cadastrarPaciente(PacienteRequestDTO dto) {
        // Normaliza o CPF removendo pontos e traços antes de consultar/salvar
        String cpfLimpo = dto.cpf().replaceAll("\\D", "");

        // Critério de Aceitação 2: Impede cadastro de CPF duplicado
        if (pacienteRepository.existsByCpf(cpfLimpo)) {
            throw new CpfDuplicadoException("Já existe um paciente cadastrado com o CPF informado.");
        }

        Paciente novoPaciente = new Paciente();
        novoPaciente.setNome(dto.nome().trim());
        novoPaciente.setCpf(cpfLimpo);
        novoPaciente.setDataNascimento(dto.dataNascimento());
        novoPaciente.setTelefone(dto.telefone().replaceAll("\\D", ""));
        novoPaciente.setEmail(dto.email() != null ? dto.email().trim().toLowerCase() : null);
        novoPaciente.setNomeConvenio(dto.nomeConvenio());
        novoPaciente.setNumeroCarteirinha(dto.numeroCarteirinha());

        Paciente pacienteSalvo = pacienteRepository.save(novoPaciente);
        return PacienteResponseDTO.fromEntity(pacienteSalvo);
    }
}