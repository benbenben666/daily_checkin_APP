package com.ben.daily_check_in.service;

import com.ben.daily_check_in.common.BizException;
import com.ben.daily_check_in.dto.auth.LoginRequest;
import com.ben.daily_check_in.dto.auth.LoginResponse;
import com.ben.daily_check_in.dto.auth.MeResponse;
import com.ben.daily_check_in.dto.auth.RegisterRequest;
import com.ben.daily_check_in.entity.User;
import com.ben.daily_check_in.mapper.UserMapper;
import com.ben.daily_check_in.security.JwtUtil;
import com.ben.daily_check_in.security.UserContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 账号：注册 / 登录 / 当前用户信息。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /** A-1 手机号全局唯一；A-2 密码加密；A-3 新用户固定 USER；A-4 管理员无法注册 */
    public void register(RegisterRequest req) {
        if (req.getPhone() == null || !req.getPhone().matches("^1\\d{10}$")) {
            throw BizException.badRequest("手机号格式不正确");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw BizException.badRequest("密码至少 6 位");
        }
        if (userMapper.selectByPhone(req.getPhone()) != null) {
            throw BizException.conflict("该手机号已注册");
        }
        // 不写 system_role，由数据库默认值给出 'USER'
        userMapper.insert(req.getPhone(), passwordEncoder.encode(req.getPassword()), req.getNickname());
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectByPhone(req.getPhone());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("手机号或密码错误");
        }
        if (user.getStatus() == 0) {
            throw BizException.unauthorized("账号已被禁用");
        }
        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.issue(user.getId()));
        resp.setUserId(user.getId());
        resp.setPhone(user.getPhone());
        resp.setNickname(user.getNickname());
        resp.setAvatarKey(user.getAvatarKey());
        resp.setSystemRole(user.getSystemRole());
        return resp;
    }

    public MeResponse me() {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        MeResponse resp = new MeResponse();
        resp.setUserId(user.getId());
        resp.setPhone(user.getPhone());
        resp.setNickname(user.getNickname());
        resp.setAvatarKey(user.getAvatarKey());
        resp.setSystemRole(user.getSystemRole());
        return resp;
    }

    /** 修改自己的昵称 */
    public void updateMe(com.ben.daily_check_in.dto.auth.UpdateMeRequest req) {
        Long userId = UserContext.requireUserId();
        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            userMapper.updateNickname(userId, req.getNickname().trim());
        }
    }
}
