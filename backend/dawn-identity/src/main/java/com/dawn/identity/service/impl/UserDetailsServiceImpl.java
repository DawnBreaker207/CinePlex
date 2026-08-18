package com.dawn.identity.service.impl;

import com.dawn.common.core.constant.ErrorCode;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.identity.model.User;
import com.dawn.identity.model.UserDetailsImpl;
import com.dawn.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String input) throws ResourceNotFoundException {
        User user;
        if (input.contains("@")) {
            user = userRepository
                    .findByEmail(input)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.EMAIL_NOT_FOUND.format()));
        } else {
            user = userRepository
                    .findByUsername(input)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USERNAME_NOT_FOUND.format()));
        }
        return UserDetailsImpl.build(user);
    }
}
