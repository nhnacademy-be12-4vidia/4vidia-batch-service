package com.nhnacademy.book_data_batch.service.author.dto;

/**
 * 작가 이름과 역할을 담는 불변 DTO
 *
 * @param name 작가 이름
 * @param role 작가 역할 (지은이, 옮긴이, 그림 등)
 */
public record AuthorWithRole(
        String name,
        String role
) {
    public static final String DEFAULT_ROLE = "지은이";

    /**
     * 역할이 지정되지 않은 경우 기본 역할("지은이")로 생성
     */
    public static AuthorWithRole withDefaultRole(String name) {
        return new AuthorWithRole(name.trim(), DEFAULT_ROLE);
    }

    /**
     * 역할이 null이거나 비어있으면 기본 역할 사용
     */
    public static AuthorWithRole of(String name, String role) {
        String trimmedName = name != null ? name.trim() : "";
        String trimmedRole = (role == null || role.isBlank()) ? DEFAULT_ROLE : role.trim();
        return new AuthorWithRole(trimmedName, trimmedRole);
    }

    /**
     * 유효한 이름인지 확인
     */
    public boolean isValid() {
        return name != null 
                && !name.isBlank() 
                && name.length() >= 2
                && !name.matches("^[^가-힣a-zA-Z\\u4e00-\\u9fff\\u3040-\\u30ff]+$");
    }
}
