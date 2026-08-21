package io.github.susimsek.springauthserversamples.config.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

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
        JpaIndexedSessionRepository repository =
                new JpaIndexedSessionRepository(
                        mock(UserSessionRepository.class), new NoOpTransactionManager());
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        SessionCleanupScheduler scheduler =
                new SessionCleanupScheduler(repository, taskScheduler, Scheduled.CRON_DISABLED);

        scheduler.afterPropertiesSet();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void destroyCancelsScheduledTaskWhenPresent() {
        JpaIndexedSessionRepository repository =
                new JpaIndexedSessionRepository(
                        mock(UserSessionRepository.class), new NoOpTransactionManager());
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
        JpaIndexedSessionRepository repository =
                new JpaIndexedSessionRepository(
                        mock(UserSessionRepository.class), new NoOpTransactionManager());
        SessionCleanupScheduler scheduler =
                new SessionCleanupScheduler(
                        repository, mock(TaskScheduler.class), Scheduled.CRON_DISABLED);

        scheduler.destroy();
    }

    private static final class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {}

        @Override
        public void rollback(TransactionStatus status) {}
    }
}
