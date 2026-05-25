package com.example.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.mapper.BusinessMapper;
import com.example.mapper.UserMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.Objects;

@Component
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    private static AdminMapper staticAdminMapper;
    private static BusinessMapper staticBusinessMapper;
    private static UserMapper staticUserMapper;

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private BusinessMapper businessMapper;
    @Resource
    private UserMapper userMapper;

    @PostConstruct
    public void init() {
        staticAdminMapper = adminMapper;
        staticBusinessMapper = businessMapper;
        staticUserMapper = userMapper;
    }

    public static String createToken(String data, String sign) {
        return JWT.create()
                .withAudience(data)
                .withExpiresAt(DateUtil.offsetHour(new Date(), 2))
                .sign(Algorithm.HMAC256(sign));
    }

    public static Account getCurrentUserOrNull() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader(Constants.TOKEN);
            if (ObjectUtil.isEmpty(token)) {
                token = request.getParameter(Constants.TOKEN);
            }
            if (ObjectUtil.isEmpty(token)) {
                return null;
            }

            String userRole = JWT.decode(token).getAudience().get(0);
            String[] parts = userRole.split("-");
            if (parts.length != 2) {
                return null;
            }

            Integer userId = Integer.valueOf(parts[0]);
            String role = parts[1];
            if (RoleEnum.ADMIN.name().equals(role)) {
                return staticAdminMapper.selectById(userId);
            }
            if (RoleEnum.BUSINESS.name().equals(role)) {
                return staticBusinessMapper.selectById(userId);
            }
            if (RoleEnum.USER.name().equals(role)) {
                return staticUserMapper.selectById(userId);
            }
            return null;
        }
        catch (Exception e) {
            log.debug("Failed to resolve current user from token", e);
            return null;
        }
    }

    public static Account getCurrentUser() {
        Account currentUser = getCurrentUserOrNull();
        if (currentUser == null || currentUser.getId() == null) {
            throw new CustomException(ResultCodeEnum.USER_NOT_LOGIN);
        }
        return currentUser;
    }

    public static void requireRole(RoleEnum... allowedRoles) {
        Account currentUser = getCurrentUser();
        for (RoleEnum allowedRole : allowedRoles) {
            if (Objects.equals(currentUser.getRole(), allowedRole.name())) {
                return;
            }
        }
        throw new CustomException(ResultCodeEnum.FORBIDDEN_ERROR);
    }

    public static boolean hasRole(Account account, RoleEnum role) {
        return account != null && Objects.equals(account.getRole(), role.name());
    }
}
