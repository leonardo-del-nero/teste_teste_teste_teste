# User Service - Sistema Multi-tenant com Design Patterns

Este projeto é uma aplicação **Spring Boot** desenvolvida para a disciplina **ILP037 - Técnicas de Programação II**. O objetivo principal é demonstrar a refatoração de um sistema legado (CRUD simples) para uma arquitetura robusta utilizando **Design Patterns (GoF)**, respeitando princípios SOLID e implementando uma arquitetura **Multi-tenant**.

---

## 1. Contextualização e Problema

### O Problema
Sistemas corporativos modernos frequentemente precisam atender múltiplos clientes (organizações) utilizando uma única instância da aplicação, garantindo que os dados de um cliente nunca sejam acessados por outro. Além disso, regras de negócio como "validação de senha" e "auditoria" tendem a mudar com frequência, tornando códigos acoplados difíceis de manter.

### A Solução
O **User Service** resolve estes problemas através de:
1.  **Isolamento de Dados (Multi-tenancy):** Utilização de uma coluna discriminadora (`tenant_id`) gerenciada automaticamente via Hibernate Filters e Aspectos, garantindo segurança a nível de banco de dados.
2.  **Desacoplamento:** Uso de Design Patterns para isolar regras de validação, criação de objetos e persistência de logs.

---

## 2. Arquitetura do Sistema

O sistema segue uma arquitetura em camadas (Layered Architecture) clássica do Spring Boot, mas enriquecida com camadas de abstração para os padrões de projeto.

* **Camada Web:** Controllers REST e Thymeleaf.
* **Camada de Aplicação:** Services que orquestram o fluxo.
* **Camada de Domínio:** Entidades e Interfaces de Padrões.
* **Camada de Infraestrutura:** Implementações concretas (Repositórios, Adapters).

> **Nota sobre C4 Model:** Os diagramas de contexto e container (Nível 1 e 2) encontram-se na pasta `/docs` do repositório.

---

## 3. Design Patterns Aplicados (Refatoração)

Abaixo detalhamos a aplicação dos 3 padrões de projeto exigidos, comparando a versão original (Legado/v1) com a versão refatorada.

### 3.1. Strategy (Comportamental) - Validação de Senha

* **Contexto:** A validação de complexidade de senha pode variar (ex: exigir caracteres especiais, tamanho mínimo, ou permitir senhas simples em dev).
* **Problema na v1:** A validação ficaria *hardcoded* no `UserService` ou anotações rígidas no DTO, violando o princípio Aberto/Fechado (OCP).
* **Solução (Pattern):** Criar uma interface `PasswordStrategy` que define o contrato de validação.

#### Antes (Acoplado)
A validação e o encode eram feitos diretamente no fluxo do serviço ou via anotações que não permitem lógica complexa dinâmica.

#### Depois (Refatorado)
O `UserService` desconhece a regra exata. Ele apenas chama `.validate()`.

```java
// Interface
public interface PasswordStrategy {
    void validate(String password);
}

// Implementação Concreta (Strategy)
@Component
@Primary
public class StrongPasswordStrategy implements PasswordStrategy {
    public void validate(String password) {
        if (password.length() < 8) throw new IllegalArgumentException("Senha muito curta");
        // ... outras regras
    }
}
```

### 3.2. Factory (Criacional) - Criação de Usuário

* **Contexto:** A criação de uma entidade `UserEntity` a partir de um DTO envolve regras específicas, como a criptografia da senha (Hash).
* **Problema na v1:** O `UserService` utilizava o DozerMapper (biblioteca externa) e injetava o `PasswordEncoder` diretamente. Isso espalhava a lógica de hash e dependências de mapeamento pelo Service.
* **Solução (Pattern):** Centralizar a criação e o encapsulamento da entidade em uma `UserFactory`.

#### Trecho de Código Relevante

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

**Justificativa:** Remove a responsabilidade de "saber como criar um usuário válido" do Service. Se mudarmos a biblioteca de hash ou de mapeamento, o Service não é afetado.

### 3.3. Adapter (Estrutural) - Sistema de Auditoria

* **Contexto:** O sistema precisa registrar operações críticas (criação/exclusão de usuários).
* **Problema na v1:** Não existia auditoria, ou seria implementada com `System.out.println` ou dependência direta de bibliotecas de Log/Arquivo dentro do Service.
* **Solução (Pattern):** Definir uma interface de domínio `AuditService` e criar um Adaptador para escrever em arquivo físico (`FileAuditAdapter`).

#### Implementação

O Service depende apenas da abstração:

```java
// No UserService
private final AuditService auditService;

public void delete(long id) {
    // ... deleta ...
    auditService.log("DELETE", "Usuario removido ID: " + id);
}
```

O Adaptador lida com a complexidade de I/O (Java IO, FileWriters, Try-with-resources):

```java
@Component
public class FileAuditAdapter implements AuditService {
    public void log(String operacao, String detalhes) {
        try (FileWriter fw = new FileWriter("audit.log", true)) {
            // Escrita em baixo nível
        }
    }
}
```

**Justificativa:** Permite trocar a tecnologia de log (para Banco de Dados, API Externa, Console) sem tocar em uma linha de código do UserService.

---

## 4. Tecnologias Utilizadas

* **Java 21:** Linguagem base.
* **Spring Boot 3.5.5:** Framework.
* **Spring Data JPA:** Persistência.
* **H2 Database:** Banco em memória.
* **Thymeleaf:** Engine de templates para o Frontend.
* **Maven:** Gerenciamento de dependências.

---

## 5. Como Executar

1.  Certifique-se de ter o **Java 21** instalado.
2.  Clone o repositório.
3.  Execute o comando:
    * Linux/Mac: `./mvnw spring-boot:run`
    * Windows: `./mvnw.cmd spring-boot:run`
4.  Acesse: `http://localhost:8080`

---

## 6. Endpoints da API

Para testar via Postman/Insomnia, utilize o header `x-tenant`.

| Verbo | Endpoint | Descrição |
| :--- | :--- | :--- |
| **POST** | `/userService/users` | Cria usuário (JSON body) |
| **GET** | `/userService/users` | Lista usuários do tenant |
| **PUT** | `/userService/users/{id}` | Atualiza usuário |

---

**Projeto desenvolvido por Leonardo Del Nero para a disciplina de Técnicas de Programação II.**