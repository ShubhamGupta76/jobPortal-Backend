package com.job_Portal_Backend.job_portal_backend.aspect;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;

    @Pointcut("execution(* com.job_Portal_Backend.job_portal_backend.service.*.create*(..))")
    public void createOperations() {
    }

    @Pointcut("execution(* com.job_Portal_Backend.job_portal_backend.service.*.update*(..))")
    public void updateOperations() {
    }

    @Pointcut("execution(* com.job_Portal_Backend.job_portal_backend.service.*.delete*(..))")
    public void deleteOperations() {
    }

    @Pointcut("execution(* com.job_Portal_Backend.job_portal_backend.controller.*.login(..)) || " +
            "execution(* com.job_Portal_Backend.job_portal_backend.controller.*.register(..))")
    public void authOperations() {
    }

    @Around("createOperations()")
    public Object auditCreateOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditOperation(joinPoint, "CREATE");
    }

    @Around("updateOperations()")
    public Object auditUpdateOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditOperation(joinPoint, "UPDATE");
    }

    @Around("deleteOperations()")
    public Object auditDeleteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditOperation(joinPoint, "DELETE");
    }

    @Around("authOperations()")
    public Object auditAuthOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditOperation(joinPoint, "AUTH");
    }

    private Object auditOperation(ProceedingJoinPoint joinPoint, String action) throws Throwable {
        User currentUser = getCurrentUser();
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();

        // Extract entity information from method arguments
        Object[] args = joinPoint.getArgs();
        String entityType = extractEntityType(joinPoint);
        Long entityId = extractEntityId(args);

        String description = String.format("%s operation on %s", action, entityType);

        try {
            Object result = joinPoint.proceed();

            // Log successful operation
            if (currentUser != null) {
                auditLogService.logAction(entityType, entityId, action, currentUser,
                        description, ipAddress, userAgent);
            }

            return result;

        } catch (Exception e) {
            // Log failed operation
            if (currentUser != null) {
                auditLogService.logAction(entityType, entityId, action + "_FAILED", currentUser,
                        description + " - Failed: " + e.getMessage(), ipAddress, userAgent);
            }
            throw e;
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    private String getClientIpAddress() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }

    private String getUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("User-Agent");
        }
        return "unknown";
    }

    private String extractEntityType(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        if (className.endsWith("Service")) {
            return className.substring(0, className.length() - 7); // Remove "Service"
        }
        return className;
    }

    private Long extractEntityId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
            // Try to find ID in entity objects
            if (arg != null && hasIdField(arg)) {
                try {
                    return (Long) arg.getClass().getMethod("getId").invoke(arg);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        return null;
    }

    private boolean hasIdField(Object obj) {
        try {
            obj.getClass().getMethod("getId");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}