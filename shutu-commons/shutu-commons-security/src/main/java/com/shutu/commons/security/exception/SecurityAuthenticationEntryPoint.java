package com.shutu.commons.security.exception;
import com.shutu.commons.tools.exception.ErrorCode;
import com.shutu.commons.tools.utils.JsonUtils;
import com.shutu.commons.tools.utils.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;

/**
 * 匿名用户(token不存在、错误)，异常处理器
 */
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(JsonUtils.toJsonString(new Result<>().error(ErrorCode.UNAUTHORIZED)));
    }
}