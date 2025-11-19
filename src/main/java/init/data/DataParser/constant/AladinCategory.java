package init.data.DataParser.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AladinCategory {
    // 1. 만화
    COMICS(2551, "만화"),

    // 2. 문학
    NOVEL(1, "소설/시/희곡"),
    ESSAY(55889, "에세이"),

    // 3. 실용/학습
    ECONOMY(170, "경제경영"),
    SELF_HELP(336, "자기계발"),
    IT(351, "컴퓨터/모바일"),

    // 4. 인문/교양
    HUMANITIES(656, "인문학"),
    HISTORY(74, "역사"),
    SOCIAL(798, "사회과학"),
    SCIENCE(987, "과학"),
    ARTS(517, "예술/대중문화"),

    // 5. 기타
    MAGAZINE(2913, "잡지"),
    TODDLER(13789, "유아");

    private final int cid;
    private final String name;

    public static Integer findCidByKorean(String koreanName) {
        for (AladinCategory category : AladinCategory.values()) {
            if (category.getName().equals(koreanName)) {
                return category.getCid();
            }
        }
        throw new IllegalArgumentException("해당하는 카테고리가 없습니다: " + koreanName);
    }
}
