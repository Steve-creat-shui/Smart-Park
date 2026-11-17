package com.energy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String phone;
    private String nickname;
    private String role;
    private String userType;
    private Long currentEnterpriseId;
    private String enterpriseName;
    private String realName;
    private String department;
    private String employeeId;
    private Boolean hasData;

    public AuthResponse(String token, String phone, String nickname, String role) {
        this.token = token;
        this.phone = phone;
        this.nickname = nickname;
        this.role = role;
    }
}
