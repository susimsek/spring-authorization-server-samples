package io.github.susimsek.springauthserversamples.config.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.Scheduled;

class SessionCleanupSchedulerTest {

    @Test
    void afterPropertiesSetSchedulesCleanupWhenCronIsEnabled() {
        JpaIndexedSessionRepository repository = mock(JpaIndexedSessionRepository.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture)
                .when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        SessionCleanupScheduler scheduler =
                new SessionCleanupScheduler(repository, taskScheduler, "0 * * * * *");

        scheduler.afterPropertiesSet();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Trigger.class));
        runnableCaptor.getValue().run();
        verify(repository).cleanUpExpiredSessions();
    }

    @Test
    void afterPropertiesSetSkipsSchedulingWhenCronIsDisabled() {
        JpaIndexedSessionRepository repository = mock(JpaIndexedSessionRepository.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        SessionCleanupScheduler scheduler =
                new SessionCleanupScheduler(repository, taskScheduler, Scheduled.CRON_DISABLED);

        scheduler.afterPropertiesSet();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void destroyCancelsScheduledTaskWhenPresent() {
        JpaIndexedSessionRepository repository = mock(JpaIndexedSessionRepository.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture)
                .when(taskScheduler)
                .schedule(any(Runnable.class), any(Trigger.class));
        SessionCleanupScheduler scheduler =
                new SessionCleanupScheduler(repository, taskScheduler, "0 * * * * *");
        scheduler.afterPropertiesSet();

        scheduler.destroy();

        verify(scheduledFuture).cancel(false);
    }

    @Test
    void destroyDoesNothingWhenTaskWasNeverScheduled() {
        JpaIndexedSessionRepository repository = mock(JpaIndexedSessionRepository.class);
        SessionCleanupScheduler scheduler =
                new SessionCleanupScheduler(
                        repository, mock(TaskScheduler.class), Scheduled.CRON_DISABLED);

        scheduler.destroy();
    }
}
