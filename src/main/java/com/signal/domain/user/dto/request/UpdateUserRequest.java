package com.signal.domain.user.dto.request;

import com.signal.domain.user.entity.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 50) @Pattern(regexp = ".*\\S.*") String name,
        @Min(1) @Max(150) Integer age,
        User.Gender gender
) {
}
