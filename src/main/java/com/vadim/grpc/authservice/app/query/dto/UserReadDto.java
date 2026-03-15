package com.vadim.grpc.authservice.app.query.dto;


public record UserReadDto(String id, String email, boolean confirmed) {}