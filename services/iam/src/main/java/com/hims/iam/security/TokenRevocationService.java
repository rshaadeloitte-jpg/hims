package com.hims.iam.security;

import java.time.Instant;

/** Token revoke-list contract. In-memory implementation is acceptable per GAP-REGISTER WP-1;
 * swap for Redis-backed impl at MIG-00x (Catalog #22: session/token cache is Redis-owned). */
public interface TokenRevocationService {

    /** Revoke a token until the given instant (typically its own expiry). */
    void revoke(String tokenIdOrHash, Instant until);

    boolean isRevoked(String tokenIdOrHash);

    /** Drop expired entries (housekeeping). */
    int purgeExpired();
}
