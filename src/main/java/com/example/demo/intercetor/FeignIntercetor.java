package com.example.demo.intercetor;

import com.example.demo.utils.MDCTraceUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Objects;

@Component
public class FeignIntercetor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(requestAttributes!=null) {
            HttpServletRequest request = requestAttributes.getRequest();
            Enumeration<String> headerNames = request.getHeaderNames();
            if(headerNames!=null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    if(Objects.equals("content-length",name)) {
                        continue;
                    }
                    String value = request.getHeader(name);
                    requestTemplate.header(name,value);
                }
            }
        }
        // 传递日志追踪traceId
        String traceId = MDCTraceUtils.getTraceId();
        if (!StringUtils.isEmpty(traceId)) {
            requestTemplate.header(MDCTraceUtils.TRACE_ID_HEADER,traceId);
        }
    }
}
