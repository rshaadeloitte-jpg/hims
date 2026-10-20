package com.hims.iam.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory revoke list keyed by SHA-256 token hash with expiry-bounded entries. */
@Service
public class InMemoryTokenRevocationService implements TokenRevocationService {

    private final Map<String, Instant> revokedUntil = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenIdOrHash, Instant until) {
        if (tokenIdOrHash != null && !tokenIdOrHash.isBlank()) {
            revokedUntil.put(tokenIdOrHash, until == null ? Instant.MAX : until);
        }
    }

    @Override
    public boolean isRevoked(String tokenIdOrHash) {
        if (tokenIdOrHash == null) {
            return false;
        }
        Instant until = revokedUntil.get(tokenIdOrHash);
        if (until == null) {
            return false;
        }
        if (until.isBefore(Instant.now())) {
            revokedUntil.remove(tokenIdOrHash);
            return false;
        }
        return true;
    }

    @Override
    public int purgeExpired() {
        Instant now = Instant.now();
        int removed = 0;
        Iterator<Map.Entry<String, Instant>> it = revokedUntil.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isBefore(now)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }
}
