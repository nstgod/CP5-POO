# Checkpoint 5 — Bug Hunt PetFiap

API Spring Boot do **PetFiap** (pet shop & clínica veterinária da FIAP) recebida como
código legado: compilava e subia, mas a suíte de testes chegava **vermelha (20 testes,
9 falhando)**. Este repositório registra a caça: cada bug corrigido, cada ajuste de
Clean Code e cada teste novo tem o seu próprio commit, a partir do commit
`chore: estado original do projeto recebido`.

## Identificação

**Grupo:** 23

| Integrante | RM | Turma |
|---|---|---|
| Fabrício Cardoso de Oliveira | 561827 | 2CCPG |
| Leonardo Luster Gomes | 564448 | 2CCPG |
| Nelson Troccoli Santos Neto | 562815 | 2CCPG |
| Pedro Luis Tofoli | 564441 | 2CCPG |
| Raphael Talarico Nascimento Silva | 565219 | 2CCPG |
| Vinicius Barbosa Gomes | 564854 | 2CCPG |

| Campo | |
|---|---|
| **Total de bugs corrigidos** | **14** / 12 (os 12 pedidos + 2 achados a mais no code review e testando a API) |
| **Total de ajustes de Clean Code** | **7** / 6 (1 a mais) |
| **Total de testes novos escritos** | **6** / 6 |
| **Suíte final (Run As → JUnit Test)** | **26 testes, 0 falhas** (os 20 entregues + os 6 novos) |

---

## Parte 1 — Bugs encontrados

> Numeração na ordem em que foram encontrados — igual aos commits `fix: bugNN`.
> bug01 a bug07 vieram dos 9 testes vermelhos; bug08 a bug11 apareceram ao escrever
> os testes faltantes; bug12 a bug14 só apareceram lendo o código e subindo a API.

| # | Sintoma observado (o que fiz/vi) | Causa raiz (arquivo e linha aproximada) | Correção aplicada | Conceito da disciplina |
|---|---|---|---|---|
| bug01 | `GeradorProtocoloTest.deveManterUmaUnicaInstancia`: `expected: <GeradorProtocolo@7ac0e420> but was: <GeradorProtocolo@289710d9>` e `deveGerarProtocolosSequenciais`: `expected: <2> but was: <1>` — todo atendimento recebia o protocolo 1. | `model/GeradorProtocolo.java` ~l.18-20: `getInstancia()` fazia `return new GeradorProtocolo()` sem guardar o objeto em `instancia`, que ficava `null` para sempre. | `instancia = new GeradorProtocolo();` dentro do `if`, e depois `return instancia`. | Padrão **Singleton** (Aula 14): atributo `static` guarda a única instância. |
| bug02 | `AtendimentoFactoryTest.deveCriarTosaQuandoTipoForTosa`: `expected: <Tosa> but was: <Banho>` — pedir uma TOSA gerava um Banho (preço, pontos e duração errados). | `factory/AtendimentoFactory.java` l.17: `case "TOSA" -> new Banho(...)` (case trocado). | `case "TOSA" -> new Tosa(...)`. | Padrão **Factory** (Aula 14) + polimorfismo: a factory é o único lugar que conhece as subclasses. |
| bug03 | `AtendimentoFactoryTest.devePreencherOsDadosDoPetNaConsulta`: `expected: <Mimi> but was: <null>` — a consulta nascia sem pet, porte, tutor e data. | `model/ConsultaVeterinaria.java` l.16-18: o construtor recebia os 5 dados e chamava `super()` vazio, descartando tudo. Banho e Tosa chamavam `super(...)` — a Consulta destoava. | `super(protocolo, petNome, petPorte, tutorNome, dataHora);` | **Herança** e encadeamento de construtores (`super(...)`). |
| bug04 | `AtendimentoBuilderTest.deveMontarAtendimentoCompleto`: `expected: <Rex> but was: <null>` — passei "Rex" no builder e o objeto saiu sem nome. | `builder/AtendimentoBuilder.java` l.24: `petNome = petNome;` — o parâmetro sombreia o atributo e é atribuído a si mesmo. | `this.petNome = petNome;` | **Encapsulamento** / palavra-chave `this` (atributo × parâmetro com o mesmo nome). |
| bug05 | `deveRecusarMontagemSemNomeDoPet` e `deveRecusarMontagemSemPorte`: `Expected IllegalArgumentException to be thrown, but nothing was thrown` — o builder montava atendimento sem pet/porte. | `builder/AtendimentoBuilder.java` l.39-43: `construir()` não validava nada; o comentário dizia que "a validação fica por conta do controller", mas o controller também não validava. | `construir()` valida `petNome` e `petPorte` (nulo ou em branco) com `validarObrigatorio(...)` e lança `IllegalArgumentException` antes de chamar a factory; comentário corrigido. | Padrão **Builder** (Aula 14): o objeto só nasce válido; exceções *unchecked* (Aula 11). |
| bug06 | `AgendaServiceTest.deveRecusarAgendamentoComHorarioJaOcupado`: `expected: <HorarioOcupadoException> but was: <NullPointerException>` — o conflito passava e o código seguia até o `save` (o NPE vinha do recibo usando o retorno do mock). | `service/AgendaService.java` l.23: `a.getPetNome() == novo.getPetNome() && a.getDataHora() == novo.getDataHora()` compara **referências**. | Comparação por conteúdo: `.equals(...)` no nome do pet e na data/hora. | **`==` vs `.equals()`** (Aula 7). |
| bug07 | `AgendaServiceTest.deveLancarExcecaoQuandoAtendimentoNaoExiste`: `Expected AtendimentoNaoEncontradoException to be thrown, but nothing was thrown` — `buscarPorId(99)` devolvia `null`. | `service/AgendaService.java` l.37-42: `try { ...orElseThrow(...) } catch (Exception e) { return null; }` — o catch genérico engolia a exceção que o próprio método lançava. | Remoção do `try/catch`: o `orElseThrow` propaga a `AtendimentoNaoEncontradoException` (controller devolve 404; `concluir`/`cancelar` não tomam mais NPE). | **Tratamento de exceções** (Aula 11): catch genérico, exceção que nunca chega a quem chamou; `Optional.orElseThrow`. |
| bug08 | Teste novo **teste01**: `expected: <60.0> but was: <100.0>` — banho de pet PEQUENO custava R$ 100 e de pet GRANDE R$ 60. | `model/Banho.java` l.26-33: preços do `if/else` invertidos (a Tosa, com a mesma estrutura, estava certa). | PEQUENO → 60, MEDIO → 80, demais (GRANDE) → 100. | **Polimorfismo** / regra de negócio no model (`calcularPreco()` sobrescrito por subclasse). |
| bug09 | Teste novo **teste02**: `expected: <60> but was: <30>` — o `/resumo` de uma tosa mostrava 30 min. | `model/Tosa.java` l.40-42: `public int getDuracaoMinutos(String porte)` — assinatura diferente, sem `@Override`: era uma **sobrecarga**, e o método herdado (30 min) continuava valendo. | `@Override public int getDuracaoMinutos()` retornando 60. | **Sobrescrita × sobrecarga** e `@Override` (Aula 7). |
| bug10 | Teste novo **teste03**: `Expected StatusInvalidoException to be thrown, but nothing was thrown` — um atendimento já CONCLUIDO podia ser cancelado (e um CANCELADO, cancelado de novo). | `model/Atendimento.java` l.62-65: `cancelar()` mudava o status sem checar nada (o `concluir()` logo acima checava). | `cancelar()` só aceita AGENDADO; caso contrário lança `StatusInvalidoException` (controller devolve 409). | **Encapsulamento** das transições de estado + exceção customizada (Aula 11). |
| bug11 | Teste novo **teste04**: `expected: <IllegalArgumentException> but was: <NullPointerException>` — agendar para ontem era aceito: o service consultava o banco e salvava. | `service/AgendaService.java` l.20-21: `agendar()` não validava a data antes de ir ao repository. | Primeira linha do `agendar()`: se `dataHora.isBefore(LocalDateTime.now())` lança `IllegalArgumentException` — o banco nem é consultado. | Validação *fail-fast* no service; exceções *unchecked* (Aula 11); `verify(..., never())` do Mockito (Aula 15). |
| bug12 | Subi a API (H2 em memória) e rodei o curl do enunciado: `POST /api/atendimentos?tipo=BANHO...` → **HTTP 500**; no log: `IdentifierGenerationException: Identifier of entity 'Banho' must be manually assigned before calling 'persist()'`. Nenhum teste unitário enxerga (o repository é mock). | `model/Atendimento.java` l.14-15: `@Id private Long id;` **sem** `@GeneratedValue` — ninguém gerava o id. | `@GeneratedValue(strategy = GenerationType.IDENTITY)` no `id`. POST volta 201 com `id` 1, 2, 3... e o `GET /1/resumo` funciona. | **JPA / Spring Data** (Aula 13): mapeamento da chave primária. |
| bug13 | Code review: o comentário do `GeradorProtocolo` promete "Thread-safe para o uso concorrente do pet shop", mas nada é sincronizado. Prova (16 threads × 50.000 pedidos): **até 16 instâncias** do "singleton" e **~14 mil protocolos duplicados** em 800 mil. | `model/GeradorProtocolo.java` l.5 e l.17-27: `getInstancia()` (checa-e-cria) e `proximo()` (`contador++` = ler-somar-gravar) sem `synchronized`; o Tomcat atende cada requisição numa thread. | `synchronized` em `getInstancia()` e em `proximo()`. Mesma prova depois: 1 instância, 0 duplicados. | **Singleton** (Aula 14) em ambiente concorrente. |
| bug14 | `curl -X POST "...tipo=VACINA..."` → `HTTP 400` com **corpo vazio**. O contrato pede "Recusar com mensagem clara"; a mensagem `Tipo invalido: VACINA` existia na exceção, mas nunca chegava a quem chamou a API (o mesmo nos 404/409). | `controller/AtendimentoController.java` l.44-48, 56-58, 77-79, 87-91, 99-103: todos os `catch` faziam `ResponseEntity...build()` sem corpo. | Método `recusar(status, e)` devolve o motivo no corpo (`ProblemDetail` do Spring): `{"status":400,"detail":"Tipo invalido: VACINA",...}`. Mesmos status HTTP de antes. | API REST com `ResponseEntity` (Aulas 13/14) + exceções (Aula 11). |

---

## Parte 2 — Ajustes de Clean Code

| # | Onde estava | Qual princípio/boas práticas era violado | O que eu mudei |
|---|---|---|---|
| clean01 | `AtendimentoFactory.criar(int p, String t, String n, String po, String tu, LocalDateTime d)` | **Nomes significativos**: parâmetros de uma ou duas letras obrigam o leitor a decifrar `po` × `tu` (foi difícil até enxergar o case trocado do bug02). | Renomeados para `protocolo`, `tipo`, `petNome`, `petPorte`, `tutorNome`, `dataHora`. |
| clean02 | Fim do `AtendimentoController`: `private double calcularDescontoFidelidade(int pontos)` + comentário "Fidelidade (futuro) - implementar quando o time aprovar". | **Código morto / YAGNI**: método privado que ninguém chama e comentário usado como lista de tarefas. Ideia futura mora no backlog, não no código. | Método e comentário removidos. |
| clean03 | Construtor do `GeradorProtocolo`: `System.out.println("GeradorProtocolo criado!");` | **Debug esquecido em produção** / construtor com efeito colateral. | `println` removido. |
| clean04 | `AgendaService.agendar()`: `System.out.println("Recibo: atendimento ...")`. | **Responsabilidade única**: o service de regra de negócio fazendo I/O de console, sem nível de log nem controle de saída. | Trocado pelo `Logger` do SLF4J (`log.info("Recibo: atendimento {} agendado para {} ...")`), que já vem no Spring Boot — **nenhuma dependência nova no `pom.xml`**. |
| clean05 | `"AGENDADO"`, `"CONCLUIDO"`, `"CANCELADO"` repetidos no construtor, no `concluir()` e no `cancelar()` do `Atendimento` e no `AgendaService`. | **Strings mágicas / DRY**: um erro de digitação (`"AGENDADA"`) compila e quebra a regra em silêncio. | Constantes `Atendimento.STATUS_AGENDADO`, `STATUS_CONCLUIDO` e `STATUS_CANCELADO` usadas em todos os pontos. (O tipo continua `String` porque os testes entregues usam `getStatus()`/`setStatus("CONCLUIDO")` — trocar por `enum` exigiria mexer neles.) |
| clean06 | `AtendimentoController`: `ResponseEntity.status(201)` e `ResponseEntity.status(409)`. | **Números mágicos**: o leitor precisa saber de cor o que é 201 e 409. | `HttpStatus.CREATED` e `HttpStatus.CONFLICT`. |
| clean07 *(extra)* | `AgendaService.agendar()`: `for (Atendimento a : doPet)` com um `if` de três condições encadeadas. | **Nomes significativos + funções pequenas que revelam a intenção**. | Variáveis `existente` e `atendimentosDoPet`; a condição virou o método `ocupaMesmoHorario(existente, novo)`. |

---

## Parte 3 — Testes novos (regras que estavam sem cobertura)

**Como as regras sem cobertura foram descobertas:** cruzamos cada célula das tabelas do
contrato com os 20 testes entregues. Ficaram sem teste: preço do Banho por porte,
preço da Tosa em MEDIO/GRANDE, preço fixo da Consulta, duração da Tosa, agendamento no
passado e quase toda a linha de `cancelar()` (o service nem tinha teste de cancelamento).
Escolhemos as 6 regras abaixo e escrevemos cada teste no padrão da suíte
(AAA, nome `deve...Quando...`, `@Mock` onde há repository).

| # | Teste escrito (classe.método) | Regra coberta | Resultado ao escrever (vermelho/verde) |
|---|---|---|---|
| teste01 | `BanhoTest.deveCobrarPrecoDaTabelaQuandoPorteForPequenoMedioOuGrande` | BANHO custa R$ 60 / R$ 80 / R$ 100 para PEQUENO / MEDIO / GRANDE. | 🔴 **Vermelho**: `expected: <60.0> but was: <100.0>` → revelou o **bug08** (preços invertidos). |
| teste02 | `TosaTest.deveDurar60MinutosQuandoUsadaComoAtendimento` | TOSA dura 60 min — inclusive quando acessada pelo tipo abstrato `Atendimento`, como faz o `/resumo`. | 🔴 **Vermelho**: `expected: <60> but was: <30>` → revelou o **bug09** (sobrecarga no lugar de sobrescrita). |
| teste03 | `AgendaServiceTest.deveRecusarCancelamentoQuandoAtendimentoJaFoiConcluido` | `cancelar()` em CONCLUIDO → `StatusInvalidoException` e nada é salvo (`verify(repository, never()).save(any())`). | 🔴 **Vermelho**: `nothing was thrown` → revelou o **bug10** (cancelar sem checar status). |
| teste04 | `AgendaServiceTest.deveRecusarAgendamentoQuandoDataHoraEstaNoPassado` | Agendar no passado → `IllegalArgumentException` e **o banco nem é consultado** (`never()` em `findByPetNome` e em `save`). | 🔴 **Vermelho**: `expected: <IllegalArgumentException> but was: <NullPointerException>` → revelou o **bug11**. |
| teste05 | `ConsultaVeterinariaTest.deveCustar150ReaisQuandoPorteForPequenoOuGrande` | CONSULTA custa R$ 150 fixo — o porte não muda o preço. | 🟢 **Verde de cara**: regra já correta; agora protegida contra regressão. |
| teste06 | `AgendaServiceTest.deveCancelarQuandoAtendimentoEstaAgendado` | `cancelar()` em AGENDADO → vira CANCELADO e é salvo. | 🟢 **Verde de cara**: regra já correta; garante que a correção do bug10 não bloqueou o caminho feliz. |

> Conferência: rodamos os 6 testes novos contra o **código original** (commit inicial)
> e o resultado foi exatamente **4 vermelhos e 2 verdes**, como a dica do enunciado.
> Os 20 testes entregues **não foram alterados**: `git diff <commit-original> -- src/test`
> mostra só linhas adicionadas (os 6 métodos novos no fim das classes).

---

## Parte 4 — Perguntas de reflexão

### 1. A suíte como contrato (Aula 15)

Cada mensagem de falha dizia **o que entrou, o que saiu e onde**. Em
`deveMontarAtendimentoCompleto`, `expected: <Rex> but was: <null>` mostrava que "Rex"
entrava no builder e sumia antes do objeto nascer — o caminho era só `comPet()` →
`construir()` → factory, e lá estava o `petNome = petNome;` (bug04). Em
`deveGerarProtocolosSequenciais`, `expected: <2> but was: <1>` indicava um contador
que zerava a cada chamada, ou seja, um objeto novo a cada `getInstancia()` (bug01). O
mais traiçoeiro foi `expected: <HorarioOcupadoException> but was: <NullPointerException>`:
o NPE não vinha da comparação, e sim do recibo depois do `save` — prova de que o código
**passou** pela checagem de conflito, que só podia estar errada (o `==` do bug06). A
suíte roda em segundos, sem Oracle e sem rede, testa uma regra por vez e foi rodada
depois de **cada** commit para pegar regressões; com curl eu precisaria subir a API e o
banco, montar o estado na mão (agendar → concluir → cancelar) e ler cada resposta.
Por outro lado, o curl achou o bug12 e o bug14, que o mock não enxerga: os dois se completam.

### 2. Mock e injeção de dependência (Aulas 13 a 15)

Em produção, quem injeta é o **container do Spring**: na subida ele cria a
implementação de `AtendimentoRepository` (um proxy gerado pelo Spring Data JPA a partir
da interface, ligado ao Hibernate e ao Oracle), cria o bean `@Service AgendaService` e
preenche o campo `@Autowired private AtendimentoRepository repository`. No
`AgendaServiceTest` não existe container: a `MockitoExtension` cria um **falso** para o
`@Mock AtendimentoRepository` (implementa a interface, devolve vazio/`null` por padrão e
grava as chamadas), e o `@InjectMocks` instancia o `AgendaService` e coloca o mock no
**mesmo campo** que o Spring preencheria. O service não sabe quem o injetou — ele só
depende da interface. Como nenhum repository de verdade é criado, não há DataSource,
Hibernate nem conexão: o teste roda sem banco e sem subir o Spring. O comportamento é
ensinado com `when(repository.findById(1L)).thenReturn(Optional.of(agendado))` e
conferido com `verify`: no teste04, `verify(repository, never()).findByPetNome(any())`
prova que "o banco nem é consultado" — algo impossível de observar com curl.

### 3. `==` vs `.equals()` (Aula 7)

`String` e `LocalDateTime` são objetos: `==` compara **referências** (é o mesmo objeto
na memória?) e `.equals()` compara **conteúdo**. O teste
`deveRecusarAgendamentoComHorarioJaOcupado` cria de propósito o mesmo horário em outro
objeto — `LocalDateTime.parse(existente.getDataHora().toString())` — e por isso
`a.getDataHora() == novo.getDataHora()` dava `false` e o agendamento duplicado passava.
No mundo real é sempre assim: a data da requisição é convertida num objeto novo e a do
banco é outro objeto criado pelo Hibernate. Já `"Rex"` "funciona por sorte" porque
literais de String vão para o **String pool**: todo `"Rex"` escrito no código aponta para
o mesmo objeto, então `"Rex" == "Rex"` é `true` — e os dois `Banho` do teste foram
criados com o literal, o que escondia o problema no nome. Um nome vindo do
`@RequestParam` ou do banco é uma String nova, e o `==` falharia. A correção
(`existente.getPetNome().equals(novo.getPetNome()) && existente.getDataHora().equals(novo.getDataHora())`)
compara o valor, que é o que a regra de negócio quer.

### 4. Sobrescrita vs sobrecarga (Aula 7)

`Atendimento` declara `public int getDuracaoMinutos()` — sem parâmetros, retornando 30.
A `Tosa` declarava `public int getDuracaoMinutos(String porte)`: mesmo nome, **lista de
parâmetros diferente** — isso é **sobrecarga** (*overload*), um método novo que só existe
na Tosa, e o método herdado continuou intacto. **Sobrescrita** (*override*) exige a mesma
assinatura (nome + parâmetros). Como o controller chama `atendimento.getDuracaoMinutos()`
numa referência do tipo `Atendimento`, a ligação dinâmica procura uma sobrescrita do
método **sem parâmetros** na Tosa, não encontra e executa o da classe mãe: 30 minutos em
vez de 60. A versão com `String` nunca era chamada por ninguém. Com `@Override` em cima
dela, o compilador recusaria na hora (*"method does not override or implement a method
from a supertype"*): o bug viraria erro de compilação. O `Banho` usava `@Override` e
estava certo — comparar as subclasses entre si entregou o problema.

### 5. Singleton manual vs bean do Spring (Aula 14)

O `GeradorProtocolo` garante **uma única instância** em toda a aplicação: construtor
`private` (ninguém faz `new`), atributo `static instancia` e `getInstancia()` como único
ponto de acesso — assim a numeração dos protocolos é global e sequencial. O bug01 era
que `getInstancia()` fazia `return new GeradorProtocolo()` sem guardar o objeto em
`instancia`: o campo ficava `null` para sempre, cada chamada criava um gerador com
`contador = 0` e todo atendimento recebia o protocolo 1. Mesmo corrigido, ainda havia o
bug13: sem `synchronized`, duas requisições (threads do Tomcat) podiam ver `null` ao
mesmo tempo e criar duas instâncias, e o `contador++` podia entregar o mesmo número a
dois atendimentos (na nossa prova: 16 instâncias e ~14 mil duplicados). O
`AgendaService` não corre esse risco porque quem o mantém único é o **container do
Spring**: o bean é criado uma vez na subida, antes de chegar qualquer requisição, fica no
`ApplicationContext` e é injetado em quem precisa — não existe código nosso de
"checa-e-cria". Além disso ele não tem estado mutável (só o repository injetado), então
pode ser compartilhado entre threads com segurança.

### 6. Cobertura de testes: onde parar? (Aula 15)

Vale manter os verdes, sim. O teste06 (cancelar AGENDADO) passou de cara, mas é ele que
vigia a correção do bug10: se o `cancelar()` tivesse sido "corrigido" para recusar
**qualquer** status, o teste03 continuaria verde e só o teste06 acusaria. O teste05
protege a Consulta de alguém copiar o `if/else` de porte do Banho para ela. Teste verde
é seguro contra regressão, e custa poucas linhas e milissegundos. Com prazo, eu
priorizaria as **regras que mexem com dinheiro e estado** (preço, transições de status,
conflito de horário) cobrindo o caminho feliz **e** os principais caminhos de erro —
metade dos 14 bugs daqui (bug05, 06, 07, 10, 11, 13 e 14) estava em caminhos de erro ou
de borda, justamente onde a suíte entregue tinha menos testes. 100% de cobertura não é a
meta: cobertura mede linhas executadas, não regras verificadas, e getters ou o
`PetFiapApplication` pouco acrescentam. E teste unitário com mock não substitui um teste
de integração: o bug12 (`@GeneratedValue`) só aparece com banco de verdade.

---

## Parte 5 — Espaço livre (opcional)

```
Observação documentada (não corrigida, de propósito): o controller chama
GeradorProtocolo.proximo() ANTES de validar a requisição. Requisições recusadas
(conflito, data no passado, tipo inválido) "queimam" números: no nosso teste com curl,
o 4º agendamento válido recebeu o protocolo 7. Corrigir exigiria gerar o protocolo
dentro do AgendaService, depois das validações — o que acopla o service ao singleton
estático e faz o GeradorProtocoloTest entregue (que espera o primeiro número = 1)
depender da ordem de execução dos testes na mesma JVM. Pela regra de mudanças mínimas,
registramos aqui. A solução ideal seria o banco gerar o protocolo (sequence) ou
transformar o gerador num bean do Spring injetado no service.

Como testamos a API sem o Oracle: subimos o jar com H2 em memória passando as
propriedades por linha de comando (o application.properties continua com
SEU_RM/SUA_SENHA no repositório):

  java -jar target/petfiap-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:h2:mem:petfiap
       --spring.datasource.driverClassName=org.h2.Driver --spring.datasource.username=sa
       --spring.datasource.password=
```

---

## Como rodar

**Requisito:** JDK 17 ou superior (testado com JDK 21) e Maven (o do Eclipse/IntelliJ serve).

### Testes (não precisam de banco)

- **Eclipse:** *File → Import → Maven → Existing Maven Projects*, depois botão direito em
  `src/test/java` → *Run As → JUnit Test*.
- **IntelliJ:** *File → Open* na pasta do projeto, botão direito em `src/test/java` →
  *Run 'All Tests'*.
- **Terminal:** `mvn test` → `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`.

### API

1. Troque `SEU_RM` e `SUA_SENHA` em `src/main/resources/application.properties`
   (só na sua máquina — **não faça commit da senha**).
2. Rode a classe `PetFiapApplication`. Quando aparecer `Started PetFiapApplication`, a API
   está em `http://localhost:8080`.

```bash
# agendar (201 + atendimento com id e protocolo)
curl -i -X POST "http://localhost:8080/api/atendimentos?tipo=BANHO&petNome=Rex&porte=PEQUENO&tutorNome=Ana&dataHora=2026-12-01T10:00"
# preço, pontos e duração
curl http://localhost:8080/api/atendimentos/1/resumo
# concluir / cancelar
curl -i -X POST http://localhost:8080/api/atendimentos/1/conclusao
curl -i -X POST http://localhost:8080/api/atendimentos/1/cancelamento   # 409: já concluído
```

| Endpoint | Sucesso | Recusas (com a mensagem no corpo) |
|---|---|---|
| `POST /api/atendimentos` | 201 | 400 (tipo inválido, pet/porte ausente, data no passado), 409 (horário ocupado) |
| `GET /api/atendimentos/{id}` | 200 | 404 |
| `GET /api/atendimentos/{id}/resumo` | 200 | 404 |
| `GET /api/atendimentos/pet/{nome}` | 200 | — |
| `POST /api/atendimentos/{id}/conclusao` | 200 | 404, 409 (status não é AGENDADO) |
| `POST /api/atendimentos/{id}/cancelamento` | 200 | 404, 409 (status não é AGENDADO) |
