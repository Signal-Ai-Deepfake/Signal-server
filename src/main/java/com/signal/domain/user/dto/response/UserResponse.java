package com.signal.domain.user.dto.response;

import com.signal.domain.user.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String name,
        Integer age,
        User.Gender gender,
        String profileImageUrl,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAge(),
                user.getGender(),
                user.getProfileImageUrl(),
                user.getCreatedAt());
    }
}
