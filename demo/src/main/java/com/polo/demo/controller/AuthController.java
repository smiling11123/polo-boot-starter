package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.core.model.Result;
import com.polo.boot.security.annotation.CurrentUser;
import com.polo.boot.security.interceptor.TokenResolver;
import com.polo.boot.security.model.ClientDevice;
import com.polo.boot.security.model.DeviceInfo;
import com.polo.boot.security.model.LoginUser;
import com.polo.boot.security.model.TokenPair;
import com.polo.boot.security.service.TokenService;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.annotation.OperationType;
import com.polo.demo.security.DemoLoginPrincipal;
import com.polo.demo.service.DemoUserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Autowired
    private TokenService tokenService;

    @Autowired
    private DemoUserProfileService demoUserProfileService;

    @PostMapping("/logintest")
    public Result<TokenPair> loginTest(@RequestBody DemoLoginPrincipal user, HttpServletRequest request){
        TokenPair tokenPair = tokenService.login(user, request);
        return Result.success(tokenPair);
    }


    @PostMapping("/login")
    @ApiOperation(value = "用户登录",
            description = "用户名密码登录并返回 token")
    @OperationLog(module = "认证中心", type = OperationType.LOGIN, desc = "'用户登录[' + #p0.username + ']'")
    public Result<LoginResponse> login(@RequestBody DemoLoginPrincipal request, HttpServletRequest httpServletRequest) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "用户名或密码不能为空");
        }

        DemoLoginPrincipal loginUser = demoUserProfileService.authenticate(request.getUsername(), request.getPassword());

        TokenPair tokenPair = tokenService.login(loginUser, httpServletRequest);

        LoginResponse response = new LoginResponse();
        response.setUserId(loginUser.getUserId());
        response.setUsername(loginUser.getUsername());
        response.setRole(loginUser.getRole());
        response.setTokenPair(tokenPair);
        return Result.success(response);
    }

    @PostMapping("/refresh")
    @ApiOperation(value = "刷新令牌", description = "使用 refreshToken 刷新 accessToken")
    @OperationLog(module = "认证中心", type = OperationType.LOGIN, desc = "刷新令牌")
    public Result<TokenPair> refresh(@RequestBody RefreshRequest request) {
        if (request == null || !StringUtils.hasText(request.getRefreshToken())) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "refreshToken 不能为空");
        }
        return Result.success(tokenService.refresh(request.getRefreshToken()));
    }

    @GetMapping("/devices")
    @ApiOperation(value = "查看在线设备", description = "获取当前账号在线设备列表")
    @OperationLog(module = "认证中心", type = OperationType.QUERY, desc = "查看在线设备")
    public Result<List<DeviceInfo>> devices(@CurrentUser DemoLoginPrincipal loginUser,
                                            HttpServletRequest httpServletRequest) {
        String currentSessionId = tokenService.resolveCurrentSessionId(requireAccessToken(httpServletRequest));
        return Result.success(tokenService.listDevices(loginUser.getUserId(), currentSessionId));
    }

    @PostMapping("/logout")
    @ApiOperation(value = "退出当前设备", description = "注销当前登录设备")
    @OperationLog(module = "认证中心", type = OperationType.LOGOUT, desc = "退出当前设备")
    public Result<Map<String, String>> logout(HttpServletRequest httpServletRequest) {
        tokenService.logout(requireAccessToken(httpServletRequest));
        return Result.success(Map.of("message", "退出成功"));
    }

    @DeleteMapping("/devices/{sessionId}")
    @ApiOperation(value = "下线指定设备", description = "按 sessionId 下线某个设备")
    @OperationLog(module = "认证中心", type = OperationType.LOGOUT, desc = "'下线设备[' + #sessionId + ']'")
    public Result<Map<String, String>> logoutDevice(@CurrentUser DemoLoginPrincipal loginUser, @PathVariable String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "sessionId 不能为空");
        }
        tokenService.logoutDevice(loginUser.getUserId(), sessionId);
        return Result.success(Map.of("message", "设备已下线"));
    }

    @PostMapping("/logout-all")
    @ApiOperation(value = "退出全部设备", description = "退出当前账号的全部设备")
    @OperationLog(module = "认证中心", type = OperationType.LOGOUT, desc = "退出全部设备")
    public Result<Map<String, String>> logoutAll(@CurrentUser DemoLoginPrincipal loginUser,
                                                 HttpServletRequest httpServletRequest,
                                                 @RequestParam(defaultValue = "false") boolean keepCurrent) {
        String keepSessionId = keepCurrent ? tokenService.resolveCurrentSessionId(requireAccessToken(httpServletRequest)) : null;
        tokenService.logoutAll(loginUser.getUserId(), keepSessionId);
        return Result.success(Map.of("message", keepCurrent ? "已退出其他设备" : "已退出全部设备"));
    }

    @GetMapping("/demo-accounts")
    @ApiOperation(value = "查看 demo 测试账号", description = "列出 demo 中可直接用于测试权限、数据权限、审计和乐观锁的账号")
    public Result<List<Map<String, Object>>> demoAccounts() {
        return Result.success(demoUserProfileService.listAccounts());
    }

    private String requireAccessToken(HttpServletRequest httpServletRequest) {
        String token = TokenResolver.resolveBearerToken(httpServletRequest.getHeader(AUTHORIZATION_HEADER));
        if (!StringUtils.hasText(token)) {
            throw new BizException(ErrorCode.TOKEN_MISSING);
        }
        return token;
    }


    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private ClientDevice device;
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }

    @Data
    public static class LoginResponse {
        private Long userId;
        private String username;
        private String role;
        private TokenPair tokenPair;
    }
}
