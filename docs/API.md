# Coupon API: documentação

Documentação interativa (Swagger UI): http://localhost:8080/swagger-ui.html
Especificação gerada pelo código: http://localhost:8080/v3/api-docs (JSON)
Contrato do desafio versionado: [`src/main/resources/static/openapi.yaml`](../src/main/resources/static/openapi.yaml)
(servido em http://localhost:8080/openapi.yaml)

Base URL: `http://localhost:8080`

## POST /coupon

Cria um cupom.

| Campo          | Tipo            | Obrigatório | Regra                                                        |
|----------------|-----------------|-------------|--------------------------------------------------------------|
| code           | string          | sim         | Caracteres especiais removidos; devem restar 6 alfanuméricos |
| description    | string          | sim         | Não pode ser vazio                                           |
| discountValue  | number          | sim         | Mínimo 0.5, sem máximo                                       |
| expirationDate | string ISO-8601 | sim         | Não pode estar no passado                                    |
| published      | boolean         | não         | Padrão `false`; pode ser criado já publicado                 |

```bash
curl -X POST http://localhost:8080/coupon \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ABC-123",
    "description": "10% de desconto na primeira compra",
    "discountValue": 0.8,
    "expirationDate": "2030-11-04T17:14:45.180Z",
    "published": false
  }'
```

**201 Created** (header `Location: /coupon/{id}`)

```json
{
  "id": "cef9d1e3-aae5-4ab6-a297-358c6032b1e7",
  "code": "ABC123",
  "description": "10% de desconto na primeira compra",
  "discountValue": 0.8,
  "expirationDate": "2030-11-04T17:14:45.180Z",
  "status": "ACTIVE",
  "published": false,
  "redeemed": false
}
```

Erros: `400` (dados inválidos ou regra de negócio violada).

## GET /coupon/{id}

Busca um cupom pelo id. Cupons deletados continuam acessíveis, com `status: "DELETED"`.

```bash
curl http://localhost:8080/coupon/cef9d1e3-aae5-4ab6-a297-358c6032b1e7
```

**200 OK**: mesmo corpo do POST. Erros: `400` (id não é UUID), `404` (não encontrado).

## DELETE /coupon/{id}

Soft delete: marca o cupom como `DELETED` e preserva os dados do cadastro.

```bash
curl -X DELETE http://localhost:8080/coupon/cef9d1e3-aae5-4ab6-a297-358c6032b1e7
```

**204 No Content**. Erros: `400` (id não é UUID), `404` (não encontrado), `409` (já deletado, ou alterado por outra requisição ao mesmo tempo).

## Formato de erro (RFC 7807)

Content-Type `application/problem+json`.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Coupon code must have exactly 6 alphanumeric characters after removing special characters",
  "instance": "/coupon"
}
```

Erros de validação de campos obrigatórios trazem também o mapa `errors`:

```json
{
  "status": 400,
  "detail": "Validation failed",
  "errors": {
    "code": "code is required",
    "discountValue": "discountValue is required"
  }
}
```

## Resumo de status

| Situação                        | Status |
|---------------------------------|--------|
| Cupom criado                    | 201    |
| Cupom encontrado                | 200    |
| Cupom deletado                  | 204    |
| Dados inválidos / regra violada | 400    |
| Cupom não encontrado            | 404    |
| Cupom já deletado               | 409    |

> O contrato do desafio define apenas os códigos de sucesso (201, 200, 204). Os códigos de erro
> acima são decisões de projeto.
