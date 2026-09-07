package studio.aroundhub.tunagiftset.auth.oauth;

import studio.aroundhub.tunagiftset.entity.type.MemberProvider;

public interface OAuthClient {

    MemberProvider providerType();

    /**
     * Exchanges an authorization code for the provider's user profile. redirectUri must be
     * byte-for-byte the same URI the frontend sent the user to the provider with, or the
     * provider rejects the exchange.
     */
    OAuthUserInfo authenticate(String code, String redirectUri);
}
