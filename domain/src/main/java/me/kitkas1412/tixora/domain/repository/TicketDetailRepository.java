package me.kitkas1412.tixora.domain.repository;

import me.kitkas1412.tixora.domain.model.entity.TicketDetail;

import java.util.List;
import java.util.Optional;

public interface TicketDetailRepository {

    Optional<TicketDetail> findById(Long id);

    List<TicketDetail> findAll();

    TicketDetail save(TicketDetail ticketDetail);
}
