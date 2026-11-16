package me.kitkas1412.tixora.infrastructure.persistence.mapper;

import me.kitkas1412.tixora.domain.model.entity.TicketDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketDetailJpaRepository extends JpaRepository<TicketDetail, Long> {
}
