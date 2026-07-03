package com.dawn.cinema.model;

import com.dawn.common.core.model.AbstractMappedEntity;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Hidden
@Entity
@Table(name = "seat_instance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SeatInstance extends AbstractMappedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_template_id", nullable = false)
    private Long seatTemplateId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "price", nullable = false)
    private BigDecimal price;
}
