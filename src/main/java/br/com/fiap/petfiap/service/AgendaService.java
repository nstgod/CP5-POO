package br.com.fiap.petfiap.service;

import br.com.fiap.petfiap.exception.AtendimentoNaoEncontradoException;
import br.com.fiap.petfiap.exception.HorarioOcupadoException;
import br.com.fiap.petfiap.model.Atendimento;
import br.com.fiap.petfiap.repository.AtendimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// Regras de agenda do PetFiap: agendar, concluir e cancelar atendimentos.
@Service
public class AgendaService {

    private static final Logger log = LoggerFactory.getLogger(AgendaService.class);

    @Autowired
    private AtendimentoRepository repository;

    // Agenda um novo atendimento: recusa data no passado (antes de ir ao banco)
    // e horario ja ocupado pelo mesmo pet.
    public Atendimento agendar(Atendimento novo) {
        if (novo.getDataHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Nao e possivel agendar no passado: " + novo.getDataHora());
        }
        List<Atendimento> doPet = repository.findByPetNome(novo.getPetNome());
        for (Atendimento a : doPet) {
            if (a.getPetNome().equals(novo.getPetNome()) && a.getDataHora().equals(novo.getDataHora())
                    && Atendimento.STATUS_AGENDADO.equals(a.getStatus())) {
                throw new HorarioOcupadoException(
                        "Pet " + novo.getPetNome() + " ja possui atendimento agendado nesse horario");
            }
        }
        Atendimento salvo = repository.save(novo);
        log.info("Recibo: atendimento {} agendado para {} (tutor {})",
                salvo.getProtocolo(), salvo.getPetNome(), salvo.getTutorNome());
        return salvo;
    }

    // Busca pelo id; nunca retorna null, o orElseThrow garante a excecao.
    public Atendimento buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtendimentoNaoEncontradoException("Atendimento nao encontrado: " + id));
    }

    // Conclui o atendimento (status AGENDADO -> CONCLUIDO).
    public Atendimento concluir(Long id) {
        Atendimento atendimento = buscarPorId(id);
        atendimento.concluir();
        return repository.save(atendimento);
    }

    // Cancela o atendimento (status AGENDADO -> CANCELADO).
    public Atendimento cancelar(Long id) {
        Atendimento atendimento = buscarPorId(id);
        atendimento.cancelar();
        return repository.save(atendimento);
    }

    // Lista os atendimentos de um pet.
    public List<Atendimento> buscarPorPet(String petNome) {
        return repository.findByPetNome(petNome);
    }
}
