package com.energy.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位数字")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    private String nickname;

    @NotBlank(message = "工号不能为空")
    private String employeeId;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotBlank(message = "部门不能为空")
    private String department;

    private String position;
    private String engineerCert;
    private String companyEmail;
    private String idCardLast4;
    private String remark;
    private String requestedRole;  // "user" or "enterprise"
}
