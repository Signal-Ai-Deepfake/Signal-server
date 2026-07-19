package com.signal.domain.user.service;

import com.signal.domain.user.entity.User;
import com.signal.domain.user.repository.UserRepository;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.file.FileStorage;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final long MAX_PROFILE_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final String PROFILE_IMAGE_DIRECTORY = "profile";

    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    @Transactional
    public String updateProfileImage(Long userId, MultipartFile image) {
        validateImage(image);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SignalException(ErrorCode.NOT_FOUND));

        String profileImageUrl = fileStorage.store(image, PROFILE_IMAGE_DIRECTORY);
        user.updateProfileImage(profileImageUrl);

        return profileImageUrl;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new SignalException(ErrorCode.INVALID_INPUT);
        }
        if (image.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new SignalException(ErrorCode.FILE_TOO_LARGE);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(image.getContentType())) {
            throw new SignalException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }
}
