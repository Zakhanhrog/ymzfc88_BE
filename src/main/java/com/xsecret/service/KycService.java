package com.xsecret.service;

import com.xsecret.dto.KycApprovalRequest;
import com.xsecret.dto.KycResponse;
import com.xsecret.dto.KycSubmissionRequest;
import com.xsecret.entity.KycVerification;
import com.xsecret.entity.User;
import com.xsecret.repository.KycVerificationRepository;
import com.xsecret.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycVerificationRepository kycVerificationRepository;
    private final UserRepository userRepository;
    private final String UPLOAD_DIR = "uploads/kyc/";

    @Transactional
    public KycResponse submitKyc(Long userId, KycSubmissionRequest request, 
                                  MultipartFile frontImage, MultipartFile backImage) throws IOException {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra xem user đã có KYC pending hoặc approved chưa
        Optional<KycVerification> existingKyc = kycVerificationRepository.findByUser(user);
        if (existingKyc.isPresent()) {
            KycVerification existing = existingKyc.get();
            if (existing.getStatus() == KycVerification.KycStatus.PENDING) {
                throw new RuntimeException("Bạn đã có yêu cầu xác thực đang chờ duyệt");
            }
            if (existing.getStatus() == KycVerification.KycStatus.APPROVED) {
                throw new RuntimeException("Tài khoản của bạn đã được xác thực");
            }
        }

        // Tạo thư mục nếu chưa có
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Upload front image
        String frontImageUrl = saveFile(frontImage, userId);
        
        // Upload back image
        String backImageUrl = saveFile(backImage, userId);

        // Tạo KYC verification mới
        KycVerification kyc = new KycVerification();
        kyc.setUser(user);
        kyc.setFrontImageUrl(frontImageUrl);
        kyc.setBackImageUrl(backImageUrl);
        kyc.setIdNumber(request.getIdNumber());
        kyc.setFullName(request.getFullName());
        kyc.setStatus(KycVerification.KycStatus.PENDING);
        kyc.setSubmittedAt(LocalDateTime.now());

        KycVerification savedKyc = kycVerificationRepository.save(kyc);
        
        return KycResponse.fromEntity(savedKyc);
    }

    private String saveFile(MultipartFile file, Long userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        
        String filename = UUID.randomUUID().toString() + "_" + userId + extension;
        Path filePath = Paths.get(UPLOAD_DIR + filename);
        
        Files.write(filePath, file.getBytes());
        
        return filename;
    }

    @Transactional(readOnly = true)
    public KycResponse getUserKycStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<KycVerification> kyc = kycVerificationRepository.findByUser(user);
        
        return kyc.map(KycResponse::fromEntity).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<KycResponse> getAllKycRequests() {
        return kycVerificationRepository.findAllByOrderBySubmittedAtDesc()
                .stream()
                .map(KycResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KycResponse> getPendingKycRequests() {
        return kycVerificationRepository.findByStatus(KycVerification.KycStatus.PENDING)
                .stream()
                .map(KycResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public KycResponse approveKyc(Long adminId, KycApprovalRequest request) {
        KycVerification kyc = kycVerificationRepository.findById(request.getKycId())
                .orElseThrow(() -> new RuntimeException("KYC request not found"));

        if ("approve".equalsIgnoreCase(request.getAction())) {
            kyc.setStatus(KycVerification.KycStatus.APPROVED);
            kyc.setVerifiedAt(LocalDateTime.now());
            kyc.setVerifiedBy(adminId);
            kyc.setAdminNotes(request.getAdminNotes());

            // Cập nhật user status
            User user = kyc.getUser();
            user.setKycVerified(true);
            userRepository.save(user);
            
        } else if ("reject".equalsIgnoreCase(request.getAction())) {
            kyc.setStatus(KycVerification.KycStatus.REJECTED);
            kyc.setVerifiedAt(LocalDateTime.now());
            kyc.setVerifiedBy(adminId);
            kyc.setRejectedReason(request.getRejectedReason());
            kyc.setAdminNotes(request.getAdminNotes());
        } else {
            throw new RuntimeException("Invalid action");
        }

        KycVerification savedKyc = kycVerificationRepository.save(kyc);
        return KycResponse.fromEntity(savedKyc);
    }

    @Transactional(readOnly = true)
    public boolean isUserVerified(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return kycVerificationRepository.existsByUserAndStatus(user, KycVerification.KycStatus.APPROVED);
    }
}

