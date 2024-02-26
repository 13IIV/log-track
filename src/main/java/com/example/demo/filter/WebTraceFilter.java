package com.example.demo.filter;

import com.example.demo.utils.MDCTraceUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class WebTraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = httpServletRequest.getHeader(MDCTraceUtils.TRACE_ID_HEADER);
            //请求上下文没有traceId 添加一个 有就将该traceId放入MDC中，由MDC传递
            if (StringUtils.isEmpty(traceId)) {
                MDCTraceUtils.addTrace();
            } else {
                MDCTraceUtils.putTrace(traceId);
            }
            filterChain.doFilter(httpServletRequest,httpServletResponse);
        } finally {
            //每次请求结束后 回收traceId 避免在线程复用的情况下 traceId会乱窜
            MDCTraceUtils.removeTrace();
        }
    }
}
