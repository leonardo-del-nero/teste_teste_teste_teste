# Projeto userService

Este é um projeto Spring Boot que expõe uma API REST para gerenciamento de usuários (CRUD). O projeto demonstra o uso de Spring Data JPA, Spring Security, validação customizada e uma arquitetura multi-tenant.

## Principais Características

* **Spring Boot 3.5.5**: Framework base da aplicação.
* **API REST**: Endpoints para operações CRUD (Criar, Ler, Atualizar, Deletar) de usuários.
* **Spring Data JPA**: Para persistência de dados com o Hibernate.
* **Banco de Dados H2**: Banco de dados em memória para desenvolvimento e testes.
* **Spring Security**: Para configuração de segurança e hashing de senhas (usando BCrypt).
* **Multi-Tenancy**: Arquitetura multi-tenant baseada em coluna (discriminator) com o header `x-tenant`.
* **Validação**: Validação de DTOs (`jakarta.validation`) e validações customizadas para criação e atualização de usuários.
* **Dozer**: Mapeamento de objetos entre DTOs (`UserDTO`) e Entidades (`UserEntity`).
* **Gerenciamento de Exceções**: Handler de exceções centralizado (`@ControllerAdvice`) para respostas de erro consistentes.
* **Spring Boot Actuator**: Endpoints de monitoramento e gerenciamento da aplicação.

## Requisitos

* Java 25 (Conforme especificado no `pom.xml`)
* Apache Maven 3.9.11+ (O projeto inclui o Maven Wrapper para facilitar a build)

## Como Executar

1.  Clone o repositório (ou tenha os arquivos do projeto).
2.  Navegue até o diretório raiz do projeto.
3.  Execute o projeto usando o Maven Wrapper:

    *No Linux/macOS:*
    ```bash
    ./mvnw spring-boot:run
    ```

    *No Windows:*
    ```bash
    ./mvnw.cmd spring-boot:run
    ```

4.  O servidor será iniciado na porta `8080`.

## Configuração

O arquivo `application.yml` define as configurações principais:

* **Porta do Servidor**: `8080`
* **Banco de Dados H2**:
    * O console H2 está habilitado e acessível em: `http://localhost:8080/h2-console`
    * **JDBC URL**: `jdbc:h2:mem:userServicedb`
    * **Usuário**: `sa`
    * **Senha**: (em branco)
* **JPA**: O `ddl-auto` está configurado como `update`, e o SQL é logado no console.
* **Flyway**: Está incluído no `pom.xml` mas desabilitado no `application.yml` (`enabled: false`).
* **Actuator**: Todos os endpoints do Actuator estão expostos (`management.endpoints.web.exposure.include: '*'`).

## Arquitetura Multi-Tenant

O projeto implementa uma estratégia de multi-tenancy baseada em "Shared Database, Shared Schema, Discriminator Column".

* A entidade `UserEntity` possui um campo `tenant` anotado com `@TenantId`.
* A classe `TenantFilter` intercepta todas as requisições para extrair o header `x-tenant`.
    * Se o header `x-tenant` estiver ausente, a requisição é rejeitada com um erro `400 Bad Request`.
* O `TenantIdentifierResolver` armazena o ID do tenant atual e o fornece ao Hibernate para filtrar as consultas.
* A tabela `USERS` possui uma constraint única (`UK_USR_001`) combinando `username` e `TENANT`, garantindo que o nome de usuário seja único *por tenant*.

**Importante**: Todas as requisições para a API devem incluir o header `x-tenant` para identificar o tenant.

## Endpoints da API

A URL base para os endpoints de usuário é `/userService/users`.

| Método | Endpoint | Descrição | Validação (Grupo) |
| :--- | :--- | :--- | :--- |
| `POST` | `/` | Cria um novo usuário. A senha é armazenada com hash BCrypt. | `View.Create` |
| `GET` | `/` | Lista todos os usuários (filtrados pelo tenant atual). | - |
| `GET` | `/{id}` | Busca um usuário por ID (do tenant atual). | - |
| `PUT` | `/{id}` | Atualiza o `username` e `roles` de um usuário existente. | `View.Update` |
| `DELETE` | `/{id}` | Remove um usuário por ID. | - |

---
### Validação

* **`UserDTO`**: Usa `jakarta.validation` (`@NotBlank`, `@NotEmpty`) para campos básicos.
* **`@UserCreate` (`UserCreateImpl.java`)**: Validação customizada ativada no grupo `View.Create`. Verifica se o `username` já existe no banco (case-insensitive) antes de criar.
* **`@UserUpdate` (`UserUpdateImpl.java`)**: Validação customizada ativada no grupo `View.Update`. Verifica se o `username` já existe, excluindo o ID do próprio usuário que está sendo atualizado.

### Segurança

A segurança é configurada em `SecurityConfig.java`:

* **CSRF**: Desabilitado (`csrf.disable()`).
* **Autorização**: Todas as requisições são permitidas (`.anyRequest().permitAll()`). (Nota: Em um ambiente de produção, isso deve ser restrito).
* **PasswordEncoder**: Um bean `BCryptPasswordEncoder` é fornecido e injetado no `UserService` para codificar as senhas dos usuários no momento da criação.

## Estrutura do Projeto (Pacotes Principais)

* `br.com.project.userService`
    * `common`: Classes utilitárias (Records `ExceptionMessage`, `FieldMessage`, e interface `View`).
    * `config`: Configurações do Spring (Dozer, Security).
    * `controller`: Controladores REST (`UserController`) e gerenciamento de exceções (`CustomExceptionHandle`).
    * `domain`: Entidades JPA (`UserEntity`).
    * `dto`: Data Transfer Objects (`UserDTO`).
    * `exception`: Exceções customizadas (`RecordNotFoundException`).
    * `repository`: Repositórios Spring Data JPA (`UserRepository`).
    * `service`: Lógica de negócios (`UserService`).
    * `tenant`: Classes de implementação da multi-tenancy (`TenantFilter`, `TenantIdentifierResolver`).
    * `validation`: Anotações e implementações de validação customizada.