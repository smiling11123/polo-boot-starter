package com.polo.boot.core.model;

import com.polo.boot.core.constant.ErrorCode;
import lombok.Data;

import java.util.List;

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    //分页属性
    private Long totalPage;
    private Long page;
    private Long size;

    /**
     * 普通返回响应
     * @param data 封装数据
     * @return 返回封装后的数据
     * @param <T> 泛型
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMsg(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 包装分页属性返回响应
     * @param data 封装数据
     * @param totalPage 总页数
     * @param page 当前页数
     * @param size 每页大小
     * @return 返回包含分页属性的数据
     * @param <T> 泛型
     */
    public static <T> Result<List<T>> success(List<T> data, Long totalPage, Long page, Long size) {
        Result<List<T>> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMsg(ErrorCode.SUCCESS.getMessage());
        result.setData(data);
        result.setTotalPage(totalPage);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    /**
     * 错误返回响应
     * @param code 错误类型码
     * @param msg 错误信息
     * @return 返回错误响应
     * @param <T> 泛型
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

}
