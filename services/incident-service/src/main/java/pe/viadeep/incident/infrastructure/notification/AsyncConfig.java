package pe.viadeep.incident.infrastructure.notification;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Hilos en segundo plano para enviar los avisos sin demorar la respuesta al usuario. */
@Configuration
@EnableAsync
class AsyncConfig {

    @Bean
    Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("notifications-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.initialize();
        return executor;
    }
}
