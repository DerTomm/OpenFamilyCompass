package org.openfamilycompass.controller;

import java.util.List;

import org.openfamilycompass.dto.BehaviorEvaluationDTO;
import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.service.BehaviorEvaluationService;
import org.openfamilycompass.service.PointService;
import org.openfamilycompass.service.PointTransactionWithBalance;
import org.openfamilycompass.service.RewardRedemptionService;
import org.openfamilycompass.service.RewardService;
import org.openfamilycompass.service.TaskInstanceService;
import org.openfamilycompass.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/child")
@RequiredArgsConstructor
public class ChildController {

        private final TaskInstanceService taskInstanceService;
        private final RewardService rewardService;
        private final RewardRedemptionService redemptionService;
        private final PointService pointService;
        private final UserService userService;
        private final BehaviorEvaluationService behaviorEvaluationService;

        @GetMapping("/dashboard")
        public String dashboard(Authentication authentication, Model model) {
                User currentUser = (User) authentication.getPrincipal();

                // Load fresh user data from database to get updated points
                User freshUser = userService.findById(currentUser.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<TaskInstance> pendingTasks = taskInstanceService.findPendingForUser(freshUser);
                List<TaskInstance> completedTasks = taskInstanceService.findByUserAndStatus(freshUser,
                                TaskStatus.CHILD_COMPLETED);
                List<BehaviorEvaluationDTO> behaviorEvaluations = behaviorEvaluationService
                                .getCurrentBehaviorEvaluationsForChild(freshUser);

                int totalBehaviorPoints = behaviorEvaluations.stream()
                                .mapToInt(BehaviorEvaluationDTO::getCurrentPoints)
                                .sum();

                model.addAttribute("user", freshUser);
                model.addAttribute("pendingTasks", pendingTasks);
                model.addAttribute("completedTasks", completedTasks);
                model.addAttribute("totalPoints", freshUser.getTotalPoints());
                model.addAttribute("totalBehaviorPoints", totalBehaviorPoints);
                model.addAttribute("behaviorEvaluations", behaviorEvaluations);

                return "child/dashboard";
        }

        @GetMapping("/behaviors")
        public String behaviors(Authentication authentication, Model model) {
                User currentUser = (User) authentication.getPrincipal();

                // Load fresh user data from database to get updated points
                User freshUser = userService.findById(currentUser.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<BehaviorEvaluationDTO> behaviorEvaluations = behaviorEvaluationService
                                .getCurrentBehaviorEvaluationsForChild(freshUser);

                int totalBehaviorPoints = behaviorEvaluations.stream()
                                .mapToInt(BehaviorEvaluationDTO::getCurrentPoints)
                                .sum();

                model.addAttribute("user", freshUser);
                model.addAttribute("behaviorEvaluations", behaviorEvaluations);
                model.addAttribute("totalBehaviorPoints", totalBehaviorPoints);

                return "child/behaviors";
        }

        @GetMapping("/tasks")
        public String tasks(Authentication authentication, Model model) {
                User currentUser = (User) authentication.getPrincipal();

                List<TaskInstance> tasks = taskInstanceService.findPendingForUser(currentUser);

                model.addAttribute("user", currentUser);
                model.addAttribute("tasks", tasks);

                return "child/tasks";
        }

        @PostMapping("/tasks/{id}/complete")
        public String completeTask(@PathVariable Long id, Authentication authentication) {
                User currentUser = (User) authentication.getPrincipal();
                taskInstanceService.markAsCompleted(id, currentUser);
                return "redirect:/child/tasks";
        }

        @GetMapping("/shop")
        public String shop(Authentication authentication, Model model) {
                User currentUser = (User) authentication.getPrincipal();

                // Load fresh user data from database to get updated points
                User freshUser = userService.findById(currentUser.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<Reward> rewards = rewardService.findAllActive();
                List<RewardRedemption> myRedemptions = redemptionService.findByUser(freshUser);

                model.addAttribute("user", freshUser);
                model.addAttribute("rewards", rewards);
                model.addAttribute("myRedemptions", myRedemptions);
                model.addAttribute("totalPoints", freshUser.getTotalPoints());

                return "child/shop";
        }

        @PostMapping("/shop/redeem/{rewardId}")
        public String redeemReward(@PathVariable Long rewardId, Authentication authentication) {
                User currentUser = (User) authentication.getPrincipal();

                // Load fresh user data to ensure we have current points
                User freshUser = userService.findById(currentUser.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Reward reward = rewardService.findById(rewardId)
                                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));

                try {
                        redemptionService.requestReward(freshUser, reward);
                } catch (IllegalStateException e) {
                        // Not enough points
                        return "redirect:/child/shop?error=notenough";
                }

                return "redirect:/child/shop?success=true";
        }

        @GetMapping("/history")
        public String history(Authentication authentication, Model model) {
                User currentUser = (User) authentication.getPrincipal();

                // Load fresh user data from database to get updated points
                User freshUser = userService.findById(currentUser.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<PointTransactionWithBalance> transactionsWithBalance = pointService
                                .getTransactionHistoryWithBalance(freshUser);

                model.addAttribute("user", freshUser);
                model.addAttribute("transactions", transactionsWithBalance);

                return "child/history";
        }
}
