package me.kitkas1412.tixora.infrastructure.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import me.kitkas1412.tixora.domain.model.entity.TicketDetail;
import me.kitkas1412.tixora.domain.repository.TicketDetailRepository;
import me.kitkas1412.tixora.infrastructure.persistence.mapper.TicketDetailJPAMapper;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TicketDetailInfrasRepositoryImpl implements TicketDetailRepository {

    private final TicketDetailJPAMapper ticketDetailJPAMapper;

    public TicketDetailInfrasRepositoryImpl(TicketDetailJPAMapper ticketDetailJPAMapper) {
        this.ticketDetailJPAMapper = ticketDetailJPAMapper;
    }

    @Override
    public Optional<TicketDetail> findById(Long id) {
        log.info("Implement Infrastructure : {}", id);
        return ticketDetailJPAMapper.findById(id);
    }

    @Override
    public List<TicketDetail> findAll() {
        return List.of();
    }

    @Override
    public TicketDetail save(TicketDetail ticketDetail) {
        return null;
    }
}
