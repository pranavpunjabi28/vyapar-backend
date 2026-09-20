package com.bbu.vyaparbackend.report;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.order.SalesOrder;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "dashboard_event", uniqueConstraints = @UniqueConstraint(name = "uk_dashboard_event_idempotency", columnNames = "idempotency_key"))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("dashevent")
class DashboardEvent extends BaseEntity {

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private Outlet outlet;
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	private SalesOrder order;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private DashboardEventType eventType;
	@Column(nullable = false)
	private long orderRevision;
	@Column(nullable = false, length = 100)
	private String idempotencyKey;
	private Instant processedAt;
	@Column(nullable = false)
	private int attemptCount;
	@Column(nullable = false)
	private Instant nextAttemptAt;
	@Column(length = 100)
	private String lastFailureCode;
}
