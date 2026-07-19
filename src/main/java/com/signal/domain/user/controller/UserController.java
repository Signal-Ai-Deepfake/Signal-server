package com.signal.domain.user.controller;

import com.signal.domain.user.dto.response.ProfileImageResponse;
import com.signal.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageResponse> uploadProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image) {
        String profileImageUrl = userService.updateProfileImage(userId, image);
        return ResponseEntity.ok(new ProfileImageResponse(profileImageUrl));
    }
}
