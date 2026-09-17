package com.dawn.identity.service;

import com.dawn.common.core.constant.URole;
import com.dawn.common.core.exception.wrapper.PermissionDeniedException;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.identity.dto.request.UserRequest;
import com.dawn.identity.dto.response.UserResponse;
import com.dawn.identity.model.Role;
import com.dawn.identity.model.User;
import com.dawn.identity.repository.UserRepository;
import com.dawn.identity.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
public class UserServiceTests {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User
                .builder()
                .id(1L)
                .username("testuser")
                .email("test@gmail.com")
                .build();
    }

    @Test
    public void findAll_GivenUserExist_WhenCalled_ThenReturnsUserList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository
                .findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        List<UserResponse> result = userService
                .findAll(pageable)
                .getContent();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(user.getUsername(), result.getFirst().getUsername());
        assertEquals(user.getEmail(), result.getFirst().getEmail());
        verify(userRepository, times(1))
                .findAll(pageable);
    }

    @Test
    public void findAll_GivenNoUserExist_WhenCalled_ThenReturnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository
                .findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        List<UserResponse> result = userService
                .findAll(pageable)
                .getContent();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1))
                .findAll(pageable);
    }

    @Test
    public void findOne_GivenValidId_WhenFound_ThenReturnsUserDto() {
        when(userRepository
                .findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse result = userService
                .findOne(1L);

        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
        verify(userRepository, times(1))
                .findById(1L);
    }

    @Test
    public void findOne_GivenNullId_WhenNotFound_ThenThrowResourceNotFoundException() {
        assertThrows(
                Exception.class,
                () -> userService.findOne(null));
    }

    @Test
    public void findOne_GivenInvalidId_WhenNotFound_ThenThrowResouceNotFoundException() {
        when(userRepository
                .findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findOne(1L));
        verify(userRepository, times(1))
                .findById(1L);
    }

    @Test
    public void findByEmail_GivenValidEmail_WhenNotFound_ThenReturnUserDto() {
        when(userRepository
                .findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        UserResponse result = userService
                .findByEmail("test@gmail.com");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@gmail.com", result.getEmail());
        verify(userRepository, times(1))
                .findByEmail("test@gmail.com");
    }

    @Test
    public void findByEmail_GivenInvalidEmail_WhenNotFound_ThenThrowResourceNotFoundException() {
        when(userRepository
                .findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findByEmail("test@gmail.com"));
        verify(userRepository, times(1))
                .findByEmail("test@gmail.com");
    }

    @Test
    public void updateUser_GivenValidIdAndDetails_WhenUpdated_ThenReturnUpdatedUserDto() {
        UserRequest updateDetails = UserRequest
                .builder()
                .username("updatedUser")
                .avatar("updated@gmail.com")
                .build();

        when(userRepository
                .findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository
                .save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService
                .update(1L, updateDetails);

        assertNotNull(result);
        assertEquals("updatedUser", result.getUsername());
        assertEquals("updated@gmail.com", result.getAvatar());
        assertEquals("test@gmail.com", result.getEmail());
        verify(userRepository, times(1))
                .findById(1L);
        verify(userRepository, times(1))
                .save(any(User.class));
    }

    @Test
    public void updateUser_GivenInvalidId_WhenNotFound_ThenThrowResourceNotFoundException() {
        UserRequest updatedDetails = UserRequest
                .builder()
                .username("updatedUser")
                .avatar("updated@gmail.com")
                .build();

        when(userRepository
                .findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.update(1L, updatedDetails));
        verify(userRepository, times(1))
                .findById(1L);
        verify(userRepository, never())
                .save(any());
    }

    @Test
    public void updateStatus_GivenLastAdmin_WhenDeactivate_ThenThrow() {
        User admin = User.builder().id(1L).username("admin").email("a@x.com").isActive(true)
                .roles(Set.of(Role.builder().name(URole.ADMIN).build())).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRolesNameInAndActive(List.of(URole.OWNER, URole.ADMIN), true))
                .thenReturn(1L);

        assertThrows(
                PermissionDeniedException.class,
                () -> userService.updateStatus(1L, false));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void updateStatus_GivenTwoAdmins_WhenDeactivateOne_ThenOk() {
        User admin = User.builder().id(1L).username("admin").email("a@x.com").isActive(true)
                .roles(Set.of(Role.builder().name(URole.ADMIN).build())).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRolesNameInAndActive(List.of(URole.OWNER, URole.ADMIN), true))
                .thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateStatus(1L, false);

        verify(userRepository, times(1)).save(any(User.class));
    }
}
