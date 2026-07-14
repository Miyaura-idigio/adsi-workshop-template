package com.example.attendance.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WorkDurationTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 7, 14, 9, 0);

    @Test
    @DisplayName("退勤未打刻の場合、全て0を返す")
    void nullClockOut_returnsZero() {
        var duration = new WorkDuration(BASE, null);
        assertThat(duration.totalMinutes()).isZero();
        assertThat(duration.breakMinutes()).isZero();
        assertThat(duration.actualMinutes()).isZero();
        assertThat(duration.overtimeMinutes()).isZero();
    }

    @Test
    @DisplayName("6時間以下の場合、休憩控除なし")
    void lessThan6Hours_noBreak() {
        var duration = new WorkDuration(BASE, BASE.plusHours(5));
        assertThat(duration.totalMinutes()).isEqualTo(300);
        assertThat(duration.breakMinutes()).isZero();
        assertThat(duration.actualMinutes()).isEqualTo(300);
    }

    @Test
    @DisplayName("ちょうど6時間の場合、休憩控除なし")
    void exactly6Hours_noBreak() {
        var duration = new WorkDuration(BASE, BASE.plusHours(6));
        assertThat(duration.totalMinutes()).isEqualTo(360);
        assertThat(duration.breakMinutes()).isZero();
        assertThat(duration.actualMinutes()).isEqualTo(360);
    }

    @Test
    @DisplayName("6時間超8時間以下の場合、45分休憩控除")
    void over6HoursUnder8Hours_45minBreak() {
        var duration = new WorkDuration(BASE, BASE.plusHours(7));
        assertThat(duration.totalMinutes()).isEqualTo(420);
        assertThat(duration.breakMinutes()).isEqualTo(45);
        assertThat(duration.actualMinutes()).isEqualTo(375);
    }

    @Test
    @DisplayName("ちょうど8時間の場合、45分休憩控除")
    void exactly8Hours_45minBreak() {
        var duration = new WorkDuration(BASE, BASE.plusHours(8));
        assertThat(duration.totalMinutes()).isEqualTo(480);
        assertThat(duration.breakMinutes()).isEqualTo(45);
        assertThat(duration.actualMinutes()).isEqualTo(435);
    }

    @Test
    @DisplayName("8時間超の場合、60分休憩控除")
    void over8Hours_60minBreak() {
        var duration = new WorkDuration(BASE, BASE.plusHours(9));
        assertThat(duration.totalMinutes()).isEqualTo(540);
        assertThat(duration.breakMinutes()).isEqualTo(60);
        assertThat(duration.actualMinutes()).isEqualTo(480);
    }

    @Test
    @DisplayName("残業なし: 実勤務が8時間以下")
    void noOvertime() {
        var duration = new WorkDuration(BASE, BASE.plusHours(9));
        assertThat(duration.overtimeMinutes()).isZero();
    }

    @Test
    @DisplayName("残業あり: 実勤務が8時間超")
    void withOvertime() {
        var duration = new WorkDuration(BASE, BASE.plusHours(10));
        assertThat(duration.totalMinutes()).isEqualTo(600);
        assertThat(duration.breakMinutes()).isEqualTo(60);
        assertThat(duration.actualMinutes()).isEqualTo(540);
        assertThat(duration.overtimeMinutes()).isEqualTo(60);
    }
}
