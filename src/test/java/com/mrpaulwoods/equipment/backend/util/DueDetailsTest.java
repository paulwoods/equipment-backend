package com.mrpaulwoods.equipment.backend.util;

import com.mrpaulwoods.equipment.backend.model.Perform;
import com.mrpaulwoods.equipment.backend.model.Procedure;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DueDetailsTest {

    private Procedure procedureWithHistory(int intervalDays, LocalDate... performDates) {
        Procedure proc = new Procedure();
        proc.setIntervalDays(intervalDays);
        List<Perform> history = new java.util.ArrayList<>();
        for (LocalDate date : performDates) {
            Perform p = new Perform();
            p.setDate(date);
            history.add(p);
        }
        proc.setHistory(history);
        return proc;
    }

    @Test
    void calculate_withNoHistory_returnsEmpty() {
        Procedure proc = new Procedure();
        proc.setIntervalDays(30);
        proc.setHistory(List.of());

        assertThat(DueDetails.calculate(proc)).isEmpty();
    }

    @Test
    void calculate_withNullHistory_returnsEmpty() {
        Procedure proc = new Procedure();
        proc.setIntervalDays(30);
        proc.setHistory(null);

        assertThat(DueDetails.calculate(proc)).isEmpty();
    }

    @Test
    void calculate_dueToday_returnsZeroDaysTillDue() {
        Procedure proc = procedureWithHistory(30, LocalDate.now().minusDays(30));

        Optional<DueDetails> result = DueDetails.calculate(proc);

        assertThat(result).isPresent();
        assertThat(result.get().daysTillDue()).isEqualTo(0);
        assertThat(result.get().dueDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void calculate_dueInFuture_returnsPositiveDaysTillDue() {
        Procedure proc = procedureWithHistory(30, LocalDate.now().minusDays(20));

        Optional<DueDetails> result = DueDetails.calculate(proc);

        assertThat(result).isPresent();
        assertThat(result.get().daysTillDue()).isEqualTo(10);
        assertThat(result.get().dueDate()).isEqualTo(LocalDate.now().plusDays(10));
    }

    @Test
    void calculate_overdue_returnsNegativeDaysTillDue() {
        Procedure proc = procedureWithHistory(30, LocalDate.now().minusDays(35));

        Optional<DueDetails> result = DueDetails.calculate(proc);

        assertThat(result).isPresent();
        assertThat(result.get().daysTillDue()).isEqualTo(-5);
    }

    @Test
    void calculate_usesLatestPerformDate() {
        // oldest date would make it look overdue; latest makes it upcoming
        Procedure proc = procedureWithHistory(30,
                LocalDate.now().minusDays(50),
                LocalDate.now().minusDays(10));

        Optional<DueDetails> result = DueDetails.calculate(proc);

        assertThat(result).isPresent();
        assertThat(result.get().daysTillDue()).isEqualTo(20);
    }

    @Test
    void status_whenOverdue_returnsOVERDUE() {
        DueDetails due = new DueDetails(-1, LocalDate.now().minusDays(1));
        assertThat(due.status()).isEqualTo("OVERDUE");
    }

    @Test
    void status_whenDueToday_returnsOVERDUE() {
        DueDetails due = new DueDetails(0, LocalDate.now());
        assertThat(due.status()).isEqualTo("OVERDUE");
    }

    @Test
    void status_whenUpcoming_returnsUpcoming() {
        DueDetails due = new DueDetails(5, LocalDate.now().plusDays(5));
        assertThat(due.status()).isEqualTo("Upcoming");
    }
}
