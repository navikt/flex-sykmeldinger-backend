package no.nav.helse.flex.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.Executors

@Configuration
@EnableAsync
class SchedulerConfig {
    @Bean
    @Primary
    fun taskScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 5
            setThreadNamePrefix("flex-sykmeldinger-backend-scheduled-task-")
            initialize()
        }

    @Bean
    fun virtualThreadExecutor(): ConcurrentTaskExecutor =
        ConcurrentTaskExecutor(
            Executors.newVirtualThreadPerTaskExecutor(),
        )

    @Bean
    fun fixedThreadPool(): ConcurrentTaskExecutor =
        ConcurrentTaskExecutor(
            Executors.newFixedThreadPool(10),
        )
}
