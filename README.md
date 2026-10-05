# Coupon API

API REST para cadastro, consulta e remoção (soft delete) de cupons.
Java 17, Spring Boot 3, Spring Data JPA, H2 em memória, Swagger (springdoc) e Docker.

## Como executar

Local:

```bash
mvn spring-boot:run
```

Docker Compose:

```bash
docker compose up --build
```

- API: http://localhost:8080/coupon
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI (JSON): http://localhost:8080/v3/api-docs
- Contrato do desafio: http://localhost:8080/openapi.yaml
- Console H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:coupondb`, user `sa`, sem senha)

Guia com exemplos e formato de erros: [docs/API.md](docs/API.md)

## Testes e cobertura

```bash
mvn clean verify
```

O relatório JaCoCo fica em `target/site/jacoco/index.html`.

## Endpoints

| Método | Rota           | Sucesso | Erros                                       |
|--------|----------------|---------|---------------------------------------------|
| POST   | `/coupon`      | 201     | 400 (dados inválidos / regra de negócio)    |
| GET    | `/coupon/{id}` | 200     | 400 (id inválido), 404                      |
| DELETE | `/coupon/{id}` | 204     | 400 (id inválido), 404, 409 (já deletado)   |

```bash
curl -X POST http://localhost:8080/coupon \
  -H "Content-Type: application/json" \
  -d '{"code":"ABC-123","description":"10% off","discountValue":0.8,"expirationDate":"2030-11-04T17:14:45.180Z","published":false}'
```

## Regras de negócio

- `code`, `description`, `discountValue` e `expirationDate` são obrigatórios.
- Caracteres especiais do `code` são removidos antes de salvar e na resposta; o resultado deve ter exatamente 6 caracteres alfanuméricos.
- `discountValue` mínimo de 0.5, sem máximo.
- `expirationDate` nunca pode estar no passado.
- Um cupom pode ser criado já publicado (`published: true`).
- Delete é soft delete (`status = DELETED` + `deletedAt`), preservando os dados recebidos no cadastro.
- Não é possível deletar um cupom já deletado (409), nem sob requisições simultâneas.

## Arquitetura (hexagonal)

```
domain          -> Coupon, CouponCode, DiscountValue: regras de negócio em Java puro
application     -> use cases (um por intenção, método único `execute`) e portas (interfaces)
infrastructure  -> adaptadores: REST (Spring Web), persistência (JPA/H2) e configuração
```

- A dependência aponta sempre para dentro: `infrastructure -> application -> domain`.
- `application` e `domain` não importam Spring nem JPA. Isso é garantido pelo `ArchitectureTest`.
- Os use cases só orquestram (`CreateCouponUseCase`, `DeleteCouponUseCase`, `FindCouponUseCase`); as regras vivem nas entidades e value objects do domínio.
- A `application` depende apenas de interfaces: `CouponRepository` (persistência) e `CurrentTime` (hora atual).
- A persistência usa um modelo JPA separado (`CouponJpaEntity`) e converte de/para o domínio.
- `Coupon.create` aplica as regras de criação; `Coupon.restore` reconstitui cupons já salvos sem revalidar a expiração (um cupom válido na criação pode expirar depois).

## Testes

- **Domínio:** regras isoladas (código, desconto, expiração, soft delete).
- **Use cases:** com repositório em memória (fake), verificando o que fica armazenado.
- **Persistência:** adaptador contra H2 real (o soft delete preserva os dados).
- **API:** ponta a ponta tentando violar as regras (código inválido, desconto baixo, data passada, deletar duas vezes).
- **Arquitetura:** ArchUnit impede imports proibidos nas camadas internas e garante um único método público por use case.

## Decisões de projeto

O contrato do desafio define apenas os códigos de sucesso (201, 200, 204). Foram decisões de projeto:

- Regra de negócio violada retorna 400; cupom inexistente 404; cupom já deletado 409.
- `GET` de um cupom deletado retorna o cupom com `status: DELETED` (a informação não é perdida).
- Erros no formato RFC 7807 (`ProblemDetail`).
- Concorrência: a entidade JPA tem `@Version` (lock otimista). O domínio apenas carrega a versão lida; se dois deletes simultâneos tentarem gravar, o segundo falha e a API devolve 409.
- O enum `status` mantém `INACTIVE` por compatibilidade com o contrato, embora as regras do desafio não o usem.

## Verificação rápida com a API no ar

Com a API rodando (local ou via Docker), execute `scripts/smoke.sh` (requer `curl` e `jq`).
O script cria, consulta e deleta cupons e tenta violar as regras de negócio, conferindo os status HTTP.
