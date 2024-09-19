package az.atl.otpservice.service.Impl;
import org.springframework.beans.factory.annotation.Value;

import az.atl.otpservice.dao.entity.OtpEntity;
import az.atl.otpservice.dao.enums.OtpStatus;
import az.atl.otpservice.dao.repository.OtpRepository;
import az.atl.otpservice.service.OtpService;
import az.atl.otpservice.util.OtpGenerator;
import az.atl.otpservice.util.dto.SendOtpRequestDto;
import az.atl.otpservice.util.dto.SendOtpResponseDto;
import lombok.RequiredArgsConstructor;
//import lombok.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor


public class OtpServiceImpl implements OtpService {

    @Value("${otp.expirationTime}")
    private Integer EXPIRATION_TIME;
    @Value("${otp.sendOtpLimit}")
    private Integer SEND_OTP_LIMIT;
    private final OtpRepository otpRepository;
    private final OtpGenerator otpGenerator;
    @Override
    public SendOtpResponseDto sendOtp(SendOtpRequestDto sendOtpRequestDto) {
        var otpEntityOptional = otpRepository.findByMsisdn(sendOtpRequestDto.getMsisdn());
        if (otpEntityOptional.isPresent()){
            var entity = otpEntityOptional.get();
            if (OtpStatus.BLOCKED.equals(entity.getOtpStatus())){
                if (Instant.now().isAfter(entity.getExpirationTime())){
                    sendOtpAgainAfterBlock(entity);
                    return new SendOtpResponseDto(OtpStatus.SUCCESS);
                }
                else{
                    return new SendOtpResponseDto(OtpStatus.BLOCKED);
                }
            }
            else{
                if (SEND_OTP_LIMIT < entity.getSendOtpCount()){
                    makeUserBlocked(entity);
                    return new SendOtpResponseDto(OtpStatus.BLOCKED);
                }
                else{
                    sendOtpAgain(entity);
                    return new SendOtpResponseDto(OtpStatus.SUCCESS);

                }
            }
        }
        sendOtpFirstTime(sendOtpRequestDto);
        return new SendOtpResponseDto(OtpStatus.SUCCESS);

    }

    private void makeUserBlocked(OtpEntity otpEntity){
        otpEntity.setOtpStatus(OtpStatus.BLOCKED);
        otpEntity.setSendOtpCount(otpEntity.getAttemptCount() + 1);
        otpEntity.setExpirationTime(Instant.now().plusMillis(EXPIRATION_TIME));
        otpRepository.save(otpEntity);
    }


    private void sendOtpAgain(OtpEntity otpEntity){
        var otpCode = otpGenerator.generateOtp();
        var expireTime = Instant.now().plusMillis(EXPIRATION_TIME);
        var sendOtpCount = otpEntity.getSendOtpCount() + 1;
        otpEntity.setOtp(otpCode);
        otpEntity.setOtpStatus(OtpStatus.PENDING);
        otpEntity.setExpirationTime(expireTime);
        otpEntity.setSendOtpCount(sendOtpCount);
        otpRepository.save(otpEntity);

    }
    private void sendOtpAgainAfterBlock(OtpEntity otpEntity){
        var otpCode = otpGenerator.generateOtp();
        var expireTime = Instant.now().plusMillis(EXPIRATION_TIME);

        otpEntity.setOtp(otpCode);
        otpEntity.setOtpStatus(OtpStatus.PENDING);
        otpEntity.setExpirationTime(expireTime);
        otpEntity.setAttemptCount(0);
        otpEntity.setSendOtpCount(1);
        otpRepository.save(otpEntity);
    }
    private void sendOtpFirstTime(SendOtpRequestDto sendOtpRequestDto){
        var otpCode = otpGenerator.generateOtp();
        var expireTime = Instant.now().plusMillis(EXPIRATION_TIME);

        var otpEntity = OtpEntity.builder()
                .otpType(sendOtpRequestDto.getOtpType())
                .otpStatus(OtpStatus.PENDING)
                .attemptCount(0)
                .sendOtpCount(1)
                .expirationTime(expireTime)
                .otp(otpCode)
                .msisdn(sendOtpRequestDto.getMsisdn())
                .build();

        otpRepository.save(otpEntity);
    }

    @Override
    public OtpStatus verifyOtp(String msisdn, String enteredOtp) {
        var otpEntityOptional = otpRepository.findByMsisdn(msisdn);
        if (otpEntityOptional.isEmpty()) {
            return OtpStatus.NOT_FOUND;
        }
        var otpEntity = otpEntityOptional.get();
        if (OtpStatus.BLOCKED.equals(otpEntity.getOtpStatus())) {
            return OtpStatus.BLOCKED;

        }
        if (Instant.now().isAfter(otpEntity.getExpirationTime())) {
            return OtpStatus.EXPIRED;
        }
        if (!otpEntity.getOtp().equals(enteredOtp)) {
            otpEntity.setAttemptCount(otpEntity.getAttemptCount() + 1);
            otpRepository.save(otpEntity);
            return OtpStatus.INVALID;
        }
        otpEntity.setOtpStatus(OtpStatus.VERIFIED);
        otpRepository.save(otpEntity);
        return OtpStatus.VERIFIED;
    }
}
