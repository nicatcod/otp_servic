package az.atl.otpservice.service;

import az.atl.otpservice.dao.enums.OtpStatus;
import az.atl.otpservice.util.dto.SendOtpRequestDto;
import az.atl.otpservice.util.dto.SendOtpResponseDto;

public interface OtpService {

    SendOtpResponseDto sendOtp (SendOtpRequestDto sendOtpRequestDto);

    OtpStatus verifyOtp(String msisdn, String enteredOtp);
}

