package com.inuteamflow.server.domain.intip.service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * INTIP 본문(contentText)은 개행이 모두 제거된 평문이라, 문장 끝과 글머리표 앞에 개행을 복원한다.
 */
public final class IntipContentFormatter {

    /**
     * 글머리표로 취급하는 기호 목록.
     *
     * <p>정규식 문자 클래스({@code [...]}) 안에 그대로 끼워 넣어 쓰므로, {@code ①-⑳}은 범위로 해석된다.
     */
    private static final String BULLET_SYMBOLS = "□■○●◦▶▷➡※◎☞★☆*①-⑳";

    /**
     * 문장 끝 뒤에서 줄을 나눈다.
     *
     * <p>예: {@code "참가를 바랍니다.1. 대회명"} → {@code "참가를 바랍니다.\n1. 대회명"}
     *
     * <ul>
     *   <li>{@code 다/요} 뒤에 {@code . ! ?}가 오는 경우를 문장 끝으로 본다.
     *   <li>{@code 다/요} 앞에 한글이 붙어 있어야 하므로, 글머리표 {@code "다. "}는 문장 끝으로 오인하지 않는다.
     * </ul>
     */
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("(?<=[가-힣][다요][.!?])\\s*(?=\\S)");

    /**
     * {@link #BULLET_SYMBOLS} 기호 글머리표 앞에서 줄을 나눈다.
     *
     * <p>예: {@code "□모집인원: 1명 □근로내용"} → {@code "□모집인원: 1명\n□근로내용"}
     *
     * <ul>
     *   <li>기호가 연달아 오면 제외해, {@code "○○명"} 같은 가림 표시를 글머리표로 오인하지 않는다.
     * </ul>
     */
    private static final Pattern SYMBOL_BULLET_PATTERN = Pattern.compile(
            "(?<=[^\\s" + BULLET_SYMBOLS + "])\\s*(?=[" + BULLET_SYMBOLS + "](?![" + BULLET_SYMBOLS + "]))");

    /**
     * {@code 1.}, {@code 2)} 같은 번호 글머리표 앞에서 줄을 나눈다.
     *
     * <p>예: {@code "근로내용 1) 근로기간 2) 근로시간"} → {@code "근로내용\n1) 근로기간\n2) 근로시간"}
     *
     * <ul>
     *   <li>앞 글자가 숫자/마침표/물결표면 제외해, {@code "2027. 2. 12."}, {@code "~ 10. 16."} 같은 날짜를 번호로 오인하지 않는다.
     *   <li>닫히지 않은 괄호 안이면 제외해, {@code "(신월로11길 16)"} 같은 주소를 번호로 오인하지 않는다.
     *   <li>번호 뒤에 숫자가 이어지면 제외해, {@code "9. 19.(토)"} 같은 월/일 날짜를 번호로 오인하지 않는다.
     * </ul>
     */
    private static final Pattern NUMBERED_BULLET_PATTERN =
            Pattern.compile("(?<!\\([^()]{0,50})(?<=[^\\d.\\s~])\\s*(?=\\d{1,2}[.)]\\s(?!\\d))");

    /**
     * {@code 가.}, {@code 나.} 같은 한글 글머리표 앞에서 줄을 나눈다.
     *
     * <p>예: {@code "개요 가. 행사 개요 나. 모집 개요"} → {@code "개요\n가. 행사 개요\n나. 모집 개요"}
     *
     * <ul>
     *   <li>앞에 공백이 반드시 있어야 하므로, {@code "입니다. "} 같은 문장 끝의 {@code 다.}를 글머리표로 오인하지 않는다.
     * </ul>
     */
    private static final Pattern KOREAN_BULLET_PATTERN = Pattern.compile("\\s+(?=[가나다라마바사아자차카타파하]\\.\\s)");

    /**
     * {@code - } 대시 글머리표 앞에서 줄을 나눈다.
     *
     * <p>예: {@code "안내 - 코스 : 5km - 참가비"} → {@code "안내\n- 코스 : 5km\n- 참가비"}
     *
     * <ul>
     *   <li>앞 글자가 숫자면 제외해, {@code "09:00 - 18:00"} 같은 범위를 글머리표로 오인하지 않는다.
     *   <li>대시 뒤에 공백이 있어야 하므로, {@code "650-750만원"}, {@code "032-431-1365"}은 나누지 않는다.
     * </ul>
     */
    private static final Pattern DASH_BULLET_PATTERN = Pattern.compile("(?<=[^\\d\\s])\\s*(?=-\\s)");

    /**
     * {@code ㅣ}로 구분한 항목 이름 앞에서 줄을 나눈다.
     *
     * <p>예: {@code "주최ㅣ대학문화유니온 주관ㅣ기획단"} → {@code "주최ㅣ대학문화유니온\n주관ㅣ기획단"}
     */
    private static final Pattern LABEL_SEPARATOR_PATTERN = Pattern.compile("\\s+(?=[가-힣]+ㅣ)");

    /**
     * {@link #format(String)}이 위에서부터 순서대로 적용하는 개행 규칙 목록.
     *
     * <p>새 규칙을 추가하려면 {@link Pattern} 상수를 선언하고 이 목록에 넣는다.
     */
    private static final List<Pattern> LINE_BREAK_PATTERNS = List.of(
            SENTENCE_END_PATTERN,
            SYMBOL_BULLET_PATTERN,
            NUMBERED_BULLET_PATTERN,
            KOREAN_BULLET_PATTERN,
            DASH_BULLET_PATTERN,
            LABEL_SEPARATOR_PATTERN);

    private IntipContentFormatter() {}

    public static String format(String content) {
        if (content == null) {
            return null;
        }
        String formatted = content;
        for (Pattern pattern : LINE_BREAK_PATTERNS) {
            formatted = pattern.matcher(formatted).replaceAll("\n");
        }
        return formatted;
    }
}
