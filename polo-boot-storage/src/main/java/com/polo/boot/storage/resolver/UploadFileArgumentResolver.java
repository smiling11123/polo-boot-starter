package com.polo.boot.storage.resolver;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.storage.annotation.UploadFile;
import com.polo.boot.storage.context.UploadContext;
import com.polo.boot.storage.model.UploadOptions;
import com.polo.boot.storage.model.UploadResult;
import com.polo.boot.storage.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;

@RequiredArgsConstructor
public class UploadFileArgumentResolver implements HandlerMethodArgumentResolver {

    private final FileUploadService fileUploadService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(UploadFile.class)) {
            return false;
        }
        Class<?> parameterType = parameter.getParameterType();
        if (UploadResult.class.equals(parameterType)) {
            return true;
        }
        if (!List.class.equals(parameterType)) {
            return false;
        }
        return UploadResult.class.equals(ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        UploadFile annotation = parameter.getParameterAnnotation(UploadFile.class);
        if (annotation == null) {
            return null;
        }
        Object nativeRequest = webRequest.getNativeRequest();
        if (!(nativeRequest instanceof MultipartHttpServletRequest multipartRequest)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "当前请求不是 multipart/form-data");
        }

        String fieldName = resolveFieldName(parameter, annotation);
        UploadOptions options = UploadOptions.from(annotation, fieldName);

        if (List.class.equals(parameter.getParameterType())) {
            List<MultipartFile> files = multipartRequest.getFiles(fieldName);
            if (files.isEmpty()) {
                if (annotation.required()) {
                    throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "缺少上传文件字段: " + fieldName);
                }
                return List.of();
            }
            List<UploadResult> results = fileUploadService.upload(files, options);
            UploadContext.add(fieldName, results);
            return results;
        }

        MultipartFile file = multipartRequest.getFile(fieldName);
        if (file == null || file.isEmpty()) {
            if (annotation.required()) {
                throw new BizException(ErrorCode.PARAM_MISSING.getCode(), "缺少上传文件字段: " + fieldName);
            }
            return null;
        }
        UploadResult result = fileUploadService.upload(file, options);
        UploadContext.add(fieldName, List.of(result));
        return result;
    }

    private String resolveFieldName(MethodParameter parameter, UploadFile annotation) {
        if (StringUtils.hasText(annotation.value())) {
            return annotation.value();
        }
        if (StringUtils.hasText(parameter.getParameterName())) {
            return parameter.getParameterName();
        }
        return "file";
    }
}
