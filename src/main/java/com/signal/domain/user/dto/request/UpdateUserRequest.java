package com.signal.domain.user.dto.request;

import com.signal.domain.user.entity.User;

public record UpdateUserRequest(
        String name,
        Integer age,
        User.Gender gender
) {
}
