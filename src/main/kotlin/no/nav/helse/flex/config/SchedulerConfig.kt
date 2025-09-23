package no.nav.helse.flex.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.support.TaskExecutorAdapter
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
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
    fun virtualThreadExecutor(): TaskExecutor =
        TaskExecutorAdapter(
            Executors.newVirtualThreadPerTaskExecutor(),
        )
}
