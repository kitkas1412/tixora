package me.kitkas1412.tixora.application.service.ticket;

import me.kitkas1412.tixora.domain.model.entity.TicketDetail;

public interface TicketDetailAppService {

    TicketDetail getTicketDetailById(Long ticketId); // should convert to TickDetailDTO by Application Module
}
