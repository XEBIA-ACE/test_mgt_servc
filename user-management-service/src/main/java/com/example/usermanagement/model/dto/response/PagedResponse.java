package com.example.usermanagement.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Wraps a paginated list of items with standard pagination metadata. */
@Data
@Builder
public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
}
