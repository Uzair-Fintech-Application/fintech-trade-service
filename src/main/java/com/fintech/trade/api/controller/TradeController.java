package com.fintech.trade.api.controller;

import com.fintech.trade.dto.*;
import com.fintech.trade.model.AuthenticatedUser;
import com.fintech.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Tag(name = "P2P Trades", description = "P2P exchange with escrow")
public class TradeController {

    private final TradeService tradeService;
    private final EntityMapper mapper;

    @PostMapping
    @Operation(summary = "Create a trade offer")
    public ResponseEntity<TradeResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody CreateTradeRequest req) {
        if ("ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Access Denied: System Administrators are strictly prohibited from creating or accepting P2P trades. This action is restricted to standard users to maintain market integrity.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toTradeResponse(tradeService.createTrade(user.getId(), req)));
    }

    @GetMapping("/oracle/rates")
    @Operation(summary = "Get live market rates from oracle API")
    public ResponseEntity<MarketOracleResponse> getOracleRates(@RequestParam String base) {
        return ResponseEntity.ok(tradeService.getOracleRates(base));
    }

    @GetMapping
    @Operation(summary = "Browse trades")
    public ResponseEntity<Page<TradeResponse>> all(@AuthenticationPrincipal AuthenticatedUser user, 
                                                   @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable p) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        if (isAdmin) {
            return ResponseEntity.ok(tradeService.getAllTrades(p).map(mapper::toTradeResponse));
        } else {
            return ResponseEntity.ok(tradeService.getOpenTrades(p).map(mapper::toTradeResponse));
        }
    }

    @GetMapping("/my")
    @Operation(summary = "My trades")
    public ResponseEntity<Page<TradeResponse>> mine(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @PageableDefault(size = 20) Pageable p) {
        return ResponseEntity.ok(tradeService.getUserTrades(user.getId(), p).map(mapper::toTradeResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trade details")
    public ResponseEntity<TradeResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(mapper.toTradeResponse(tradeService.findTrade(id)));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept trade")
    public ResponseEntity<TradeResponse> accept(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Integer id) {
        if ("ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Access Denied: System Administrators are strictly prohibited from creating or accepting P2P trades. This action is restricted to standard users to maintain market integrity.");
        }
        return ResponseEntity.ok(mapper.toTradeResponse(tradeService.acceptTrade(user.getId(), id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an open trade")
    public ResponseEntity<TradeResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Integer id,
                                                 @Valid @RequestBody UpdateTradeRequest req) {
        return ResponseEntity.ok(mapper.toTradeResponse(tradeService.updateTrade(user.getId(), id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an open trade entirely")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable Integer id) {
        tradeService.deleteTrade(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

