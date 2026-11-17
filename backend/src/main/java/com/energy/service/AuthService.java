package com.energy.service;

import com.energy.dto.AuthResponse;
import com.energy.dto.LoginRequest;
import com.energy.dto.RegisterRequest;
import com.energy.entity.Enterprise;
import com.energy.entity.User;
import com.energy.repository.EnergyConsumptionRepository;
import com.energy.repository.EnterpriseRepository;
import com.energy.repository.UserRepository;
import com.energy.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final EnergyConsumptionRepository energyRepo;
    private final VerificationCodeService codeService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void sendCode(String phone) {
        codeService.sendCode(phone);
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new RuntimeException("该手机号已注册");
        }
        if (!codeService.verifyCode(req.getPhone(), req.getCode())) {
            throw new RuntimeException("验证码无效或已过期");
        }
        User user = new User();
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null && !req.getNickname().isEmpty()
                ? req.getNickname() : req.getRealName());
        user.setEmployeeId(req.getEmployeeId());
        user.setRealName(req.getRealName());
        user.setDepartment(req.getDepartment());
        user.setPosition(req.getPosition());
        user.setEngineerCert(req.getEngineerCert());
        user.setCompanyEmail(req.getCompanyEmail());
        user.setIdCardLast4(req.getIdCardLast4());
        user.setRemark(req.getRemark());
        String role = "enterprise".equals(req.getRequestedRole()) ? "enterprise" : "user";
        user.setRole(role);
        user.setStatus("pending");
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        // pending 状态不返回 token，只返回基本信息
        AuthResponse resp = new AuthResponse(null, user.getPhone(), user.getNickname(), user.getRole());
        resp.setRealName(user.getRealName());
        resp.setDepartment(user.getDepartment());
        resp.setEmployeeId(user.getEmployeeId());
        return resp;
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new RuntimeException("手机号未注册"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if ("pending".equals(user.getStatus())) {
            throw new RuntimeException("PENDING:您的账号正在审核中，请耐心等待管理员审批");
        }
        if ("rejected".equals(user.getStatus())) {
            String reason = user.getRejectReason() != null ? user.getRejectReason() : "未说明";
            throw new RuntimeException("REJECTED:您的注册申请已被拒绝，原因：" + reason);
        }
        if ("disabled".equals(user.getStatus())) {
            throw new RuntimeException("DISABLED:账号已被禁用，请联系管理员");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String userType = user.getUserType() != null ? user.getUserType() : "normal";
        Long enterpriseId = user.getCurrentEnterpriseId();
        String token = jwtUtil.generateToken(user.getPhone(), user.getNickname(), user.getRole(), userType, enterpriseId, user.getId());
        AuthResponse resp = new AuthResponse(token, user.getPhone(), user.getNickname(), user.getRole());
        resp.setUserType(userType);
        resp.setCurrentEnterpriseId(enterpriseId);
        resp.setRealName(user.getRealName());
        resp.setDepartment(user.getDepartment());
        resp.setEmployeeId(user.getEmployeeId());
        // 填充企业名称 & 检查是否有数据
        if (enterpriseId != null) {
            enterpriseRepository.findById(enterpriseId).ifPresent(e -> resp.setEnterpriseName(e.getEnterpriseName()));
            resp.setHasData(energyRepo.countByEnterpriseId(enterpriseId) > 0);
        } else {
            // 个人用户: 用 -userId 作为虚拟企业ID隔离数据
            Long personalEid = -user.getId();
            resp.setHasData(energyRepo.countByEnterpriseId(personalEid) > 0);
        }
        return resp;
    }
}
