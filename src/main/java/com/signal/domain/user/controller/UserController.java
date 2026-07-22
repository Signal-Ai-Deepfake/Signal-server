package com.signal.domain.user.controller;

import com.signal.domain.user.dto.request.UpdateUserRequest;
import com.signal.domain.user.dto.response.ProfileImageResponse;
import com.signal.domain.user.dto.response.UserResponse;
import com.signal.domain.user.entity.User;
import com.signal.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        User user = userService.getMyProfile(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody UpdateUserRequest request) {
        User user = userService.updateProfile(userId, request);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal Long userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileImageResponse> uploadProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image) {
        String profileImageUrl = userService.updateProfileImage(userId, image);
        return ResponseEntity.ok(new ProfileImageResponse(profileImageUrl));
    }
}
