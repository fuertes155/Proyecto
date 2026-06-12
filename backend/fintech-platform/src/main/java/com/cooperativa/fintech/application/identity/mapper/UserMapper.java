package com.cooperativa.fintech.application.identity.mapper;

import com.cooperativa.fintech.application.identity.dto.UserResponse;
import com.cooperativa.fintech.domain.identity.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getDocumentType(),
                user.getDocumentNumber(),
                user.getEmail(),
                user.getPhone(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.getKycStatus(),
                user.getCreatedAt()
        );
    }
}
