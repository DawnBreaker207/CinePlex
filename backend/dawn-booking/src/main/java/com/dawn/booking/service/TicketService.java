package com.dawn.booking.service;

import com.dawn.booking.model.Reservation;
import com.dawn.cinema.dto.response.SeatResponse;

import java.util.List;

public interface TicketService {
    void createTickets(Reservation reservation, List<SeatResponse> seats);
}
