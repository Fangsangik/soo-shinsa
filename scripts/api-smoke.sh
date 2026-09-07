#!/bin/bash
# 전 엔드포인트 스모크. 실패한 것만 눈에 띄게 찍는다.
B=http://localhost:8080/api/v1
TS=$(date +%s)
PASS=0; FAIL=0
declare -a FAILURES

# call <기대코드들(| 구분)> <설명> <curl 인자...>
call() {
  local expect="$1"; shift
  local desc="$1"; shift
  local out code
  out=$(curl -s -w $'\n%{http_code}' "$@")
  code=$(printf '%s' "$out" | tail -1)
  body=$(printf '%s' "$out" | sed '$d')
  if [[ "|$expect|" == *"|$code|"* ]]; then
    PASS=$((PASS+1))
    printf '  ok   %-3s %s\n' "$code" "$desc"
  else
    FAIL=$((FAIL+1))
    FAILURES+=("$code  $desc  ::  $(printf '%s' "$body" | head -c 160)")
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

echo "=========== 1. 회원가입 / 로그인 ==========="
call "200|201" "고객 가입" -X POST $B/users/signin -H 'Content-Type: application/json' \
  -d "{\"email\":\"smoke.cus$TS@t.com\",\"name\":\"스모크고객\",\"phoneNum\":\"010-1000-0001\",\"password\":\"Abcd1234!\"}"
call "200|201" "업주 가입" -X POST $B/users/signin -H 'Content-Type: application/json' \
  -d "{\"email\":\"smoke.ven$TS@t.com\",\"name\":\"스모크업주\",\"phoneNum\":\"010-1000-0002\",\"password\":\"Abcd1234!\",\"businessNumber\":\"111-22-33333\"}"
call "200|201" "관리자 가입" -X POST $B/users/signin -H 'Content-Type: application/json' \
  -d "{\"email\":\"smoke.adm$TS@t.com\",\"name\":\"스모크관리자\",\"phoneNum\":\"010-1000-0003\",\"password\":\"Abcd1234!\",\"adminKey\":\"SooShinsa_Admin_Secret_2025_SuperSecure!\"}"

login() {
  curl -s -X POST $B/users/login -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"Abcd1234!\"}" \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null
}
CUS=$(login "smoke.cus$TS@t.com"); VEN=$(login "smoke.ven$TS@t.com"); ADM=$(login "smoke.adm$TS@t.com")
[ -n "$CUS" ] && echo "  ok   고객 로그인" || { echo "  FAIL 고객 로그인"; FAIL=$((FAIL+1)); }
[ -n "$VEN" ] && echo "  ok   업주 로그인" || { echo "  FAIL 업주 로그인"; FAIL=$((FAIL+1)); }
[ -n "$ADM" ] && echo "  ok   관리자 로그인" || { echo "  FAIL 관리자 로그인"; FAIL=$((FAIL+1)); }
AC="Authorization: Bearer $CUS"; AV="Authorization: Bearer $VEN"; AA="Authorization: Bearer $ADM"
JSON='Content-Type: application/json'

echo "=========== 2. 내 정보 ==========="
call 200 "고객 내 정보"   $B/users -H "$AC"
call 200 "업주 내 정보"   $B/users -H "$AV"
call 200 "관리자 내 정보" $B/users -H "$AA"
call 200 "내 정보 수정"   -X PATCH $B/users -H "$AC" -H "$JSON" -d '{"name":"스모크고객2","phoneNum":"010-1000-0011"}'

echo "=========== 3. 카테고리 / 서브 카테고리 ==========="
call 200 "카테고리 목록" "$B/categories?page=0&size=5"
CAT=$(jget data content 0 id)
call 200 "카테고리 단건" $B/categories/$CAT
call 200 "서브 카테고리 목록" $B/sub-categories
SUB=$(jget data 0 id)
call 200 "서브 카테고리 단건" $B/sub-categories/$SUB
call "200|201" "카테고리 생성(관리자)" -X POST $B/categories -H "$AA" -H "$JSON" -d "{\"name\":\"스모크카테고리$TS\"}"
NEWCAT=$(jget data id)
call 200 "카테고리 수정(관리자)" -X PATCH $B/categories/$NEWCAT -H "$AA" -H "$JSON" -d "{\"name\":\"스모크카테고리수정$TS\"}"
call "200|201" "서브 카테고리 생성(관리자)" -X POST $B/sub-categories -H "$AA" -H "$JSON" -d "{\"parentId\":$NEWCAT,\"name\":\"스모크서브$TS\"}"
NEWSUB=$(jget data id)

echo "=========== 4. 브랜드 ==========="
call 200 "브랜드 목록" "$B/brands?page=0&size=5"
BR=$(jget data content 0 id)
call 200 "브랜드 단건" $B/brands/$BR
call "200|201" "브랜드 신청(업주)" -X POST $B/brands -H "$AV" -H "$JSON" \
  -d "{\"name\":\"스모크브랜드$TS\",\"registrationNum\":\"111-22-33333\",\"context\":\"스모크\",\"subCategoryId\":$NEWSUB}"
MYBR=$(jget data id)
call 200 "내 브랜드(업주)" $B/brands/vendor -H "$AV"
call 200 "승인 대기(관리자)" "$B/brands/admin/pending?page=0&size=5" -H "$AA"
call 200 "브랜드 승인(관리자)" -X PATCH $B/brands/admin/$MYBR/approve -H "$AA" -H "$JSON" -d '{"approvalReason":"스모크 승인"}'
call 200 "브랜드 수정(업주)" -X PATCH $B/brands/$MYBR -H "$AV" -H "$JSON" -d '{"name":"스모크브랜드수정","context":"수정됨"}'

echo "=========== 5. 상품 / 옵션 ==========="
call "200|201" "상품 등록(업주)" -X POST $B/products/brands/$MYBR -H "$AV" -H "$JSON" \
  -d "{\"name\":\"스모크상품$TS\",\"price\":10000,\"brandId\":$MYBR,\"status\":\"AVAILABLE\"}"
PID=$(jget data id)
call 200 "상품 단건"     $B/products/$PID
call 200 "브랜드별 상품" "$B/products/brands/$MYBR?page=0&size=5"
call 200 "상품 검색"     "$B/products/search?page=0&size=5"
call 200 "자동완성"      -G --data-urlencode "keyword=스모크" "$B/products/autocomplete"
call 200 "인기 검색어"   "$B/products/search/popular?limit=5"
call 200 "상품 수정(업주)" -X PATCH $B/products/$PID -H "$AV" -H "$JSON" -d '{"name":"스모크상품수정","price":12000}'
call "200|201" "옵션 등록(업주)" -X POST $B/options/products/$PID -H "$AV" -H "$JSON" \
  -d '{"size":"M","color":"BLACK","quantity":50,"status":"AVAILABLE"}'
OPT=$(jget data id)
call 200 "옵션 단건 조회" $B/options/$OPT
call 200 "옵션 목록 조회" "$B/options?optionSize=M"
call 200 "추천 상품(고객)" "$B/products/recommendations?page=0&size=5" -H "$AC"

echo "=========== 6. 장바구니 ==========="
call "200|201" "장바구니 담기" -X POST $B/carts -H "$AC" -H "$JSON" -d "{\"productId\":$PID,\"quantity\":2,\"productOptionIds\":[$OPT]}"
CART=$(jget data cartItemId)
call 200 "장바구니 목록" "$B/carts/users?page=0&size=10" -H "$AC"
call 200 "장바구니 단건" $B/carts/$CART -H "$AC"

echo "=========== 7. 주문 / 결제 ==========="
call "200|201" "단품 주문" -X POST $B/orders/single -H "$AC" -H "$JSON" -d "{\"productOptionId\":$OPT,\"quantity\":1}"
ORD=$(jget data id)
call 200 "주문 단건" $B/orders/$ORD -H "$AC"
call 200 "내 주문 목록" "$B/orders/users?page=0&size=5" -H "$AC"
call 200 "주문 요약" "$B/orders/users/summary?page=0&size=5" -H "$AC"
call 200 "주문 최적화 조회" $B/orders/$ORD/optimized -H "$AC"
call 200 "결제 준비 정보" $B/payments/checkout/$ORD -H "$AC"
call 200 "주문 아이템 목록" "$B/order-items?page=0&size=5" -H "$AC"

echo "=========== 8. 쿠폰 ==========="
call "200|201" "쿠폰 생성(관리자)" -X POST $B/coupons -H "$AA" -H "$JSON" \
  -d "{\"couponName\":\"스모크쿠폰$TS\",\"discountRate\":10,\"couponType\":\"SPECIFIC_BRAND\",\"maxCount\":5,\"brands\":[{\"brandId\":$MYBR}]}"
COUP=$(jget data id)
call "200|201" "쿠폰 발급(고객)" -X POST $B/coupons/$COUP/issue -H "$AC"


echo "=========== 9. 리뷰 / 신고 ==========="
ORDITEM=$(curl -s "$B/order-items?page=0&size=1" -H "$AC" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data']['content'][0]['orderItemId'])" 2>/dev/null)
call "200|201" "리뷰 작성(고객)" -X POST $B/reviews/order-item/${ORDITEM:-1} -H "$AC" \
  -H "$JSON" -d "{\"rate\":5,\"content\":\"스모크 리뷰\",\"productId\":$PID}"
REV=$(jget data id)
call 200 "리뷰 목록" "$B/reviews/products/$PID?page=0&size=5"
call "200|201" "신고 생성(고객)" -X POST $B/reports -H "$AC" -H "$JSON" \
  -d "{\"targetId\":${REV:-1},\"targetType\":\"REVIEW\",\"status\":\"OPEN\",\"content\":\"스모크 신고\"}"
call 200 "신고 목록(관리자)" "$B/reports/admin?page=0&size=5" -H "$AA"

echo "=========== 10. 통계(관리자) ==========="
call 200 "매출 통계" "$B/admin/statistics/sales?periodType=DAY" -H "$AA"
call 200 "판매 수 통계" "$B/admin/statistics/count?periodType=DAY" -H "$AA"

echo
echo "=========== 결과 ==========="
echo "통과 $PASS / 실패 $FAIL"
if [ ${#FAILURES[@]} -gt 0 ]; then
  echo "--- 실패 목록 ---"
  for f in "${FAILURES[@]}"; do echo "  $f"; done
fi
echo "TS=$TS"
