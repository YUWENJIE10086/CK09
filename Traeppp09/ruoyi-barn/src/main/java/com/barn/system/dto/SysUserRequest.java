package com.barn.system.dto;

import lombok.Data;

/**
 * 用户管理写入请求 DTO。
 *
 * 只暴露业务允许由客户端提交的字段，避免直接绑定 SysUser 数据库实体导致批量赋值。
 */
@Data
public class SysUserRequest {

    /**
     * 修改用户时使用；新增用户时忽略并由数据库生成。
     */
    private Long id;

    private Long deptId;
    private String userName;
    private String nickName;
    private String userType;
    private String email;
    private String phone;
    private String sex;
    private String avatar;

    /**
     * 仅新增用户时作为初始密码使用；普通编辑接口不会更新密码。
     */
    private String password;

    private String status;
    private String remark;
}
