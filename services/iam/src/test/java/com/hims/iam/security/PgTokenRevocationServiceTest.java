package com.hims.iam.security;

import com.hims.iam.infrastructure.cache.PgCacheStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PgTokenRevocationServiceTest {

    private final PgCacheStore cache = mock(PgCacheStore.class);
    private final PgTokenRevocationService service = new PgTokenRevocationService(cache);

    @Test
    void revoke_storesHashedKeyThatFitsTheCacheKeyColumn() {
        String jwt = "eyJ" + "x".repeat(600);
        Instant until = Instant.now().plusSeconds(1800);

        service.revoke(jwt, until);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(cache).put(key.capture(), eq("true"), eq(until));
        assertThat(key.getValue()).startsWith("iam:revoked:").hasSizeLessThanOrEqualTo(255);
        assertThat(key.getValue()).isEqualTo(PgTokenRevocationService.key(jwt));
    }

    @Test
    void revoke_withoutExpiry_isCappedInsteadOfForever() {
        service.revoke("token", null);

        ArgumentCaptor<Instant> expiresAt = ArgumentCaptor.forClass(Instant.class);
        verify(cache).put(anyString(), eq("true"), expiresAt.capture());
        assertThat(expiresAt.getValue())
                .isBefore(Instant.now().plus(PgTokenRevocationService.MAX_REVOCATION).plus(Duration.ofSeconds(5)));
    }

    @Test
    void isRevoked_isAHitOnTheHashedKey_andAMissOtherwise() {
        when(cache.get(PgTokenRevocationService.key("revoked"))).thenReturn(Optional.of("true"));
        when(cache.get(PgTokenRevocationService.key("live"))).thenReturn(Optional.empty());

        assertThat(service.isRevoked("revoked")).isTrue();
        assertThat(service.isRevoked("live")).isFalse();
        assertThat(service.isRevoked(null)).isFalse();
    }
}
