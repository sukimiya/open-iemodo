package com.iemodo.common.context;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * 反应式用户上下文工具类。
 * 
 * <p>在 WebFlux 反应式环境中，由于不能使用 ThreadLocal，
 * 需要使用 Reactor Context 来传递当前用户ID。
 *
 * <p>使用示例：
 * <pre>
 * // 在 Filter 中设置用户ID
 * return chain.filter(exchange)
 *     .contextWrite(ReactiveUserContext.withUserId(userId));
 *
 * // 在 Service 或 Callback 中获取用户ID
 * ReactiveUserContext.getCurrentUserId()
 *     .subscribe(userId -> {...});
 * </pre>
 */
public class ReactiveUserContext {

    /** Context key for user ID */
    private static final String USER_ID_KEY = "USER_ID";

    /**
     * 获取当前用户ID
     *
     * @return Mono<Long> 用户ID，如果没有则返回 0L
     */
    public static Mono<Long> getCurrentUserId() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(USER_ID_KEY)) {
                return Mono.just(ctx.get(USER_ID_KEY));
            }
            return Mono.just(0L); // 默认返回0（系统用户）
        });
    }

    /**
     * 创建包含用户ID的 Context
     *
     * @param userId 用户ID
     * @return Context
     */
    public static Context withUserId(Long userId) {
        return Context.of(USER_ID_KEY, userId != null ? userId : 0L);
    }

    /**
     * 创建包含用户ID的 Context（添加到现有 Context）
     *
     * @param userId 用户ID
     * @return Context
     */
    public static Context putUserId(Context context, Long userId) {
        return context.put(USER_ID_KEY, userId != null ? userId : 0L);
    }

    /**
     * 检查当前 Context 是否包含用户ID
     */
    public static Mono<Boolean> hasUserId() {
        return Mono.deferContextual(ctx -> Mono.just(ctx.hasKey(USER_ID_KEY)));
    }
}
