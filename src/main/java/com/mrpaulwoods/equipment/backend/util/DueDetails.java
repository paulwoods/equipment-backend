package com.mrpaulwoods.equipment.backend.util;

import com.mrpaulwoods.equipment.backend.model.Perform;
import com.mrpaulwoods.equipment.backend.model.Procedure;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;

public record DueDetails(int daysTillDue, LocalDate dueDate) {

    public static Optional<DueDetails> calculate(Procedure proc) {
        if (proc.getHistory() == null || proc.getHistory().isEmpty()) {
            return Optional.empty();
        }

        LocalDate latest = proc.getHistory().stream()
                .map(Perform::getDate)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        long daysSince = ChronoUnit.DAYS.between(latest, LocalDate.now());
        int daysTillDue = proc.getIntervalDays() - (int) daysSince;
        LocalDate dueDate = LocalDate.now().plusDays(daysTillDue);

        return Optional.of(new DueDetails(daysTillDue, dueDate));
    }

    public String status() {
        return daysTillDue <= 0 ? "OVERDUE" : "Upcoming";
    }
}
