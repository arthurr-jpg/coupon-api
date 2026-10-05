package com.arthur.coupon.infrastructure.web;

import com.arthur.coupon.application.usecase.CreateCouponCommand;
import com.arthur.coupon.application.usecase.CreateCouponUseCase;
import com.arthur.coupon.application.usecase.DeleteCouponUseCase;
import com.arthur.coupon.application.usecase.FindCouponUseCase;
import com.arthur.coupon.domain.Coupon;
import com.arthur.coupon.infrastructure.web.dto.CouponRequest;
import com.arthur.coupon.infrastructure.web.dto.CouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/coupon")
@Tag(name = "coupon", description = "Cadastro, consulta e remocao de cupons")
public class CouponController {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String ID_EXAMPLE = "cef9d1e3-aae5-4ab6-a297-358c6032b1e7";

    private final CreateCouponUseCase createCoupon;
    private final FindCouponUseCase findCoupon;
    private final DeleteCouponUseCase deleteCoupon;

    public CouponController(CreateCouponUseCase createCoupon,
                            FindCouponUseCase findCoupon,
                            DeleteCouponUseCase deleteCoupon) {
        this.createCoupon = createCoupon;
        this.findCoupon = findCoupon;
        this.deleteCoupon = deleteCoupon;
    }

    @PostMapping
    @Operation(summary = "Cria um cupom",
            description = "Remove caracteres especiais do codigo antes de salvar. O cupom pode ser criado ja publicado.")
    @ApiResponse(responseCode = "201", description = "Cupom criado. O header Location aponta para o recurso.")
    @ApiResponse(responseCode = "400",
            description = "Dados invalidos ou regra de negocio violada "
                    + "(codigo fora de 6 caracteres, desconto < 0.5, data no passado, campo obrigatorio ausente)",
            content = @Content(mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Bad Request\","
                            + "\"status\":400,\"detail\":\"Coupon code must have exactly 6 alphanumeric characters "
                            + "after removing special characters\",\"instance\":\"/coupon\"}")))
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        Coupon coupon = createCoupon.execute(new CreateCouponCommand(
                request.code(),
                request.description(),
                request.discountValue(),
                request.expirationDate(),
                request.published()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(coupon.getId())
                .toUri();

        return ResponseEntity.created(location).body(CouponResponse.from(coupon));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cupom pelo id",
            description = "Cupons deletados continuam consultaveis e retornam status DELETED.")
    @ApiResponse(responseCode = "200", description = "Cupom encontrado")
    @ApiResponse(responseCode = "400", description = "Id em formato invalido (nao e UUID)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cupom nao encontrado",
            content = @Content(mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Not Found\","
                            + "\"status\":404,\"detail\":\"Coupon not found: " + ID_EXAMPLE + "\","
                            + "\"instance\":\"/coupon/" + ID_EXAMPLE + "\"}")))
    public CouponResponse findById(
            @Parameter(description = "Id do cupom (UUID)", example = ID_EXAMPLE)
            @PathVariable UUID id) {
        return CouponResponse.from(findCoupon.execute(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um cupom (soft delete)",
            description = "Marca o cupom como DELETED e registra deletedAt, preservando os dados do cadastro. "
                    + "Nao e possivel deletar um cupom ja deletado.")
    @ApiResponse(responseCode = "204", description = "Cupom deletado")
    @ApiResponse(responseCode = "400", description = "Id em formato invalido (nao e UUID)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cupom nao encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "Cupom ja deletado (ou alterado por outra requisicao ao mesmo tempo)",
            content = @Content(mediaType = PROBLEM_JSON,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = "{\"type\":\"about:blank\",\"title\":\"Conflict\","
                            + "\"status\":409,\"detail\":\"Coupon already deleted: " + ID_EXAMPLE + "\","
                            + "\"instance\":\"/coupon/" + ID_EXAMPLE + "\"}")))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id do cupom (UUID)", example = ID_EXAMPLE)
            @PathVariable UUID id) {
        deleteCoupon.execute(id);
        return ResponseEntity.noContent().build();
    }
}
