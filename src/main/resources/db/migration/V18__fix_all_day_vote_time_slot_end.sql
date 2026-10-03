-- 종일 투표 슬롯의 종료 시각이 LocalTime.MAX로 저장되며 00:00:00으로 넘어간 행을 23:59:59로 바로잡는다.
-- 시작과 종료가 모두 00:00:00이라 확정 요청의 종료 시각과 일치할 수 없어 결과를 확정할 수 없던 데이터다.
-- 종일 투표(is_all_day = true)의 슬롯만 대상이며, 시간 지정 투표 슬롯은 건드리지 않는다.
UPDATE vote_time_slot
SET slot_end_at = '23:59:59'
WHERE slot_start_at = '00:00:00'
  AND slot_end_at = '00:00:00'
  AND vote_date_id IN (
      SELECT vd.vote_date_id
      FROM vote_date vd
      JOIN vote v ON v.vote_id = vd.vote_id
      WHERE v.is_all_day = true);
