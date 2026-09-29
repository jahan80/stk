package com.starterkit.ticket.ticket.api;

import com.starterkit.ticket.shared.api.response.ApiCode;
import com.starterkit.ticket.shared.api.response.ApiResponse;
import com.starterkit.ticket.shared.api.response.ApiResponseFactory;
import com.starterkit.ticket.ticket.api.dto.CategoryResponse;
import com.starterkit.ticket.ticket.application.service.TicketCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/categories")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryService service;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return responseFactory.success(ApiCode.SUCCESS, service.listEnabled());
    }
}
