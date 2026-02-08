package com.example.utils.modal;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExcelReadModal {
    private List<String> titles;
    private List<List<Map<String, Object>>> dataList;
}
