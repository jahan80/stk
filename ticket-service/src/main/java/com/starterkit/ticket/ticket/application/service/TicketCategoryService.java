package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.api.dto.CategoryResponse;
import com.starterkit.ticket.ticket.application.exception.CategoryNotFoundException;
import com.starterkit.ticket.ticket.domain.entity.TicketCategory;
import com.starterkit.ticket.ticket.domain.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketCategoryService {

    private final TicketCategoryRepository repo;

    public List<CategoryResponse> listEnabled() {
        return repo.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse).toList();
    }

    public List<CategoryResponse> listAll() {
        return repo.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse).toList();
    }

    public TicketCategory getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private CategoryResponse toResponse(TicketCategory c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .description(c.getDescription())
                .enabled(c.isEnabled())
                .displayOrder(c.getDisplayOrder())
                .build();
    }
}
