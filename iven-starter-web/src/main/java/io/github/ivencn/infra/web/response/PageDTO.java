package io.github.ivencn.infra.web.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应 DTO，作为分页接口的统一返回类型（禁止直接暴露 Spring Data {@link Page}）。
 *
 * @param <T>
 *            记录类型
 */
public class PageDTO<T> {

    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int number;
    private final int size;
    private final int numberOfElements;
    private final boolean first;
    private final boolean last;
    private final boolean empty;

    @JsonCreator
    public PageDTO(
            @JsonProperty("content") List<T> content,
            @JsonProperty("totalElements") long totalElements,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("number") int number,
            @JsonProperty("size") int size,
            @JsonProperty("numberOfElements") int numberOfElements,
            @JsonProperty("first") boolean first,
            @JsonProperty("last") boolean last,
            @JsonProperty("empty") boolean empty) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.number = number;
        this.size = size;
        this.numberOfElements = numberOfElements;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }

    public static <T> PageDTO<T> from(Page<T> page) {
        return new PageDTO<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty());
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }

    public boolean isEmpty() {
        return empty;
    }
}
