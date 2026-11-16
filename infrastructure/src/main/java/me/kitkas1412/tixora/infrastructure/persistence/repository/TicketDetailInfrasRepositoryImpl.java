package me.kitkas1412.tixora.infrastructure.persistence.repository;

import me.kitkas1412.tixora.domain.model.entity.TicketDetail;
import me.kitkas1412.tixora.domain.repository.TicketDetailRepository;
import me.kitkas1412.tixora.infrastructure.persistence.mapper.TicketDetailJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TicketDetailInfrasRepositoryImpl implements TicketDetailRepository {

    private final TicketDetailJpaRepository ticketDetailJpaRepository;

    public TicketDetailInfrasRepositoryImpl(TicketDetailJpaRepository ticketDetailJpaRepository) {
        this.ticketDetailJpaRepository = ticketDetailJpaRepository;
    }

    @Override
    public Optional<TicketDetail> findById(Long id) {
        return ticketDetailJpaRepository.findById(id);
    }

    @Override
    public List<TicketDetail> findAll() {
        return ticketDetailJpaRepository.findAll();
    }

    @Override
    public TicketDetail save(TicketDetail ticketDetail) {
        return ticketDetailJpaRepository.save(ticketDetail);
    }
}
