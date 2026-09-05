package org.openfamilycompass.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.api.v1.dto.BehaviorDto;
import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.BehaviorEvaluationService;
import org.openfamilycompass.service.BehaviorService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BehaviorApiControllerTest {

    @Mock
    private BehaviorService behaviorService;

    @Mock
    private BehaviorEvaluationService evaluationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private BehaviorApiController behaviorApiController;

    private Behavior behavior;
    private User firstChild;
    private User secondChild;

    @BeforeEach
    void setUp() {
        firstChild = createChild(1L, "first");
        secondChild = createChild(2L, "second");

        behavior = new Behavior();
        behavior.setId(10L);
        behavior.setTitle("Test behavior");
        behavior.setGuideline("Test guideline");
        behavior.setPoints(5);
        behavior.setMinusPoints(3);
        behavior.setActive(true);
        behavior.setUser(firstChild);
    }

    @Test
    void updateBehavior_ShouldChangeAssignedChild() {
        when(behaviorService.findById(10L)).thenReturn(Optional.of(behavior));
        when(userService.findById(2L)).thenReturn(Optional.of(secondChild));
        when(behaviorService.save(any(Behavior.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BehaviorDto.UpdateRequest request = new BehaviorDto.UpdateRequest();
        request.setUserId(2L);

        ResponseEntity<BehaviorDto.Response> response =
                behaviorApiController.updateBehavior(10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUser()).isNotNull();
        assertThat(response.getBody().getUser().getId()).isEqualTo(2L);

        ArgumentCaptor<Behavior> captor = ArgumentCaptor.forClass(Behavior.class);
        verify(behaviorService).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(secondChild);
    }

    @Test
    void updateBehavior_WithZeroUserId_ShouldAssignToAllChildren() {
        when(behaviorService.findById(10L)).thenReturn(Optional.of(behavior));
        when(behaviorService.save(any(Behavior.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BehaviorDto.UpdateRequest request = new BehaviorDto.UpdateRequest();
        request.setUserId(0L);

        ResponseEntity<BehaviorDto.Response> response =
                behaviorApiController.updateBehavior(10L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUser()).isNull();
        verify(userService, never()).findById(any());
    }

    @Test
    void updateBehavior_WithUnknownUserId_ShouldReturnBadRequest() {
        when(behaviorService.findById(10L)).thenReturn(Optional.of(behavior));
        when(userService.findById(99L)).thenReturn(Optional.empty());

        BehaviorDto.UpdateRequest request = new BehaviorDto.UpdateRequest();
        request.setUserId(99L);

        assertThatThrownBy(() -> behaviorApiController.updateBehavior(10L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private User createChild(Long id, String username) {
        User child = new User();
        child.setId(id);
        child.setUsername(username);
        child.setFirstName(username);
        child.setRole(UserRole.CHILD);
        child.setActive(true);
        return child;
    }
}
