package az.atl.otpservice.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {
private static final String DIGITS = "0123456789";
private static final int OTP_PIN_LENGTH = 6;
private static final SecureRandom RANDOM = new SecureRandom();

    public Integer generateOtp(){
        StringBuilder otp = new StringBuilder(OTP_PIN_LENGTH);
        for (int i = 0; i < OTP_PIN_LENGTH; i++) {
            otp.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }

        return otp.toString().hashCode();
    }
}
