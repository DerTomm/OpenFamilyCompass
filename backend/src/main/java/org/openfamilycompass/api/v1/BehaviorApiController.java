package org.openfamilycompass.api.v1;

import java.util.List;
import java.util.stream.Collectors;

import org.openfamilycompass.api.v1.dto.BehaviorDto;
import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;
import org.openfamilycompass.model.User;
import org.openfamilycompass.service.BehaviorEvaluationService;
import org.openfamilycompass.service.BehaviorService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RequestMapping("/api/v1/behaviors")
@RequiredArgsConstructor
@Tag(name = "Behaviors", description = "Behavior rules and evaluations")
public class BehaviorApiController {

    private final BehaviorService behaviorService;
    private final BehaviorEvaluationService evaluationService;
    private final UserService userService;

    // ============ BEHAVIORS ============

    @GetMapping
    @Operation(summary = "List behaviors")
    public ResponseEntity<List<BehaviorDto.Response>> listBehaviors(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean active) {

        List<Behavior> behaviors;
        if (userId != null) {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            behaviors = behaviorService.findByUserOrGlobal(user);
        } else {
            behaviors = behaviorService.findAll();
        }

        if (active != null) {
            behaviors = behaviors.stream()
                    .filter(b -> b.isActive() == active)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                behaviors.stream()
                        .map(BehaviorDto.Response::fromEntity)
                        .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Create a new behavior")
    public ResponseEntity<BehaviorDto.Response> createBehavior(
            @Valid @RequestBody BehaviorDto.CreateRequest request) {

        Behavior behavior = new Behavior();
        behavior.setTitle(request.getTitle());
        behavior.setGuideline(request.getGuideline());
        behavior.setPoints(request.getPlusPoints());
        behavior.setMinusPoints(request.getMinusPoints());
        behavior.setActive(true);

        if (behavior.getPoints() == 0 && behavior.getMinusPoints() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "plusPoints and minusPoints must not both be 0");
        }

        if (request.getUserId() != null) {
            User user = userService.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            behavior.setUser(user);
        }

        if (request.getRank() != null) {
            behavior.setRank(request.getRank());
        }

        Behavior saved = behaviorService.save(behavior);
        return ResponseEntity.status(HttpStatus.CREATED).body(BehaviorDto.Response.fromEntity(saved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get behavior by ID")
    public ResponseEntity<BehaviorDto.Response> getBehaviorById(@PathVariable Long id) {
        Behavior behavior = behaviorService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavior not found"));
        return ResponseEntity.ok(BehaviorDto.Response.fromEntity(behavior));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Update behavior")
    public ResponseEntity<BehaviorDto.Response> updateBehavior(
            @PathVariable Long id,
            @Valid @RequestBody BehaviorDto.UpdateRequest request) {

        Behavior behavior = behaviorService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavior not found"));

        if (request.getTitle() != null) {
            behavior.setTitle(request.getTitle());
        }
        if (request.getGuideline() != null) {
            behavior.setGuideline(request.getGuideline());
        }
        int oldPlus = behavior.getPoints();
        int oldMinus = behavior.getMinusPoints();

        if (request.getPlusPoints() != null) {
            behavior.setPoints(request.getPlusPoints());
        }

        if (request.getMinusPoints() != null) {
            behavior.setMinusPoints(request.getMinusPoints());
        }
        if (request.getRank() != null) {
            behavior.setRank(request.getRank());
        }
        if (request.getActive() != null) {
            behavior.setActive(request.getActive());
        }

        if (behavior.getPoints() == 0 && behavior.getMinusPoints() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "plusPoints and minusPoints must not both be 0");
        }

        // If range shrinks, cap existing uncommitted evaluations
        if (behavior.getPoints() < oldPlus || behavior.getMinusPoints() < oldMinus) {
            behaviorService.capPointsInCurrentWeek(behavior, behavior.getPoints(), behavior.getMinusPoints());
        }

        Behavior saved = behaviorService.save(behavior);
        return ResponseEntity.ok(BehaviorDto.Response.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Deactivate behavior")
    public ResponseEntity<Void> deactivateBehavior(@PathVariable Long id) {
        Behavior behavior = behaviorService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Behavior not found"));

        behavior.setActive(false);
        behaviorService.save(behavior);

        return ResponseEntity.noContent().build();
    }

    // ============ EVALUATIONS ============

    @GetMapping("/evaluations")
    @Operation(summary = "List behavior evaluations")
    public ResponseEntity<List<BehaviorDto.EvaluationResponse>> listEvaluations(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean committed) {

        List<BehaviorEvaluation> evaluations;
        if (userId != null) {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            evaluations = evaluationService.findByUser(user);
        } else {
            evaluations = evaluationService.findAll();
        }

        if (committed != null) {
            evaluations = evaluations.stream()
                    .filter(e -> e.isCommitted() == committed)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                evaluations.stream()
                        .map(BehaviorDto.EvaluationResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Create or update behavior evaluation")
    public ResponseEntity<BehaviorDto.EvaluationResponse> saveEvaluation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BehaviorDto.SaveEvaluationRequest request) {

        User currentUser = getCurrentUser(jwt);
        behaviorService.findById(request.getBehaviorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Behavior not found"));
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        BehaviorEvaluation saved = evaluationService.updateEvaluation(
                request.getBehaviorId(),
                user,
                request.getCurrentPoints(),
                request.getRemarks(),
                currentUser);
        return ResponseEntity.ok(BehaviorDto.EvaluationResponse.fromEntity(saved));
    }

    @PostMapping("/evaluations/commit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Commit all pending evaluations for a user")
    public ResponseEntity<BehaviorDto.CommitEvaluationsResponse> commitEvaluations(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BehaviorDto.CommitEvaluationsRequest request) {

        User currentUser = getCurrentUser(jwt);
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        // Get current evaluations count before commit
        List<BehaviorEvaluation> pendingEvals = evaluationService.getCurrentWeekEvaluations(user);
        int totalPoints = pendingEvals.stream().mapToInt(BehaviorEvaluation::getCurrentPoints).sum();
        int count = pendingEvals.size();

        evaluationService.commitWeeklyEvaluations(user, currentUser);

        return ResponseEntity.ok(BehaviorDto.CommitEvaluationsResponse.builder()
                .totalPointsAwarded(totalPoints)
                .evaluationsCommitted(count)
                .build());
    }

    private User getCurrentUser(Jwt jwt) {
        String username = jwt.getSubject();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
