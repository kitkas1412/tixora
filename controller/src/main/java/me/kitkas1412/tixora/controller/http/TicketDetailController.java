package me.kitkas1412.tixora.controller.http;

import lombok.extern.slf4j.Slf4j;
import me.kitkas1412.tixora.application.service.ticket.TicketDetailAppService;
import me.kitkas1412.tixora.controller.model.enums.ResultUtil;
import me.kitkas1412.tixora.controller.model.vo.ResultMessage;
import me.kitkas1412.tixora.domain.model.entity.TicketDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ticket")
@Slf4j
public class TicketDetailController {

    // CALL Service Application
    private final TicketDetailAppService ticketDetailAppService;

    public TicketDetailController(TicketDetailAppService ticketDetailAppService) {
        this.ticketDetailAppService = ticketDetailAppService;
    }

    @GetMapping("/{ticketId}/detail/{detailId}")
    public ResultMessage<TicketDetail> getTicketDetail(
            @PathVariable("ticketId") Long ticketId,
            @PathVariable("detailId") Long detailId
    ) {
        log.info("MEMBER TIPS GO");
        log.info(" ticketId:{}, detailId:{}", ticketId, detailId);
        return ResultUtil.data(ticketDetailAppService.getTicketDetailById(detailId));
    }
}
