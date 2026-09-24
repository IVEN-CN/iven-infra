package io.github.ivencn.infra.security.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtProperties properties() {
        JwtProperties props = new JwtProperties();
        props.setSecret("testsecretkey_must_be_at_least_256_bits_long!!");
        props.setExpiration(3600L);
        return props;
    }

    @Test
    @DisplayName("默认载荷：签发并解析后字段正确往返")
    void defaultPayloadRoundTrip() {
        JwtService jwtService = new JwtService(properties());

        String token = jwtService.issue(42L);
        JwtClaims parsed = jwtService.parse(token);

        assertThat(parsed).isInstanceOf(DefaultJwtPayload.class);
        assertThat(parsed.getSubjectId()).isEqualTo(42L);
        assertThat(parsed.getJti()).isEqualTo(jwtService.getJti(token));
        assertThat(parsed.getIssuedAt()).isNotNull().isPositive();
        assertThat(parsed.getExpiration()).isEqualTo(parsed.getIssuedAt() + 3600);
    }

    @Test
    @DisplayName("自定义载荷类：解析为目标类型且自定义字段保留")
    void customPayloadClass() {
        JwtProperties props = properties();
        props.setPayloadClass(MyPayload.class);
        JwtService jwtService = new JwtService(props);

        String token = jwtService.issue(7L);
        JwtClaims parsed = jwtService.parse(token);

        assertThat(parsed).isInstanceOf(MyPayload.class);
        MyPayload mine = (MyPayload) parsed;
        assertThat(mine.getSubjectId()).isEqualTo(7L);
        assertThat(mine.getJti()).isNotBlank();
    }

    @Test
    @DisplayName("过期/篡改/空 token 解析返回 null，validateToken 为 false")
    void invalidTokens() throws InterruptedException {
        JwtProperties props = properties();
        props.setExpiration(1L);
        JwtService jwtService = new JwtService(props);

        String expired = jwtService.issue(1L);
        Thread.sleep(1100);
        assertThat(jwtService.parse(expired)).isNull();
        assertThat(jwtService.validateToken(expired)).isFalse();

        assertThat(jwtService.parse("not-a-jwt")).isNull();
        assertThat(jwtService.parse(null)).isNull();

        JwtProperties otherProps = properties();
        otherProps.setSecret("othersecretkey_must_be_at_least_256_bits_long!!");
        JwtService other = new JwtService(otherProps);
        String foreign = other.issue(1L);
        assertThat(jwtService.parse(foreign)).isNull();
        assertThat(jwtService.validateToken(foreign)).isFalse();
    }

    static class MyPayload extends DefaultJwtPayload {
        @JsonProperty("scope")
        private String scope;

        public String getScope() {
            return scope;
        }
    }
}
