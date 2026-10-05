#!/usr/bin/env bash
BASE=${1:-http://localhost:8080}
JSON='Content-Type: application/json'
ok=0; fail=0

check() {  # nome esperado obtido
  if [ "$2" = "$3" ]; then echo "  ok      $1 ($3)"; ok=$((ok+1))
  else echo "  FALHOU  $1: esperado $2, veio $3"; fail=$((fail+1)); fi
}
status() { curl -s -o /dev/null -w "%{http_code}" "$@"; }
post() { status -X POST "$BASE/coupon" -H "$JSON" -d "$1"; }

FUTURE='2030-11-04T17:14:45.180Z'

echo "Criacao e leitura"
resp=$(curl -s -w "\n%{http_code}" -X POST "$BASE/coupon" -H "$JSON" \
  -d "{\"code\":\"ABC-123\",\"description\":\"10% off\",\"discountValue\":0.8,\"expirationDate\":\"$FUTURE\",\"published\":false}")
code=$(echo "$resp" | tail -n1); body=$(echo "$resp" | sed '$d'); ID=$(echo "$body" | jq -r .id)
check "criar cupom" 201 "$code"
check "codigo sanitizado na resposta" ABC123 "$(echo "$body" | jq -r .code)"
check "status inicial" ACTIVE "$(echo "$body" | jq -r .status)"
check "buscar cupom" 200 "$(status "$BASE/coupon/$ID")"
check "criar ja publicado" 201 "$(post "{\"code\":\"PUB123\",\"description\":\"d\",\"discountValue\":1,\"expirationDate\":\"$FUTURE\",\"published\":true}")"

echo "Soft delete"
check "delete 1" 204 "$(status -X DELETE "$BASE/coupon/$ID")"
check "delete 2 (ja deletado)" 409 "$(status -X DELETE "$BASE/coupon/$ID")"
check "status apos delete" DELETED "$(curl -s "$BASE/coupon/$ID" | jq -r .status)"
check "dados preservados" ABC123 "$(curl -s "$BASE/coupon/$ID" | jq -r .code)"

echo "Tentativas de quebrar as regras"
check "codigo com 4 chars apos sanitizar" 400 "$(post "{\"code\":\"AB-12\",\"description\":\"d\",\"discountValue\":0.8,\"expirationDate\":\"$FUTURE\"}")"
check "codigo com 7 chars" 400 "$(post "{\"code\":\"ABCDEFG\",\"description\":\"d\",\"discountValue\":0.8,\"expirationDate\":\"$FUTURE\"}")"
check "desconto 0.49" 400 "$(post "{\"code\":\"ABC123\",\"description\":\"d\",\"discountValue\":0.49,\"expirationDate\":\"$FUTURE\"}")"
check "desconto 0.5 (minimo) aceito" 201 "$(post "{\"code\":\"MIN123\",\"description\":\"d\",\"discountValue\":0.5,\"expirationDate\":\"$FUTURE\"}")"
check "data no passado" 400 "$(post '{"code":"ABC123","description":"d","discountValue":0.8,"expirationDate":"2020-01-01T00:00:00.000Z"}')"
check "campos obrigatorios ausentes" 400 "$(post '{}')"
check "id inexistente" 404 "$(status "$BASE/coupon/00000000-0000-0000-0000-000000000000")"
check "delete de inexistente" 404 "$(status -X DELETE "$BASE/coupon/00000000-0000-0000-0000-000000000000")"
check "id que nao e UUID" 400 "$(status "$BASE/coupon/abc")"

echo "Documentacao"
check "swagger ui" 200 "$(status -L "$BASE/swagger-ui.html")"
check "openapi json" 200 "$(status "$BASE/v3/api-docs")"
check "contrato do desafio" 200 "$(status "$BASE/openapi.yaml")"

echo; echo "Resultado: $ok ok, $fail falhas"
[ "$fail" -eq 0 ]
