package me.kitkas1412.tixora.application.service.ticket.impl;

import lombok.extern.slf4j.Slf4j;
import me.kitkas1412.tixora.application.service.ticket.TicketDetailAppService;
import me.kitkas1412.tixora.application.service.ticket.cache.TicketDetailCacheService;
import me.kitkas1412.tixora.domain.model.entity.TicketDetail;
import me.kitkas1412.tixora.domain.service.TicketDetailDomainService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TicketDetailAppServiceImpl implements TicketDetailAppService {

    private final TicketDetailDomainService ticketDetailDomainService;

    private final TicketDetailCacheService ticketDetailCacheService;

    public TicketDetailAppServiceImpl(TicketDetailDomainService ticketDetailDomainService, TicketDetailCacheService ticketDetailCacheService) {
        this.ticketDetailDomainService = ticketDetailDomainService;
        this.ticketDetailCacheService = ticketDetailCacheService;
    }


    @Override
    public TicketDetail getTicketDetailById(Long ticketId) {
        log.info("Implement Application : {}", ticketId);
//        return ticketDetailDomainService.getTicketDetailById(ticketId);
//        return ticketDetailCacheService.getTicketDefaultCacheNormal(ticketId, System.currentTimeMillis());
        return ticketDetailCacheService.getTicketDefaultCacheVip(ticketId, System.currentTimeMillis());
    }
}
