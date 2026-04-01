package io.springlens.agent.starter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanWrapperImpl;

public class AgentOverlayValueResolver {

    public Object resolveMethodExpression(String expression, Object[] args, Object result, Throwable error) {
        String normalized = normalize(expression);
        if (normalized == null) {
            return fallbackValue(args, result, error);
        }
        if ("result".equals(normalized)) {
            return result;
        }
        if (normalized.startsWith("result.")) {
            return readPath(result, normalized.substring("result.".length()));
        }
        if ("exception".equals(normalized)) {
            return error == null ? null : error.toString();
        }
        if ("exception.message".equals(normalized)) {
            return error == null ? null : error.getMessage();
        }
        if (normalized.startsWith("args[")) {
            return resolveArgumentExpression(normalized, args);
        }
        return fallbackValue(args, result, error);
    }

    public Object resolveHttpExpression(
            String expression,
            HttpServletRequest request,
            HttpServletResponse response,
            Exception error
    ) {
        String normalized = normalize(expression);
        if (normalized == null || "request.path".equals(normalized)) {
            return request.getRequestURI();
        }
        if ("request.method".equals(normalized)) {
            return request.getMethod();
        }
        if ("response.status".equals(normalized)) {
            return response.getStatus();
        }
        if ("exception.message".equals(normalized)) {
            return error == null ? null : error.getMessage();
        }
        return request.getRequestURI();
    }

    private Object resolveArgumentExpression(String expression, Object[] args) {
        int endBracket = expression.indexOf(']');
        if (endBracket < 0) {
            return args;
        }
        int index = Integer.parseInt(expression.substring(5, endBracket));
        if (index < 0 || index >= args.length) {
            return null;
        }
        Object argument = args[index];
        if (endBracket == expression.length() - 1) {
            return argument;
        }
        String path = expression.substring(endBracket + 2);
        return readPath(argument, path);
    }

    private Object readPath(Object value, String path) {
        if (value == null) {
            return null;
        }
        if (path == null || path.isBlank()) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            return map.get(path);
        }
        if (value instanceof List<?> list) {
            int index = Integer.parseInt(path);
            return index >= 0 && index < list.size() ? list.get(index) : null;
        }
        return new BeanWrapperImpl(value).getPropertyValue(path);
    }

    private Object fallbackValue(Object[] args, Object result, Throwable error) {
        if (result != null) {
            return result;
        }
        if (error != null) {
            return error.getMessage();
        }
        return args.length == 1 ? args[0] : args;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
