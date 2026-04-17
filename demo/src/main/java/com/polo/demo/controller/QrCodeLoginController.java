package com.polo.demo.controller;

import com.polo.boot.api.doc.annotation.ApiOperation;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.core.model.Result;
import com.polo.boot.security.annotation.CurrentUser;
import com.polo.boot.security.annotation.RequireRole;
import com.polo.boot.security.model.TokenPair;
import com.polo.boot.security.service.QrCodeService;
import com.polo.demo.security.DemoLoginPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/qrcode")
public class QrCodeLoginController {
    @Autowired
    private QrCodeService qrCodeService;

    // ==========================================
    // ==== PC端接口：获取验证码和长轮询状态 ====
    // ==========================================
    @GetMapping("/generate")
    @ApiOperation(value = "请求生成登录二维码", description = "供PC端调用，返回唯一的UUID")
    public Result<String> generate() {
        return Result.success(qrCodeService.generateQrCode());
    }

    @GetMapping("/check")
    @ApiOperation(value = "轮询二维码状态", description = "PC端定时轮询或通过SSE获取状态变化")
    public Result<Map<String, Object>> check(@RequestParam String uuid) {
        return Result.success(qrCodeService.checkStatus(uuid));
    }

    // ==========================================
    // ==== App端接口：手机端需要带凭证登录 ====
    // ==========================================
    @RequireRole("admin")
    @PostMapping("/scan")
    @ApiOperation(value = "App扫描二维码", description = "App调起摄像头扫描并告知服务端已扫描")
    public Result<Void> scan(@RequestParam String uuid, @CurrentUser DemoLoginPrincipal user) {
        qrCodeService.scan(uuid, user);
        return Result.success(null);
    }
    @RequireRole("admin")
    @PostMapping("/confirm")
    @ApiOperation(value = "App确认登录授权", description = "App端用户点击确认授权登录按钮")
    public Result<AuthController.LoginResponse> confirm(@RequestParam String uuid, @CurrentUser DemoLoginPrincipal user) {
        TokenPair tokenPair = qrCodeService.confirm(uuid, user);

        AuthController.LoginResponse response = new AuthController.LoginResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setTokenPair(tokenPair);
        return Result.success(response);
    }
}
