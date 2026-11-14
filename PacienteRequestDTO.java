package br.com.mediclinic.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;
import java.time.LocalDate;

public record PacienteRequestDTO(
    @NotBlank(message = "O nome completo é obrigatório.")
    @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
    String nome,

    @NotBlank(message = "O CPF é obrigatório.")
    @CPF(message = "O formato ou dígito verificador do CPF é inválido.")
    String cpf,

    @NotNull(message = "A data de nascimento é obrigatória.")
    @Past(message = "A data de nascimento deve ser uma data anterior a hoje.")
    LocalDate dataNascimento,

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "^\\(?\\d{2}\\)?[\\s-]?9?\\d{4}-?\\d{4}$", message = "Formato de telefone inválido.")
    String telefone,

    @Email(message = "O formato do e-mail é inválido.")
    String email,

    // Campos opcionais de convênio
    String nomeConvenio,
    String numeroCarteirinha
) {}