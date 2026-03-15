package com.vadim.grpc.authservice.infra.grpc;


import com.vadim.grpc.authservice.app.command.service.AuthCommandService;
import com.vadim.grpc.authservice.app.query.service.AuthQueryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import com.vadim.grpc.authservice.proto.AuthServiceGrpc.AuthServiceImplBase;
import com.vadim.grpc.authservice.proto.AuthResponse;
import com.vadim.grpc.authservice.proto.RegisterRequest;
import com.vadim.grpc.authservice.proto.AuthRequest;
import com.vadim.grpc.authservice.proto.RequestResetRequest;
import com.vadim.grpc.authservice.proto.EmptyResponse;
import com.vadim.grpc.authservice.proto.ResetPasswordRequest;
import com.vadim.grpc.authservice.proto.UserDto;
import com.vadim.grpc.authservice.proto.GetUserRequest;
import io.grpc.Status;

@GrpcService
@RequiredArgsConstructor
public class AuthGrpcServiceImpl extends AuthServiceImplBase {

    private final AuthCommandService commands;
    private final AuthQueryService queries;

    @Override
    public void register(RegisterRequest req, StreamObserver<AuthResponse> responseObserver) {
        try {
            String userId = commands.register(req.getEmail(), req.getPassword());
            AuthResponse resp = AuthResponse.newBuilder().setUserId(userId).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void authenticate(AuthRequest req, StreamObserver<AuthResponse> responseObserver) {
        try {
            var res = commands.authenticate(req.getEmail(), req.getPassword());
            AuthResponse r = AuthResponse.newBuilder()
                    .setAccessToken(res.accessToken())
                    .setRefreshToken(res.refreshToken())
                    .setUserId(res.userId()).build();
            responseObserver.onNext(r);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void requestPasswordReset(RequestResetRequest req, StreamObserver<EmptyResponse> responseObserver) {
        try {
            commands.requestPasswordReset(req.getEmail());
            responseObserver.onNext(EmptyResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest req, StreamObserver<AuthResponse> responseObserver) {
        try {
            var res = commands.resetPassword(req.getEmail(), req.getToken(), req.getNewPassword());
            AuthResponse r = AuthResponse.newBuilder()
                    .setAccessToken(res.accessToken())
                    .setRefreshToken(res.refreshToken())
                    .setUserId(res.userId()).build();
            responseObserver.onNext(r);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getUser(GetUserRequest req, StreamObserver<UserDto> responseObserver) {
        var user = queries.getById(req.getId());
        if (user == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("User not found").asRuntimeException());
            return;
        }
        UserDto dto = UserDto.newBuilder().setId(user.id()).setEmail(user.email()).setConfirmed(user.confirmed()).build();
        responseObserver.onNext(dto);
        responseObserver.onCompleted();
    }
}