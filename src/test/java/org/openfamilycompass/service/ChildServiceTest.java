package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.Child;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.ChildRepository;

@ExtendWith(MockitoExtension.class)
class ChildServiceTest {

    @Mock
    private ChildRepository childRepository;

    @InjectMocks
    private ChildService childService;

    private Child testChild;
    private User childUser;

    @BeforeEach
    void setUp() {
        childUser = new User();
        childUser.setId(2L);
        childUser.setUsername("child");
        childUser.setRole(UserRole.CHILD);

        testChild = new Child();
        testChild.setId(1L);
        testChild.setFirstName("John");
        testChild.setUser(childUser);
        testChild.setTotalPoints(100);
    }

    @Test
    void findById_ShouldReturnChild_WhenExists() {
        // Given
        when(childRepository.findById(1L)).thenReturn(Optional.of(testChild));

        // When
        Optional<Child> result = childService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
        verify(childRepository).findById(1L);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(childRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Child> result = childService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(childRepository).findById(999L);
    }

    @Test
    void findByUser_ShouldReturnChild() {
        // Given
        when(childRepository.findByUser(childUser)).thenReturn(Optional.of(testChild));

        // When
        Optional<Child> result = childService.findByUser(childUser);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isEqualTo(childUser);
        verify(childRepository).findByUser(childUser);
    }

    @Test
    void createChild_ShouldCreateNewChild() {
        // Given
        when(childRepository.save(any(Child.class))).thenReturn(testChild);

        // When
        Child result = childService.createChild(childUser, "John", "/avatar.png");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(childRepository).save(any(Child.class));
    }

    @Test
    void updatePoints_ShouldUpdateChildPoints() {
        // Given
        when(childRepository.findById(1L)).thenReturn(Optional.of(testChild));
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Child result = childService.updatePoints(1L, 150);

        // Then
        assertThat(result.getTotalPoints()).isEqualTo(150);
        verify(childRepository).findById(1L);
        verify(childRepository).save(any(Child.class));
    }

    @Test
    void updatePoints_ShouldThrowException_WhenChildNotFound() {
        // Given
        when(childRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> childService.updatePoints(999L, 150))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Child not found");
    }

    @Test
    void updateAvatar_ShouldUpdateAvatarPath() {
        // Given
        when(childRepository.findById(1L)).thenReturn(Optional.of(testChild));
        when(childRepository.save(any(Child.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String newAvatar = "/new-avatar.png";

        // When
        Child result = childService.updateAvatar(1L, newAvatar);

        // Then
        assertThat(result.getAvatarPath()).isEqualTo(newAvatar);
        verify(childRepository).findById(1L);
        verify(childRepository).save(any(Child.class));
    }

    @Test
    void findAll_ShouldReturnAllChildren() {
        // Given
        List<Child> children = Arrays.asList(testChild);
        when(childRepository.findAllByOrderByFirstNameAsc()).thenReturn(children);

        // When
        List<Child> result = childService.findAll();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        verify(childRepository).findAllByOrderByFirstNameAsc();
    }
}
