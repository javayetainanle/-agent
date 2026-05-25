package cn.yangeit.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginRequestDto {

    @Schema(description = "用户名",example = "张三",required = true)
    private String nickName;

    @Schema(description = "状态码",example = "123456",required = true)
    private String code;

    @Schema(description = "手机号",example = "12345678901",required = true)
    private String phoneCode;
}