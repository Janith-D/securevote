package com.securevote.securevotebackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequestDto {
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}
