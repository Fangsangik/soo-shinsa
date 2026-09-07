#!/bin/bash
# 1차 스모크에서 빠진 흐름들: 토큰 갱신/로그아웃, 장바구니 수정/쿠폰적용/주문,
# 주문 수정/부분취소, 리뷰 수정/삭제, 신고 처리, 탈퇴
B=http://localhost:8080/api/v1
TS=$(date +%s)
PASS=0; FAIL=0
declare -a FAILURES

call() {
  local expect="$1"; shift
  local desc="$1"; shift
  local out code
  out=$(curl -s -w $'\n%{http_code}' "$@")
  code=$(printf '%s' "$out" | tail -1)
  body=$(printf '%s' "$out" | sed '$d')
  if [[ "|$expect|" == *"|$code|"* ]]; then
    PASS=$((PASS+1)); printf '  ok   %-3s %s\n' "$code" "$desc"
  else
    FAIL=$((FAIL+1)); FAILURES+=("$code  $desc  ::  $(printf '%s' "$body" | head -c 160)")
    printf '  FAIL %-3s %s\n     %s\n' "$code" "$desc" "$(printf '%s' "$body" | head -c 160)"
  fi
  LAST_BODY="$body"
}
jget() {
  printf '%s' "$LAST_BODY" | python3 -c '
import sys, json
d = json.load(sys.stdin)
for k in sys.argv[1:]:
    d = d[int(k)] if k.isdigit() else d[k]
print(d)
' "$@" 2>/dev/null
}
JSON='Content-Type: application/json'

echo "=========== 준비: 계정/브랜드/상품 ==========="
call "200|201" "고객 가입" -X POST $B/users/signin -H "$JSON" -d "{\"email\":\"s2.cus$TS@t.com\",\"name\":\"이차고객\",\"phoneNum\":\"010-2000-0001\",\"password\":\"Abcd1234!\"}"
call "200|201" "업주 가입" -X POST $B/users/signin -H "$JSON" -d "{\"email\":\"s2.ven$TS@t.com\",\"name\":\"이차업주\",\"phoneNum\":\"010-2000-0002\",\"password\":\"Abcd1234!\",\"businessNumber\":\"222-33-44444\"}"
call "200|201" "관리자 가입" -X POST $B/users/signin -H "$JSON" -d "{\"email\":\"s2.adm$TS@t.com\",\"name\":\"이차관리자\",\"phoneNum\":\"010-2000-0003\",\"password\":\"Abcd1234!\",\"adminKey\":\"SooShinsa_Admin_Secret_2025_SuperSecure!\"}"

login_body() { curl -s -X POST $B/users/login -H "$JSON" -d "{\"email\":\"$1\",\"password\":\"Abcd1234!\"}"; }
CUS_LOGIN=$(login_body "s2.cus$TS@t.com")
CUS=$(printf '%s' "$CUS_LOGIN" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
CUS_REFRESH=$(printf '%s' "$CUS_LOGIN" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")
VEN=$(login_body "s2.ven$TS@t.com" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
ADM=$(login_body "s2.adm$TS@t.com" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
AC="Authorization: Bearer $CUS"; AV="Authorization: Bearer $VEN"; AA="Authorization: Bearer $ADM"

SUB=$(curl -s "$B/sub-categories" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'][0]['id'])")
call "200|201" "브랜드 신청" -X POST $B/brands -H "$AV" -H "$JSON" -d "{\"name\":\"이차브랜드$TS\",\"registrationNum\":\"222-33-44444\",\"context\":\"이차\",\"subCategoryId\":$SUB}"
MYBR=$(jget data id)
call 200 "브랜드 승인" -X PATCH $B/brands/admin/$MYBR/approve -H "$AA" -H "$JSON" -d '{"approvalReason":"ok"}'
call "200|201" "상품 등록" -X POST $B/products/brands/$MYBR -H "$AV" -H "$JSON" -d "{\"name\":\"이차상품$TS\",\"price\":20000,\"brandId\":$MYBR,\"status\":\"AVAILABLE\"}"
PID=$(jget data id)
call "200|201" "옵션 등록" -X POST $B/options/products/$PID -H "$AV" -H "$JSON" -d '{"size":"L","color":"WHITE","quantity":30,"status":"AVAILABLE"}'
OPT=$(jget data id)

echo "=========== 1. 토큰 갱신 / 로그아웃 ==========="
call 200 "토큰 갱신" -X POST $B/users/refresh -H "$JSON" -d "{\"refreshToken\":\"$CUS_REFRESH\",\"accessToken\":\"$CUS\"}"
NEW_ACCESS=$(jget data accessToken)
if [ -n "$NEW_ACCESS" ]; then AC="Authorization: Bearer $NEW_ACCESS"; fi
call 200 "갱신 토큰으로 내 정보" $B/users -H "$AC"

# 로그아웃 후 같은 토큰은 블랙리스트로 거부돼야 한다
TMP=$(login_body "s2.cus$TS@t.com" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")
call 200 "로그아웃" -X POST $B/users/logout -H "Authorization: Bearer $TMP"
call 401 "로그아웃한 토큰 거부" $B/users -H "Authorization: Bearer $TMP"

echo "=========== 2. 장바구니 흐름 ==========="
call "200|201" "장바구니 담기" -X POST $B/carts -H "$AC" -H "$JSON" -d "{\"productId\":$PID,\"quantity\":2,\"productOptionIds\":[$OPT]}"
CART=$(jget data cartItemId)
call 200 "장바구니 수량 수정" -X PATCH $B/carts -H "$AC" -H "$JSON" -d "{\"cartItemId\":$CART,\"productId\":$PID,\"quantity\":3,\"productOptionIds\":[$OPT]}"

call "200|201" "쿠폰 생성" -X POST $B/coupons -H "$AA" -H "$JSON" -d "{\"couponName\":\"이차쿠폰$TS\",\"discountRate\":10,\"couponType\":\"SPECIFIC_BRAND\",\"maxCount\":5,\"brands\":[{\"brandId\":$MYBR}]}"
COUP=$(jget data id)
call "200|201" "쿠폰 발급" -X POST $B/coupons/$COUP/issue -H "$AC"
call 200 "장바구니 쿠폰 적용" -X POST $B/carts/$CART/apply-coupon -H "$AC" -H "$JSON" -d "{\"couponId\":$COUP}"

call "200|201" "장바구니 개별 주문" -X POST $B/orders/carts -H "$AC" -H "$JSON" -d "{\"cartId\":$CART}"
ORD1=$(jget data id)

call "200|201" "장바구니 담기(전체주문용)" -X POST $B/carts -H "$AC" -H "$JSON" -d "{\"productId\":$PID,\"quantity\":1,\"productOptionIds\":[$OPT]}"
call "200|201" "장바구니 전체 주문" -X POST $B/orders/carts/all -H "$AC"
ORD2=$(jget data id)

call "200|201" "장바구니 담기(삭제용)" -X POST $B/carts -H "$AC" -H "$JSON" -d "{\"productId\":$PID,\"quantity\":1,\"productOptionIds\":[$OPT]}"
CART3=$(jget data cartItemId)
call 204 "장바구니 삭제" -X DELETE $B/carts/$CART3 -H "$AC"

echo "=========== 3. 주문 수정 / 부분 취소 ==========="
call 403 "주문 상태 수정은 고객 차단" -X PATCH $B/orders -H "$AC" -H "$JSON" -d "{\"orderId\":$ORD1,\"status\":\"ORDERCOMPLETED\"}"
call 200 "주문 상태 수정(관리자)" -X PATCH $B/orders -H "$AA" -H "$JSON" -d "{\"orderId\":$ORD1,\"status\":\"ORDERCOMPLETED\"}"
OITEM=$(curl -s "$B/orders/$ORD2" -H "$AC" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['orderItems'][0]['orderItemId'])" 2>/dev/null)
call 200 "부분 취소" -X POST $B/orders/$ORD2/partial-cancel -H "$AC" -H "$JSON" -d "{\"orderItemIds\":[$OITEM],\"cancelReason\":\"이차 점검\"}"
STOCK_AFTER=$(curl -s $B/options/$OPT | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['quantity'])" 2>/dev/null)
ITEM_STATUS=$(curl -s "$B/orders/$ORD2" -H "$AC" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['orderItems'][0]['status'])" 2>/dev/null)
if [ "$ITEM_STATUS" = "CANCELLED" ]; then echo "  ok   -   부분취소 상태가 저장됨 (status=$ITEM_STATUS, 재고=$STOCK_AFTER)"; PASS=$((PASS+1));
else echo "  FAIL -   부분취소 상태가 저장 안 됨 (status=$ITEM_STATUS)"; FAIL=$((FAIL+1)); FAILURES+=("-  부분취소 상태 미저장 status=$ITEM_STATUS"); fi

echo "=========== 4. 리뷰 수정/삭제, 신고 처리 ==========="
DITEM=$(curl -s "$B/orders/$ORD1" -H "$AC" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['orderItems'][0]['orderItemId'])" 2>/dev/null)
call "200|201" "리뷰 작성" -X POST $B/reviews/order-item/$DITEM -H "$AC" -H "$JSON" -d "{\"rate\":4,\"content\":\"이차 리뷰\",\"productId\":$PID}"
REV=$(jget data id)
call 200 "리뷰 단건 조회" $B/reviews/$REV
call 200 "리뷰 수정" -X PATCH $B/reviews/$REV -H "$AC" -H "$JSON" -d "{\"id\":$REV,\"rate\":5,\"content\":\"이차 리뷰 수정\"}"

call "200|201" "신고 생성" -X POST $B/reports -H "$AC" -H "$JSON" -d "{\"targetId\":$REV,\"targetType\":\"REVIEW\",\"status\":\"OPEN\",\"content\":\"이차 신고\"}"
REP=$(jget data id)
call 200 "신고 단건(관리자)" $B/reports/admin/$REP -H "$AA"
call "200|204" "신고 상태 변경(관리자)" -X PATCH $B/reports/admin/$REP/status -H "$AA" -H "$JSON" -d '{"status":"RESOLVED"}' 
call "200|204" "신고 삭제(관리자)" -X DELETE $B/reports/admin/$REP -H "$AA"
call "200|204" "리뷰 삭제" -X DELETE $B/reviews/$REV -H "$AC"

echo "=========== 5. 탈퇴 ==========="
call "200|204" "회원 탈퇴" -X POST $B/users/leave -H "$AC" -H "$JSON" -d '{"password":"Abcd1234!"}'
call "400|401" "탈퇴 후 로그인 거부" -X POST $B/users/login -H "$JSON" -d "{\"email\":\"s2.cus$TS@t.com\",\"password\":\"Abcd1234!\"}"

echo
echo "=========== 결과 ==========="
echo "통과 $PASS / 실패 $FAIL"
if [ ${#FAILURES[@]} -gt 0 ]; then
  echo "--- 실패 목록 ---"
  for f in "${FAILURES[@]}"; do echo "  $f"; done
fi
echo "TS=$TS"
