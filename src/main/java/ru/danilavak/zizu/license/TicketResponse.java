package ru.danilavak.zizu.license;

public record TicketResponse(
        Ticket ticket,
        String signature
) {
}
