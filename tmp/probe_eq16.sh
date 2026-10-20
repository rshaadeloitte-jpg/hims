#!/bin/bash
# TAS-16 E2E probe: opening-balance API on the shared PG (dev :8088)
B=http://localhost:8088/api/v1/equipment
AUTH='-u admin:admin'
CT='Content-Type: application/json'

echo "=========== 1. ADD full payload (hijack id=999 unitId=42, rate 149.995 -> 150.00)"
curl -s $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '{
  "itemId": 880, "openingBalance": 12, "rate": 149.995, "amount": 1799.94,
  "accessionNo": " ACC-LIVE ", "serialNo": "SN-LIVE-1", "model": "Monitor M200",
  "storeId": 5, "supplierId": 9, "remarks": "live stock take", "id": 999, "unitId": 42
}' | tee /tmp/ob_full.json
echo; echo "--- hijack/scale/trim checks:"
grep -o '"id":[0-9]*' /tmp/ob_full.json | head -1
grep -o '"unitId":[0-9]*' /tmp/ob_full.json | head -1
grep -o '"rate":[0-9.]*' /tmp/ob_full.json | head -1
grep -o '"accessionNo":"[^"]*"' /tmp/ob_full.json | head -1

echo "=========== 2. ADD minimal (no openingBalance key -> column DEFAULT 0)"
curl -s $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '{"itemId": 881, "remarks": "qty omitted"}' | grep -o '"openingBalance":[0-9]*'

echo "=========== 3. BULK import (array body, 2 rows)"
curl -s $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" \
  -d '[{"itemId":890,"openingBalance":4,"rate":10.00},{"itemId":891,"amount":99.50}]' | grep -o '"itemId":[0-9]*'

echo "=========== 4. GUARDS (all must be 400 BEXA003 except last = 401)"
echo -n "null body            -> "; curl -s -o /dev/null -w '%{http_code} ' $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT"; curl -s $AUTH -X POST "$B/opening-balance?unitId=7" | head -c 120; echo
echo -n "missing itemId       -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '{"openingBalance":3}'
echo -n "negative qty         -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '{"itemId":882,"openingBalance":-1}'
echo -n "negative rate        -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '{"itemId":882,"rate":-5}'
echo -n "oversized amount     -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '{"itemId":882,"amount":1000000000000000000}'
echo -n "bad money bind       -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '{"itemId":882,"rate":"abc"}'
echo -n "empty array          -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH -X POST "$B/opening-balance?unitId=7" -H "$CT" -d '[]'
echo -n "no auth              -> "; curl -s -o /dev/null -w '%{http_code}\n' -X GET "$B/opening-balance"

echo "=========== 5. LIST register"
echo "--- all active:"; curl -s $AUTH "$B/opening-balance" | grep -o '"itemId":[0-9]*'
echo "--- unitId=7:";    curl -s $AUTH "$B/opening-balance?unitId=7" | grep -o '"itemId":[0-9]*'
echo "--- unitId=99:";   curl -s $AUTH "$B/opening-balance?unitId=99"
echo "--- itemId=890:";  curl -s $AUTH "$B/opening-balance?itemId=890" | grep -o '"itemId":[0-9]*'

OB_ID=$(grep -o '"id":[0-9]*' /tmp/ob_full.json | head -1 | cut -d: -f2)
echo "=========== 6. UPDATE row $OB_ID — business patch + full server-owned hijack attack"
curl -s $AUTH -X PUT "$B/opening-balance/$OB_ID" -H "$CT" -d '{
  "id": 888, "itemId": 999, "unitId": 42, "createdUnitId": 42, "updatedUnitId": 42,
  "addedBy": 42, "updatedBy": 42, "synchronizedd": true,
  "openingBalance": 30, "rate": "199.99", "remarks": "recounted after audit"
}' | tee /tmp/ob_patch.json
echo; echo "--- hijack neutralized?:"
grep -o '"itemId":[0-9]*' /tmp/ob_patch.json | head -1
grep -o '"unitId":[0-9]*' /tmp/ob_patch.json | head -1
grep -o '"createdUnitId":[0-9]*' /tmp/ob_patch.json | head -1
grep -o '"synchronizedd":[a-z]*' /tmp/ob_patch.json | head -1
grep -o '"updatedBy":[0-9]*' /tmp/ob_patch.json | head -1
echo -n "unknown id -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH -X PUT "$B/opening-balance/987654" -H "$CT" -d '{"openingBalance":1}'

echo "=========== 7. REGRESSIONS"   # NOTE: $B already ends in /api/v1/equipment
echo -n "equipment by-id 1005   -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH "$B/1005"
echo -n "equipment search       -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH "$B?unitId=7"
echo -n "complaint search       -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH "$B/complaints?page=0&size=3"
echo -n "pm due list            -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH "$B/pm-schedules?dueTo=2026-08-22"
echo -n "warranty counts        -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH "$B/warranties/counts"
echo -n "usage report           -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH "$B/usage/report?unitId=7"
echo -n "docs of asset 1005     -> "; curl -s -o /dev/null -w '%{http_code}\n' $AUTH "$B/1005/documents"
echo -n "openapi opening paths  -> "; curl -sf $AUTH http://localhost:8088/v3/api-docs | grep -o '"/api/v1/equipment/opening-balance[^"]*"' | sort -u
