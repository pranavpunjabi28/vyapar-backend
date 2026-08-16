package com.bbu.vyaparbackend.shared;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class Pageables {
    private Pageables() {
    }

    public static Pageable requireAllowedSort(Pageable pageable, Set<String> allowed, Sort defaultSort) {
        Sort requested = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        for (Sort.Order order : requested) {
            if (!allowed.contains(order.getProperty())) {
                throw ApiException.invalid(ErrorMessages.Messages.INVALID_SORT + ": " + order.getProperty());
            }
        }
        if (requested.getOrderFor("id") == null && allowed.contains("id")) {
            requested = requested.and(Sort.by("id"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), requested);
    }
}
