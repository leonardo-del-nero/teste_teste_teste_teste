# User Service - Sistema Multi-tenant com Design Patterns

Este projeto é uma aplicação **Spring Boot** desenvolvida para a disciplina **ILP037 - Técnicas de Programação II**.

O objetivo principal é demonstrar a **refatoração de um sistema legado** para uma arquitetura robusta, utilizando **Design Patterns (GoF)**, respeitando princípios SOLID e implementando uma arquitetura **Multi-tenant** com interface administrativa em **Thymeleaf**.

---

## 🚀 Principais Características

* **Refatoração de Legado**: Evolução de um CRUD simples para uma arquitetura desacoplada.
* **Arquitetura Multi-Tenant**: Isolamento de dados baseado em coluna (`discriminator`) via header `x-tenant` ou sessão web.
* **Design Patterns**: Aplicação prática de padrões Criacionais, Estruturais e Comportamentais.
* **Interface Web Responsiva**: Dashboard administrativo criado com Thymeleaf e Bootstrap 5.
* **Auditoria**: Sistema de log de operações em arquivo (`audit.log`) via Adapter.
* **Segurança**: Hashing de senhas com BCrypt e validação de força de senha via Strategy.

---

## 🏛️ Arquitetura do Sistema

Em conformidade com os requisitos do projeto, a aplicação segue a **Arquitetura em Camadas (Layered Architecture)** típica do ecossistema Spring Boot, enriquecida com camadas de abstração para os padrões de projeto.

* **Controller (Web/API)**: Gerencia requisições HTTP, valida DTOs e direciona o fluxo.
* **Service (Business)**: Encapsula as regras de negócio e orquestra os Design Patterns.
* **Repository (Data)**: Abstrai o acesso ao banco H2 via Spring Data JPA.
* **Domain/Model**: Entidades (`UserEntity`) e Interfaces.
* **Infrastructure**: Implementações concretas dos padrões (Adapters, Strategies).

> **Nota sobre C4 Model:** Os diagramas de contexto e container (Nível 1 e 2) exigidos na atividade encontram-se na pasta `/docs` do repositório.

---

## 🏗️ Design Patterns Aplicados (Refatoração)

Abaixo detalhamos a aplicação dos 3 padrões de projeto exigidos para resolver problemas de acoplamento do sistema original.

### 1. Strategy (Comportamental) - Validação de Senha
* **Problema (Legado)**: A validação de senha estava "hardcoded" no Service. Mudar a regra (ex: de "simples" para "forte") exigia alterar e recompilar a classe principal, violando o OCP (Open/Closed Principle).
* **Solução (Pattern)**: Criação da interface `PasswordStrategy`.
* **Implementação**: O Service desconhece a regra. Ele apenas chama `.validate()`. A classe `StrongPasswordStrategy` encapsula a lógica atual.

```java
// Interface (Contrato)
public interface PasswordStrategy {
    void validate(String password);
}

// Implementação Concreta (Estratégia)
@Component
@Primary
public class StrongPasswordStrategy implements PasswordStrategy {
    @Override
    public void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }
        // ... outras regras
    }
}
```

### 2. Factory (Criacional) - Criação de Usuário
* **Problema (Legado)**: A criação da entidade `UserEntity` misturava lógica de mapeamento (DTO para Entity) com lógica de criptografia (BCrypt) dentro do Service.
* **Solução (Pattern)**: Centralizar a criação na `UserFactory`.
* **Implementação**: A Factory garante que todo usuário criado já nasça com a senha criptografada corretamente, isolando essa responsabilidade.

```java
@Component
@RequiredArgsConstructor
public class UserFactory {
    private final PasswordEncoder passwordEncoder;

    public UserEntity createEntityFromDTO(UserDTO dto) {
        UserEntity entity = new UserEntity();
        entity.setUsername(dto.getUsername());
        // A Factory decide que a senha deve ser hashada ao nascer
        entity.setPassword(passwordEncoder.encode(dto.getPassword())); 
        entity.setRoles(dto.getRoles());
        return entity;
    }
}
```

### 3. Adapter (Estrutural) - Sistema de Auditoria
* **Problema (Legado)**: O sistema precisava registrar logs em arquivo físico, mas acoplar o Service diretamente à biblioteca `java.io` dificultaria testes e futuras migrações para banco de dados.
* **Solução (Pattern)**: Interface `AuditService` e adaptador `FileAuditAdapter`.
* **Implementação**: O `FileAuditAdapter` adapta a interface de domínio para a escrita em baixo nível no sistema de arquivos.

```java
// O Service depende apenas desta Interface
public interface AuditService {
    void log(String operacao, String detalhes);
}

// O Adapter lida com a complexidade de I/O
@Component
public class FileAuditAdapter implements AuditService {
    public void log(String operacao, String detalhes) {
        try (FileWriter fw = new FileWriter("audit.log", true)) {
            // Escrita em arquivo
        } catch (IOException e) {
            System.err.println("Erro de auditoria: " + e.getMessage());
        }
    }
}
```

---

## 🖥️ Interface Web e Multi-tenancy

O sistema possui um mecanismo inteligente de gestão de tenants (clientes):

1.  **Seleção de Tenant**: Ao acessar a home, o usuário define o contexto da organização.
2.  **Sessão**: O tenant selecionado é salvo na sessão do navegador.
3.  **Isolamento Completo**: O Hibernate utiliza um filtro global (`@Filter`) para garantir que um usuário só veja dados do seu próprio tenant.

---

## 🛠️ Tecnologias Utilizadas

* **Java 21**: Linguagem base.
* **Spring Boot 3.5.5**: Framework principal.
* **Spring Data JPA**: Persistência.
* **H2 Database**: Banco em memória.
* **Thymeleaf + Bootstrap 5**: Frontend.
* **Maven**: Gestão de dependências.

---

## ⚙️ Como Executar

### Pré-requisitos
* Java 21 instalado.

### Passos
1.  Clone o repositório.
2.  Na raiz do projeto, execute via terminal:
    * **Linux/Mac:** `./mvnw spring-boot:run`
    * **Windows:** `./mvnw.cmd spring-boot:run`
3.  Acesse: `http://localhost:8080`

### 🧪 Executando Testes Unitários
O projeto inclui testes automatizados para validar os Design Patterns.
```bash
./mvnw test
```

---

## 🔌 Endpoints da API

Para integrações externas (Postman/Insomnia), é obrigatório enviar o header `x-tenant`.

| Verbo | Endpoint | Descrição |
| :--- | :--- | :--- |
| `POST` | `/userService/users` | Cria usuário (JSON body) |
| `GET` | `/userService/users` | Lista usuários do tenant |
| `PUT` | `/userService/users/{id}` | Atualiza usuário |
| `DELETE` | `/userService/users/{id}` | Remove usuário |

---

## 📂 Estrutura de Pastas Relevante

```text
src/main/java/br/com/project/userService
├── adapter      # Padrão Adapter (AuditService)
├── controller   # Controladores Web e API
├── factory      # Padrão Factory (UserFactory)
├── service      # Regras de Negócio
├── strategy     # Padrão Strategy (PasswordStrategy)
└── tenant       # Filtros e Resolver de Multi-tenancy
```

**Projeto desenvolvido por Leonardo Del Nero para a disciplina de Técnicas de Programação II.**