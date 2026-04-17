package com.ticketbuy.kafka;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ticketbuy.entity.Invoice;
import com.ticketbuy.entity.Order;
import com.ticketbuy.repository.InvoiceMapper;
import com.ticketbuy.repository.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceMapper invoiceMapper;
    private final OrderMapper orderMapper;

    public InvoiceService(InvoiceMapper invoiceMapper, OrderMapper orderMapper) {
        this.invoiceMapper = invoiceMapper;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public Invoice createInvoice(Long orderId, Long userId, Integer titleType, String title, String taxNo, String email) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        Invoice invoice = new Invoice();
        invoice.setOrderId(orderId);
        invoice.setUserId(userId);
        invoice.setTitleType(titleType);
        invoice.setTitle(title);
        invoice.setTaxNo(taxNo);
        invoice.setEmail(email);
        invoice.setAmount(order.getTotalAmount());
        invoice.setStatus(1);

        invoiceMapper.insert(invoice);
        return invoice;
    }

    @Transactional
    public void issueInvoice(Long invoiceId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new RuntimeException("发票不存在");
        }

        invoice.setInvoiceNo("INV" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        invoice.setStatus(2);
        invoice.setIssuedAt(LocalDateTime.now());

        invoiceMapper.updateById(invoice);
        sendInvoiceEmail(invoice);
    }

    private void sendInvoiceEmail(Invoice invoice) {
        log.info("Sending invoice email to: {}, invoiceNo: {}", invoice.getEmail(), invoice.getInvoiceNo());
    }
}
