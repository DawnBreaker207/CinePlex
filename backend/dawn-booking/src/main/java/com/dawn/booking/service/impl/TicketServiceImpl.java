package com.dawn.booking.service.impl;

import com.dawn.booking.model.Reservation;
import com.dawn.booking.model.Ticket;
import com.dawn.booking.repository.TicketRepository;
import com.dawn.booking.service.TicketService;
import com.dawn.cinema.dto.response.SeatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private static final String DEFAULT_TICKET_TYPE = "NORMAL";

    private final TicketRepository ticketRepository;

    @Override
    public void createTickets(Reservation reservation, List<SeatResponse> seats) {
        if (seats == null || seats.isEmpty()) return;

        List<Ticket> tickets = seats.stream()
                .<Ticket>map(seat -> Ticket.builder()
                        .reservationId(reservation.getId())
                        .seatInstanceId(seat.getId())
                        .ticketType(seat.getSeatType() != null ? seat.getSeatType() : DEFAULT_TICKET_TYPE)
                        .price(seat.getPrice())
                        .build())
                .toList();

        ticketRepository.saveAll(tickets);
        log.info("Created {} tickets for reservation {}", tickets.size(), reservation.getReservationCode());
    }
}
