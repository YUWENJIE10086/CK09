package com.barn.system.dto;

import lombok.Data;

@Data
public class SysUserDTO {
    private Long id;
    private String userName;
    private String nickName;
    private String password;
    private String email;
    private String phone;
    private String sex;
    private String avatar;
    private String remark;
}
