package com.ftc.caffeinedemo.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author 冯铁城 [17615007230@163.com]
 * @date 2025-07-01 11:49:40
 * @describe 用户实体
 */
@Data
public class User {

    @Schema(defaultValue = "用户名")
    private String username;

    @Schema(defaultValue = "密码")
    private String password;

    @Schema(defaultValue = "昵称")
    private String nickname;

    @Schema(defaultValue = "地址")
    private String address;
}
