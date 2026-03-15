package com.vadim.grpc.authservice.app.query.service;


import com.vadim.grpc.authservice.app.query.dto.UserReadDto;
import com.vadim.grpc.authservice.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthQueryService {
    private final UserRepository userRepository;

    public UserReadDto getById(String id) {
        return userRepository.findById(id)
                .map(u -> new UserReadDto(u.getId(), u.getEmail(), u.isConfirmed()))
                .orElse(null);
    }
}