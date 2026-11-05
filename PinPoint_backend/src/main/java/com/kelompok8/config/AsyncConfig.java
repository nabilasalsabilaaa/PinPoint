package com.kelompok8.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Konfigurasi pemrosesan asinkron (@Async).
 *
 * <p>Menyediakan penangan error global (AsyncUncaughtExceptionHandler)
 * agar setiap exception yang terjadi di thread background (seperti EmailService)
 * tidak ditelan secara diam-diam (silent swallow), melainkan langsung dicatat
 * ke logger dan dicetak ke konsol terminal.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... params) -> {
            logger.error("❌ [ASYNC UNCAUGHT ERROR] Terjadi kegagalan pada background thread {}.{}() dengan parameter {}: {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    Arrays.toString(params),
                    throwable.getMessage(),
                    throwable);

            System.err.println("\n══════════════════════════════════════════════════════════════════════════");
            System.err.println("❌ [ASYNC THREAD FAILURE]");
            System.err.println("Method   : " + method.getDeclaringClass().getName() + "." + method.getName());
            System.err.println("Params   : " + Arrays.toString(params));
            System.err.println("Exception: " + throwable.getClass().getName());
            System.err.println("Message  : " + throwable.getMessage());
            System.err.println("══════════════════════════════════════════════════════════════════════════\n");
        };
    }
}
