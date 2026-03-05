package org.openfamilycompass.api.v1;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.openfamilycompass.api.v1.dto.RewardDto;
import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.RewardRedemptionService;
import org.openfamilycompass.service.RewardService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Tag(name = "Rewards", description = "Rewards and redemptions")
public class RewardApiController {

    private final RewardService rewardService;
    private final RewardRedemptionService redemptionService;
    private final UserService userService;

    // ============ REWARDS ============

    @GetMapping
    @Operation(summary = "List all rewards")
    public ResponseEntity<List<RewardDto.Response>> listRewards(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Boolean active) {

        // If the caller is a child, only return rewards visible to them
        if (jwt != null) {
            try {
                User currentUser = getCurrentUser(jwt);
                if (currentUser.getRole() == UserRole.CHILD) {
                    return ResponseEntity.ok(
                            rewardService.findActiveForUser(currentUser).stream()
                                    .map(RewardDto.Response::fromEntity)
                                    .collect(Collectors.toList()));
                }
            } catch (Exception ignored) {
                // unauthenticated – fall through
            }
        }

        List<Reward> rewards;
        if (active != null && active) {
            rewards = rewardService.findAllActive();
        } else {
            rewards = rewardService.findAll();
        }

        return ResponseEntity.ok(
                rewards.stream()
                        .map(RewardDto.Response::fromEntity)
                        .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Create a new reward")
    public ResponseEntity<RewardDto.Response> createReward(
            @Valid @RequestBody RewardDto.CreateRequest request) {

        Reward reward = new Reward();
        reward.setTitle(request.getTitle());
        reward.setDescription(request.getDescription());
        reward.setPointsCost(request.getPointsCost());
        reward.setActive(true);

        if (request.getUserId() != null) {
            User user = userService.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            reward.setUser(user);
        }

        Reward saved = rewardService.save(reward);
        return ResponseEntity.status(HttpStatus.CREATED).body(RewardDto.Response.fromEntity(saved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reward by ID")
    public ResponseEntity<RewardDto.Response> getRewardById(@PathVariable Long id) {
        Reward reward = rewardService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reward not found"));
        return ResponseEntity.ok(RewardDto.Response.fromEntity(reward));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Update reward")
    public ResponseEntity<RewardDto.Response> updateReward(
            @PathVariable Long id,
            @Valid @RequestBody RewardDto.UpdateRequest request) {

        Reward reward = rewardService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reward not found"));

        if (request.getTitle() != null) {
            reward.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            reward.setDescription(request.getDescription());
        }
        if (request.getPointsCost() != null) {
            reward.setPointsCost(request.getPointsCost());
        }
        if (request.getActive() != null) {
            reward.setActive(request.getActive());
        }
        if (request.getUserId() != null) {
            if (request.getUserId() == 0L) {
                // 0 signals "remove restriction"
                reward.setUser(null);
            } else {
                User user = userService.findById(request.getUserId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
                reward.setUser(user);
            }
        }

        Reward saved = rewardService.save(reward);
        return ResponseEntity.ok(RewardDto.Response.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Deactivate reward")
    public ResponseEntity<Void> deactivateReward(@PathVariable Long id) {
        Reward reward = rewardService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reward not found"));

        reward.setActive(false);
        rewardService.save(reward);

        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Upload reward image")
    public ResponseEntity<RewardDto.Response> uploadRewardImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        Reward reward = rewardService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reward not found"));

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        try {
            reward.setImageData(file.getBytes());
            reward.setImageContentType(contentType);
            Reward saved = rewardService.save(reward);
            return ResponseEntity.ok(RewardDto.Response.fromEntity(saved));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image");
        }
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Get reward image")
    public ResponseEntity<byte[]> getRewardImage(@PathVariable Long id) {
        Reward reward = rewardService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reward not found"));

        if (reward.getImageData() == null || reward.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String ct = reward.getImageContentType() != null ? reward.getImageContentType() : "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header("Cache-Control", "public, max-age=86400")
                .body(reward.getImageData());
    }

    // ============ REDEMPTIONS ============

    @GetMapping("/redemptions")
    @Operation(summary = "List redemptions")
    public ResponseEntity<List<RewardDto.RedemptionResponse>> listRedemptions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) RewardStatus status) {

        User currentUser = getCurrentUser(jwt);
        List<RewardRedemption> redemptions;

        if (currentUser.getRole() == UserRole.CHILD) {
            redemptions = redemptionService.findByUser(currentUser);
        } else if (userId != null) {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            redemptions = redemptionService.findByUser(user);
        } else {
            redemptions = redemptionService.findAll();
        }

        if (status != null) {
            redemptions = redemptions.stream()
                    .filter(r -> r.getStatus() == status)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                redemptions.stream()
                        .map(RewardDto.RedemptionResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/redemptions")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Request reward redemption")
    public ResponseEntity<RewardDto.RedemptionResponse> requestRedemption(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RewardDto.RequestRedemptionRequest request) {

        User currentUser = getCurrentUser(jwt);
        Reward reward = rewardService.findById(request.getRewardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reward not found"));

        if (!reward.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reward is not available");
        }

        if (currentUser.getTotalPoints() < reward.getPointsCost()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient points");
        }

        RewardRedemption redemption = redemptionService.requestReward(currentUser, reward);
        return ResponseEntity.status(HttpStatus.CREATED).body(RewardDto.RedemptionResponse.fromEntity(redemption));
    }

    @PostMapping("/redemptions/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Approve redemption")
    public ResponseEntity<RewardDto.RedemptionResponse> approveRedemption(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        User currentUser = getCurrentUser(jwt);
        RewardRedemption redemption = redemptionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Redemption not found"));

        if (redemption.getStatus() != RewardStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redemption cannot be approved");
        }

        RewardRedemption approved = redemptionService.approveRedemption(redemption.getId(), currentUser, null);
        return ResponseEntity.ok(RewardDto.RedemptionResponse.fromEntity(approved));
    }

    @PostMapping("/redemptions/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Reject redemption")
    public ResponseEntity<RewardDto.RedemptionResponse> rejectRedemption(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody(required = false) RewardDto.RejectRedemptionRequest request) {

        User currentUser = getCurrentUser(jwt);
        RewardRedemption redemption = redemptionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Redemption not found"));

        if (redemption.getStatus() != RewardStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redemption cannot be rejected");
        }

        RewardRedemption rejected = redemptionService.cancelRedemption(redemption.getId(), currentUser);
        return ResponseEntity.ok(RewardDto.RedemptionResponse.fromEntity(rejected));
    }

    @PostMapping("/redemptions/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Mark redemption as delivered")
    public ResponseEntity<RewardDto.RedemptionResponse> deliverRedemption(@PathVariable Long id) {
        RewardRedemption redemption = redemptionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Redemption not found"));

        if (redemption.getStatus() != RewardStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redemption cannot be delivered");
        }

        RewardRedemption delivered = redemptionService.markAsDelivered(redemption.getId());
        return ResponseEntity.ok(RewardDto.RedemptionResponse.fromEntity(delivered));
    }

    @GetMapping("/redemptions/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Get pending redemptions")
    public ResponseEntity<List<RewardDto.RedemptionResponse>> getPendingRedemptions() {
        List<RewardRedemption> pending = redemptionService.findByStatus(RewardStatus.REQUESTED);
        return ResponseEntity.ok(
                pending.stream()
                        .map(RewardDto.RedemptionResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    private User getCurrentUser(Jwt jwt) {
        String username = jwt.getSubject();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
