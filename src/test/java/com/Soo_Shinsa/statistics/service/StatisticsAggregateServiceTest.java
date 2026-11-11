package com.Soo_Shinsa.statistics.service;

import com.Soo_Shinsa.statistics.repository.StatisticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsAggregateServiceTest {
    @Mock JobLauncher jobLauncher;
    @Mock Job stackOrderHistoryJob;
    @Mock StatisticsRepository statisticsRepository;
    @InjectMocks StatisticsAggregateService service;

    @Test
    void 기존_집계를_지운_뒤_날짜_파라미터로_잡을_돌리고_건수를_돌려준다() throws Exception {
        LocalDate d = LocalDate.of(2026, 11, 10);
        JobExecution ok = new JobExecution(1L); ok.setStatus(BatchStatus.COMPLETED);
        when(jobLauncher.run(eq(stackOrderHistoryJob), any())).thenReturn(ok);
        when(statisticsRepository.countByOrderDate(d)).thenReturn(7L);

        long rows = service.aggregate(d);

        assertThat(rows).isEqualTo(7L);
        InOrderHelper.verifyOrder(statisticsRepository, jobLauncher, d);
        ArgumentCaptor<JobParameters> params = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(stackOrderHistoryJob), params.capture());
        assertThat(params.getValue().getString("date")).isEqualTo("2026-11-10");
        assertThat(params.getValue().getLong("run")).isNotNull(); // 같은 날짜 재실행 허용
    }

    @Test
    void 잡이_실패하면_예외() throws Exception {
        JobExecution failed = new JobExecution(1L); failed.setStatus(BatchStatus.FAILED);
        when(jobLauncher.run(any(), any())).thenReturn(failed);
        assertThatThrownBy(() -> service.aggregate(LocalDate.now())).isInstanceOf(IllegalStateException.class);
        verify(statisticsRepository, never()).countByOrderDate(any());
    }

    static class InOrderHelper {
        static void verifyOrder(StatisticsRepository repo, JobLauncher launcher, LocalDate d) throws Exception {
            org.mockito.InOrder in = inOrder(repo, launcher);
            in.verify(repo).deleteByOrderDate(d);
            in.verify(launcher).run(any(), any());
        }
    }
}
