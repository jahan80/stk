package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.api.dto.CategoryResponse;
import com.starterkit.ticket.ticket.application.exception.CategoryNotFoundException;
import com.starterkit.ticket.ticket.domain.entity.TicketCategory;
import com.starterkit.ticket.ticket.domain.repository.TicketCategoryRepository;
import com.starterkit.ticket.ticket.domain.repository.TicketRepository;
import com.starterkit.ticket.ticket.application.exception.CategoryInUseException;
import com.starterkit.ticket.ticket.application.exception.CategoryAlreadyExistsException;
import com.starterkit.ticket.ticket.api.dto.UpdateCategoryRequest;
import com.starterkit.ticket.ticket.api.dto.CreateCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketCategoryService {

    private final TicketCategoryRepository repo;
    private final TicketRepository ticketRepo;

    // ===== READ =====

    public List<CategoryResponse> listEnabled() {
        return repo.findByEnabledTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse).toList();
    }

    public List<CategoryResponse> listAll() {
        return repo.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::toResponse).toList();
    }

    public CategoryResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    public TicketCategory getEntity(Long id) {
        return repo.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    // ===== WRITE =====

    @Transactional
    public CategoryResponse create(CreateCategoryRequest req) {
        if (repo.existsByCode(req.getCode())) {
            throw new CategoryAlreadyExistsException(req.getCode());
        }

        TicketCategory c = new TicketCategory();
        c.setCode(req.getCode());
        c.setName(req.getName());
        c.setDescription(req.getDescription());
        c.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        c.setEnabled(req.getEnabled() == null || req.getEnabled());

        return toResponse(repo.save(c));
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest req) {
        TicketCategory c = getEntity(id);

        // code is immutable (referenced externally)

        c.setName(req.getName());
        c.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) {
            c.setDisplayOrder(req.getDisplayOrder());
        }
        if (req.getEnabled() != null) {
            c.setEnabled(req.getEnabled());
        }

        return toResponse(repo.save(c));
    }

    @Transactional
    public CategoryResponse setEnabled(Long id, boolean enabled) {
        TicketCategory c = getEntity(id);
        c.setEnabled(enabled);
        return toResponse(repo.save(c));
    }

    /**
     * Hard delete. Only allowed if NO ticket uses this category.
     * Prefer `setEnabled(false)` for categories that are just
     * decommissioned — that keeps historical tickets intact.
     */
    @Transactional
    public void delete(Long id) {
        TicketCategory c = getEntity(id);

        long used = ticketRepo.countByCategoryId(id);
        if (used > 0) {
            throw new CategoryInUseException(id, used);
        }

        repo.delete(c);
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
