package br.com.fiap.petfiap.builder;

import br.com.fiap.petfiap.factory.AtendimentoFactory;
import br.com.fiap.petfiap.model.Atendimento;

import java.time.LocalDateTime;

// Padrao Builder (Aula 14): monta um atendimento complexo passo a passo,
// sem construtor gigante no controller.
public class AtendimentoBuilder {

    private String tipo;
    private String petNome;
    private String petPorte;
    private String tutorNome;
    private LocalDateTime dataHora;

    public AtendimentoBuilder comTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public AtendimentoBuilder comPet(String petNome, String petPorte) {
        this.petNome = petNome;
        this.petPorte = petPorte;
        return this;
    }

    public AtendimentoBuilder comTutor(String tutorNome) {
        this.tutorNome = tutorNome;
        return this;
    }

    public AtendimentoBuilder comDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
        return this;
    }

    // O objeto so nasce valido: a validacao dos campos obrigatorios fica
    // concentrada aqui, antes de a factory criar o atendimento.
    public Atendimento construir(int protocolo) {
        validarObrigatorio(petNome, "Nome do pet");
        validarObrigatorio(petPorte, "Porte do pet");
        return AtendimentoFactory.criar(protocolo, tipo, petNome, petPorte, tutorNome, dataHora);
    }

    private void validarObrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio");
        }
    }
}
