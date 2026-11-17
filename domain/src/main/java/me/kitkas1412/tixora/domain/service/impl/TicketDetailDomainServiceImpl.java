package me.kitkas1412.tixora.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.kitkas1412.tixora.domain.model.entity.TicketDetail;
import me.kitkas1412.tixora.domain.repository.TicketDetailRepository;
import me.kitkas1412.tixora.domain.service.TicketDetailDomainService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TicketDetailDomainServiceImpl implements TicketDetailDomainService {
    private final TicketDetailRepository ticketDetailRepository;

    public TicketDetailDomainServiceImpl(TicketDetailRepository ticketDetailRepository) {
        this.ticketDetailRepository = ticketDetailRepository;
    }

    @Override
    public TicketDetail getTicketDetailById(Long ticketId) {
        log.info("Implement Domain : {}", ticketId);
        return ticketDetailRepository.findById(ticketId).orElse(null);
    }
}
