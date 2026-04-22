package org.openfamilycompass.api.v1;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.openfamilycompass.api.v1.dto.PointDto;
import org.openfamilycompass.model.PointTransaction;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.PointService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Points", description = "Points and transactions")
public class PointApiController {

    private final PointService pointService;
    private final UserService userService;

    @GetMapping("/balance")
    @Operation(summary = "Get current user's point balance")
    public ResponseEntity<PointDto.BalanceResponse> getPointBalance(@AuthenticationPrincipal Jwt jwt) {
        User currentUser = getCurrentUser(jwt);
        return ResponseEntity.ok(PointDto.BalanceResponse.builder()
                .userId(currentUser.getId())
                .totalPoints(currentUser.getTotalPoints())
                .build());
    }

    @GetMapping("/balance/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Get point balance for a user")
    public ResponseEntity<PointDto.BalanceResponse> getPointBalanceForUser(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(PointDto.BalanceResponse.builder()
                .userId(user.getId())
                .totalPoints(user.getTotalPoints())
                .build());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get point transactions")
    public ResponseEntity<PointDto.TransactionsResponse> getPointTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) PointTransactionType type,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        User currentUser = getCurrentUser(jwt);
        User targetUser;

        if (currentUser.getRole() == UserRole.CHILD) {
            targetUser = currentUser;
        } else if (userId != null) {
            targetUser = userService.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        } else {
            targetUser = null;
        }

        List<PointTransaction> transactions;
        if (targetUser != null) {
            transactions = pointService.findByUser(targetUser);
        } else {
            transactions = pointService.findAll();
        }

        if (type != null) {
            transactions = transactions.stream()
                    .filter(t -> t.getType() == type)
                    .collect(Collectors.toList());
        }
        if (from != null) {
            transactions = transactions.stream()
                    .filter(t -> !t.getCreatedAt().isBefore(from))
                    .collect(Collectors.toList());
        }
        if (to != null) {
            transactions = transactions.stream()
                    .filter(t -> !t.getCreatedAt().isAfter(to))
                    .collect(Collectors.toList());
        }

        int total = transactions.size();
        transactions = transactions.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        // Calculate running balance. Transactions are sorted newest-first, so we
        // start from the user's current balance and walk backwards. Only rows that
        // actually affect the balance (PointTransaction#affectsBalance) shift it.
        int balance = targetUser != null ? targetUser.getTotalPoints() : 0;
        List<PointDto.TransactionResponse> responseList = new java.util.ArrayList<>();
        for (PointTransaction tx : transactions) {
            responseList.add(PointDto.TransactionResponse.fromEntityWithBalance(tx, balance));
            if (tx.affectsBalance()) {
                balance -= tx.getPoints();
            }
        }

        return ResponseEntity.ok(PointDto.TransactionsResponse.builder()
                .transactions(responseList)
                .total(total)
                .limit(limit)
                .offset(offset)
                .build());
    }

    @PostMapping("/bonus")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Award bonus points")
    public ResponseEntity<PointDto.TransactionResponse> awardBonusPoints(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PointDto.AwardPointsRequest request) {

        User currentUser = getCurrentUser(jwt);
        User targetUser = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        PointTransaction transaction = pointService.awardPoints(
                targetUser,
                request.getPoints(),
                PointTransactionType.BONUS,
                request.getDescription(),
                null,
                currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PointDto.TransactionResponse.fromEntityWithBalance(transaction, targetUser.getTotalPoints()));
    }

    @PostMapping("/penalty")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Deduct penalty points")
    public ResponseEntity<PointDto.TransactionResponse> deductPenaltyPoints(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PointDto.AwardPointsRequest request) {

        User currentUser = getCurrentUser(jwt);
        User targetUser = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        PointTransaction transaction = pointService.deductPoints(
                targetUser,
                request.getPoints(),
                PointTransactionType.PENALTY,
                request.getDescription(),
                null,
                currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PointDto.TransactionResponse.fromEntityWithBalance(transaction, targetUser.getTotalPoints()));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Add points to a user (bonus or penalty)")
    public ResponseEntity<PointDto.TransactionResponse> addPoints(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PointDto.AddPointsRequest request) {
        User currentUser = getCurrentUser(jwt);
        User targetUser = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        PointTransaction transaction = pointService.addPointsWithRemarks(
                targetUser,
                request.getPoints(),
                request.getType(),
                request.getDescription(),
                null,
                null,
                currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PointDto.TransactionResponse.fromEntityWithBalance(transaction, targetUser.getTotalPoints()));
    }

    private User getCurrentUser(Jwt jwt) {
        String username = jwt.getSubject();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
