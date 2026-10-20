# M03 — Tests (from 06 "Tests:" lines)

| Rule | Test cases |
|---|---|
| BR-IPD-001 | + ok; − each precondition violation (BERA022/BERB009/BERB013/BERB012) |
| BR-IPD-002 | + first-fit; + waiting fallback; + floating fallback; + none → BEXB001; **boundary: concurrent claims (CAS)** |
| BR-IPD-003 | + release status ok; − non-release → BERB005 |
| BR-IPD-004 | + normal; + emergency; boundary: sequence formatting / overflow |
| BR-IPD-005 | + 3-day threshold; boundary: exactly 3 days (`<=`) |
| BR-DIS-001 | + clean discharge; − each violation (BERB023/BERB015/BERB011/24/25) |
| BR-DIS-002 | + normal; + death; + baby delink/decouple |
| BR-DIS-003 | + normal; + universal (amenities) bed; + cradle bed |

**Golden-master priorities:** concurrent bed claim (CAS) test; discharge/death Solr consistency (encounter cleared unless death); interim abscond at exactly 3 days.
