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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号：注册 / 登录 / 当前用户信息。
 */
@Service
public class AuthService {

    private static final int NICKNAME_MAX = 50;
    /** 同一手机号连续失败多少次后暂时锁定 */
    private static final int MAX_LOGIN_FAILURES = 5;
    /** 锁定时长：15 分钟 */
    private static final long LOGIN_LOCK_MILLIS = 15 * 60 * 1000L;
    /** 失败计数表容量上限，防止用海量手机号把内存撑爆 */
    private static final int FAILURE_TABLE_MAX = 10_000;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 登录失败计数：手机号 -> [失败次数, 最近一次失败时间戳] */
    private final Map<String, long[]> loginFailures = new ConcurrentHashMap<>();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    /** A-1 手机号全局唯一；A-2 密码加密；A-3 新用户固定 USER；A-4 管理员无法注册 */
    public void register(RegisterRequest req) {
        if (req == null || req.getPhone() == null || !req.getPhone().matches("^1\\d{10}$")) {
            throw BizException.badRequest("手机号格式不正确");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw BizException.badRequest("密码至少 6 位");
        }
        String nickname = req.getNickname() == null ? null : req.getNickname().trim();
        if (nickname != null && nickname.length() > NICKNAME_MAX) {
            throw BizException.badRequest("昵称不能超过 " + NICKNAME_MAX + " 个字符");
        }
        if (userMapper.selectByPhone(req.getPhone()) != null) {
            throw BizException.conflict("该手机号已注册");
        }
        // 不写 system_role，由数据库默认值给出 'USER'；空昵称存 NULL 而不是空串
        userMapper.insert(req.getPhone(), passwordEncoder.encode(req.getPassword()),
                nickname == null || nickname.isEmpty() ? null : nickname);
    }

    public LoginResponse login(LoginRequest req) {
        if (req == null || req.getPhone() == null || req.getPassword() == null) {
            throw BizException.badRequest("手机号和密码不能为空");
        }
        // 简单限流：/api/auth/login 在拦截器白名单里，不限流就能无限撞库
        checkLoginAllowed(req.getPhone());
        User user = userMapper.selectByPhone(req.getPhone());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            recordLoginFailure(req.getPhone());
            throw BizException.unauthorized("手机号或密码错误");
        }
        if (user.getStatus() == 0) {
            throw BizException.unauthorized("账号已被禁用");
        }
        loginFailures.remove(req.getPhone());
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
        if (req == null || req.getNickname() == null || req.getNickname().isBlank()) {
            return;
        }
        String nickname = req.getNickname().trim();
        if (nickname.length() > NICKNAME_MAX) {
            throw BizException.badRequest("昵称不能超过 " + NICKNAME_MAX + " 个字符");
        }
        userMapper.updateNickname(userId, nickname);
    }

    // ---------- 登录限流（内存实现，重启即清零，够用即可） ----------

    private void checkLoginAllowed(String phone) {
        long[] rec = loginFailures.get(phone);
        if (rec == null) {
            return;
        }
        synchronized (rec) {
            if (rec[0] < MAX_LOGIN_FAILURES) {
                return;
            }
            long elapsed = System.currentTimeMillis() - rec[1];
            if (elapsed >= LOGIN_LOCK_MILLIS) {
                // 锁定期已过，重新计数
                loginFailures.remove(phone);
                return;
            }
            long minutes = (LOGIN_LOCK_MILLIS - elapsed) / 60000 + 1;
            throw new BizException(429, "登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
    }

    private void recordLoginFailure(String phone) {
        if (loginFailures.size() > FAILURE_TABLE_MAX) {
            loginFailures.clear();
        }
        long[] rec = loginFailures.computeIfAbsent(phone, k -> new long[]{0, 0});
        synchronized (rec) {
            rec[0]++;
            rec[1] = System.currentTimeMillis();
        }
    }
}
