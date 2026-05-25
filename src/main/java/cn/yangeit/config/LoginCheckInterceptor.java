package cn.yangeit.config;


import cn.hutool.core.util.ObjectUtil;
import cn.yangeit.common.BaseException;
import cn.yangeit.common.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

//自定义拦截器
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {
    //目标资源方法执行前执行。 返回true：放行    返回false：不放行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("preHandle .... ");
        //判断是否是拦截的方法
        if (!(handler instanceof HandlerMethod)){
            System.out.println("1");
        return true; //true表示放行
        }
        //获取token
        String token = request.getHeader("authorization");
        if(ObjectUtil.isEmpty(token)){
            throw new BaseException("请登录");
        }
        //解析token
        Claims claims = JwtUtils.parseJWT(token);
        Long userId = Long.valueOf(claims.get("userId").toString());
        if(ObjectUtil.isEmpty(userId)){
            throw new BaseException("认证失败");
        }
        BaseContext.setCurrentId(userId);
        return true;
    }

    //目标资源方法执行后执行
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle ... ");
    }

    //视图渲染完毕后执行，最后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
        System.out.println("afterCompletion .... ");
    }
}

