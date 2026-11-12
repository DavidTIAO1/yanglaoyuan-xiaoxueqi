package com.automedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 药品实体 — 无 createdAt/updatedAt 字段
 */
@Data
@Entity
@Table(name = "medicines")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "generic_name", length = 100)
    private String genericName;

    @Column(length = 50)
    private String category;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column
    private Integer stock = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "requires_prescription")
    private Boolean requiresPrescription = false;

    @Column
    private Integer status = 1;
}
