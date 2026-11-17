package com.energy.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();
    private static final long CODE_TTL_MS = 5 * 60 * 1000;

    public String sendCode(String phone) {
        String code = String.format("%06d", new Random().nextInt(1000000));
        codeStore.put(phone, new CodeEntry(code, System.currentTimeMillis()));
        System.out.println("[验证码] 手机号: " + phone + "  验证码: " + code);
        return code;
    }

    public boolean verifyCode(String phone, String code) {
        CodeEntry entry = codeStore.get(phone);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.timestamp > CODE_TTL_MS) {
            codeStore.remove(phone);
            return false;
        }
        if (entry.code.equals(code)) {
            codeStore.remove(phone);
            return true;
        }
        return false;
    }

    private static class CodeEntry {
        String code;
        long timestamp;
        CodeEntry(String code, long timestamp) {
            this.code = code;
            this.timestamp = timestamp;
        }
    }
}
