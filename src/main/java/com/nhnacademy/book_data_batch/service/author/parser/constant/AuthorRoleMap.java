package com.nhnacademy.book_data_batch.service.author.parser.constant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthorRoleMap {
    public static final Map<String, String> ROLE_KEYWORDS;

    public static String getNormalizedRole(String role) {
        if (role == null) return null;
        String cleanRole = role.replaceAll("[^가-힣a-zA-Z]", "");
        return ROLE_KEYWORDS.get(cleanRole);
    }

    static {
        Map<String, String> map = new LinkedHashMap<>();
        // 집필/저술
        map.put("지은이", "지은이");
        map.put("지은", "지은이");
        map.put("지음", "지은이");
        map.put("저", "지은이");
        map.put("저자", "지은이");
        map.put("편저자", "지은이");
        map.put("작가", "지은이");
        map.put("글쓴이", "지은이");
        map.put("글쓴사람들", "지은이");
        map.put("글작가", "지은이");
        map.put("저술", "지은이");
        map.put("지필", "지은이");
        map.put("집필", "지은이");
        map.put("글", "지은이");
        map.put("쓴", "지은이");
        map.put("씀", "지은이");
        map.put("著", "지은이");
        map.put("조사집필", "지은이");
        map.put("author", "지은이");
        map.put("writer", "지은이");
        map.put("공동편저", "지은이");
        map.put("공동편저자", "지은이");
        map.put("공편저", "지은이");
        map.put("공편자", "지은이");
        map.put("공편저자", "지은이");
        map.put("공동저자", "지은이");
        map.put("공저", "지은이");
        map.put("공저자", "지은이");
        map.put("공지음", "지은이");
        map.put("공동집필", "지은이");
        map.put("공동집필위원", "지은이");
        map.put("기록", "지은이");
        map.put("대표저자", "지은이");
        map.put("대표집필", "지은이");
        map.put("대표집필자", "지은이");
        map.put("도록", "지은이");
        map.put("도록원고", "지은이");
        map.put("도록논고", "지은이");
        map.put("도록필진", "지은이");
        map.put("동화", "지은이");
        map.put("만든이", "지은이");
        map.put("만든이들", "지은이");
        map.put("만든사람들", "지은이");
        map.put("만화", "지은이");
        map.put("말", "지은이");
        map.put("쓴이", "지은이");
        map.put("수필", "지은이");
        map.put("시", "지은이");
        map.put("시인", "지은이");
        map.put("시집", "지은이");
        map.put("에세이", "지은이");
        map.put("원저자", "지은이");
        map.put("저자대표", "지은이");
        map.put("저자명", "지은이");
        map.put("저자진", "지은이");
        map.put("저자책임", "지은이");
        map.put("저작자", "지은이");
        map.put("집필대표", "지은이");
        map.put("집필분야", "지은이");
        map.put("집필위원", "지은이");
        map.put("집필인", "지은이");
        map.put("집필자", "지은이");
        map.put("집필진", "지은이");
        map.put("집필편집자", "지은이");
        map.put("집필책임자", "지은이");
        map.put("책임저자", "지은이");
        map.put("책임집필", "지은이");
        map.put("책임집필자", "지은이");
        map.put("책임집필진", "지은이");
        map.put("초대시인", "지은이");
        map.put("초대작가", "지은이");
        map.put("편집저자", "지은이");
        map.put("필자", "지은이");
        map.put("필진", "지은이");
        map.put("해설집필", "지은이");
        map.put("구술자", "지은이");
        map.put("논고", "지은이");
        map.put("작성", "지은이");
        map.put("작성인", "지은이");
        map.put("작성자", "지은이");
        map.put("작품", "지은이");
        map.put("수록작가", "지은이");
        map.put("외저", "지은이");
        map.put("외지음", "지은이");
        map.put("개정저자", "지은이");
        map.put("공과집필", "지은이");
        map.put("극본", "지은이");

        map.put("글그림", "지은이");
        map.put("글그린이", "지은이");
        map.put("글그림사진", "지은이");
        map.put("글사진", "지은이");
        map.put("글구성", "지은이");
        map.put("글사진구성", "지은이");
        map.put("글감수", "지은이");
        map.put("글교정", "지은이");
        map.put("글구연", "지은이");
        map.put("글만화", "지은이");
        map.put("글편집", "지은이");
        map.put("글쓴이그린이", "지은이");
        map.put("그림글", "지은이");
        map.put("그림글사진", "지은이");
        map.put("그림시나리오", "지은이");
        map.put("기획글", "지은이");
        map.put("기획집필", "지은이");
        map.put("기획집필진", "지은이");
        map.put("기획편저", "지은이");
        map.put("기획편집", "지은이");
        map.put("기획및편집", "지은이");
        map.put("분석집필", "지은이");
        map.put("사진글", "지은이");
        map.put("사진편집", "지은이");
        map.put("시그림", "지은이");
        map.put("시사진", "지은이");
        map.put("시삽화", "지은이");


        // 원작
        map.put("원작자", "원작");
        map.put("원작", "원작");
        map.put("원저", "원작");
        map.put("original", "원작");
        map.put("원고", "원고");
        map.put("원고집필", "원고");

        // 번역/역주
        map.put("옮긴이", "옮긴이");
        map.put("공동번역", "옮긴이");
        map.put("옮긴", "옮긴이");
        map.put("옮김", "옮긴이");
        map.put("역", "옮긴이");
        map.put("번역", "옮긴이");
        map.put("번역인", "옮긴이");
        map.put("번역자", "옮긴이");
        map.put("번역교열", "옮긴이");
        map.put("번역편집", "옮긴이");
        map.put("번역해설", "옮긴이");
        map.put("译", "옮긴이");
        map.put("translation", "옮긴이");
        map.put("translator", "옮긴이");
        map.put("translated", "옮긴이");
        map.put("역자", "옮긴이");
        map.put("역주위원", "옮긴이");
        map.put("역주자", "옮긴이");
        map.put("영문번역", "옮긴이");

        map.put("역주", "역주");

        // 엮음
        map.put("엮은이", "엮은이");
        map.put("엮은", "엮은이");
        map.put("엮음", "엮은이");
        map.put("편저", "엮은이");
        map.put("편역", "엮은이");

        // 편집
        map.put("편자", "편집");
        map.put("편집위원", "편집");
        map.put("편집인", "편집");
        map.put("편집자", "편집");
        map.put("편찬", "편집");
        map.put("편찬위원", "편집");
        map.put("편찬위원장", "편집");
        map.put("편찬자", "편집");
        map.put("편찬책임", "편집");
        map.put("편집", "편집");
        map.put("편", "편집");
        map.put("필자편집자", "편집");
        map.put("editor", "편집");
        map.put("ed", "편집");

        // 그림/사진
        map.put("그림", "그림");
        map.put("그린이", "그림");
        map.put("그림작가", "그림");
        map.put("삽화", "그림");
        map.put("삽화가", "그림");
        map.put("일러스트", "그림");
        map.put("illustrator", "그림");
        map.put("illustration", "그림");
        map.put("사진", "사진");
        map.put("사진촬영", "사진");
        map.put("촬영", "사진");
        map.put("사진작가", "사진");
        map.put("photo", "사진");
        map.put("photograph", "사진");

        // 감수/해설
        map.put("감수", "감수");
        map.put("감역", "감역");
        map.put("해설", "해설");
        map.put("review", "감수");

        // 연구
        map.put("부연구위원", "연구");
        map.put("부연구원", "연구");
        map.put("부연구자", "연구");
        map.put("연구자", "연구");
        map.put("연구", "연구");
        map.put("연구원", "연구");
        map.put("연구진", "연구");
        map.put("연구진내부", "연구");
        map.put("연구기관", "연구");
        map.put("공동연구", "연구");
        map.put("공동연구원", "연구");
        map.put("공동연구진", "연구");
        map.put("공동연구책임", "연구");
        map.put("공동책임연구원", "연구");
        map.put("과제책임자", "연구");
        map.put("선임연구위원", "연구");
        map.put("실측조사", "연구");
        map.put("연구개발", "연구");
        map.put("연구담당", "연구");
        map.put("연구담당자", "연구");
        map.put("연구사", "연구");
        map.put("연구수행", "연구");
        map.put("연구수행자", "연구");
        map.put("연구위원", "연구");
        map.put("연구위원장", "연구");
        map.put("연구진행", "연구");
        map.put("연구집필", "연구");
        map.put("연구집필진", "연구");
        map.put("연구책임", "연구");
        map.put("연구책임자", "연구");
        map.put("참여연구원", "연구");
        map.put("참여기술자", "연구");
        map.put("참여시인", "연구");
        map.put("참여연구자", "연구");
        map.put("참여연구진", "연구");
        map.put("참여자", "연구");
        map.put("참여작가", "연구");
        map.put("참여학생", "연구");
        map.put("참여진", "연구");
        map.put("참여집필진", "연구");
        map.put("책임", "연구");
        map.put("책임연구", "연구");
        map.put("책임연구원", "연구");
        map.put("책임연구위원", "연구");
        map.put("책임연구자", "연구");
        map.put("지도교수", "연구");

        // 개발
        map.put("개발", "개발");
        map.put("개발자", "개발");
        map.put("개발책임자", "개발");
        map.put("개발진", "개발");
        map.put("개발위원", "개발");

        // 기타
        map.put("인터뷰", "인터뷰");
        map.put("각색", "각색");
        map.put("도움", "도움");
        map.put("구성", "구성");
        map.put("구술", "구술");
        map.put("구성그림", "구성그림");
        map.put("검토", "검토");
        map.put("기획", "기획");
        map.put("수행기관", "수행기관");
        map.put("사업수행기관", "사업수행기관");
        map.put("연구용역기관명", "연구수행기관");
        map.put("연구용역수행기관", "연구수행기관");
        map.put("요리", "요리");
        map.put("요리사", "요리");
        map.put("주최", "주최");
        map.put("주최기획", "주최");
        map.put("주최주관", "주최");

        ROLE_KEYWORDS = Collections.unmodifiableMap(map);
    }
    
    private AuthorRoleMap() {}
}
