package com.realtime.notification.ratelimit;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String clientIp = getClientIp();
        String key = "ratelimit:" + clientIp + ":" + joinPoint.getSignature().getName();

        try {
            Long count = redisTemplate.opsForValue().increment(key);

            if (count == 1) {
                redisTemplate.expire(key, rateLimit.windowSeconds(), TimeUnit.SECONDS);
            }

            if (count > rateLimit.maxRequests()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Max " + rateLimit.maxRequests() +
                    " requests per " + rateLimit.windowSeconds() + " seconds.");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting, allowing request: {}", e.getMessage());
        }

        return joinPoint.proceed();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes.getRequest().getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}