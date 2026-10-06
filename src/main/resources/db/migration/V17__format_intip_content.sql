-- 개행 복원 적용 이전에 저장된 INTIP 공지 본문에 개행을 한 번 넣는다.
-- 신규 공지는 IntipSyncService가 저장 시점에 IntipContentFormatter로 처리하므로 기존 행만 대상이다.
--
-- 아래 정규식은 이 시점의 IntipContentFormatter 규칙을 PostgreSQL로 옮긴 스냅샷이며, 같은 순서로 적용한다.
-- - source_url이 있는 행(INTIP 수집 공지)만 대상이며, 사용자가 직접 작성한 정보글은 건드리지 않는다.
-- - 규칙이 멱등이라 이미 개행이 들어간 행에 다시 적용해도 결과가 같다.
-- - 내용 수정이 아니라 서식 정리이므로 updated_at은 갱신하지 않는다.

-- 문장 끝('~다.', '~요!' 등) 뒤
UPDATE info_post
SET content = regexp_replace(content, '(?<=[가-힣][다요][.!?])\s*(?=\S)', chr(10), 'g')
WHERE source_url IS NOT NULL;

-- 기호 글머리표 앞 (기호가 연달아 오는 "○○명" 같은 가림 표시는 제외)
UPDATE info_post
SET content = regexp_replace(
        content,
        '(?<=[^\s□■○●◦▶▷➡※◎☞★☆*①-⑳])\s*(?=[□■○●◦▶▷➡※◎☞★☆*①-⑳](?![□■○●◦▶▷➡※◎☞★☆*①-⑳]))',
        chr(10),
        'g')
WHERE source_url IS NOT NULL;

-- '1.', '2)' 번호 글머리표 앞 (날짜, 괄호 안 주소 등은 제외)
UPDATE info_post
SET content = regexp_replace(content, '(?<!\([^()]{0,50})(?<=[^\d.\s~])\s*(?=\d{1,2}[.)]\s(?!\d))', chr(10), 'g')
WHERE source_url IS NOT NULL;

-- '가.', '나.' 한글 글머리표 앞
UPDATE info_post
SET content = regexp_replace(content, '\s+(?=[가나다라마바사아자차카타파하]\.\s)', chr(10), 'g')
WHERE source_url IS NOT NULL;

-- '- ' 대시 글머리표 앞 ("09:00 - 18:00" 같은 범위는 제외)
UPDATE info_post
SET content = regexp_replace(content, '(?<=[^\d\s])\s*(?=-\s)', chr(10), 'g')
WHERE source_url IS NOT NULL;

-- 'ㅣ'로 구분한 항목 이름 앞 ("주최ㅣ… 주관ㅣ…")
UPDATE info_post
SET content = regexp_replace(content, '\s+(?=[가-힣]+ㅣ)', chr(10), 'g')
WHERE source_url IS NOT NULL;
