package com.salesianostriana.dam.oAuth2Example.security.oauth2

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.stereotype.Component
import javax.sql.DataSource
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService

@Component
class SetupOAuth2ClientData(
        @Qualifier("dataSource")
        private val dataSource: DataSource,
        passwordEncoder: PasswordEncoder,
        @Value("\${oauth2.access-token-validity-days:1.0}") private val accessTokenValidityDays: Float,
        @Value("\${oauth2.refresh-token-validity-days:3.0}")  private val refreshTokenValidityDays: Float


) : ApplicationListener<ContextRefreshedEvent> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val service = JdbcClientDetailsService(dataSource)

    private var alreadySetup = false

    init {
        service.setPasswordEncoder(passwordEncoder)
    }

    companion object {
        const val SECONDS_IN_A_DAY = 24 * 60 * 60
    }

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (alreadySetup) return
        alreadySetup = true

        createClientIfNotExist(
                "un-cliente",
                "secreto",
                scopes = hashSetOf("read", "write"),
                authorizedGrantType = hashSetOf("password", "refresh_token"),
                resourceIds = hashSetOf(OAuth2ResourceServerConfiguration.MY_RESOURCE_ID),
                authorities = AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_CLIENT, ROLE_TRUSTED_CLIENT").toSet(),
                accessTokenValiditySeconds =  (accessTokenValidityDays * SECONDS_IN_A_DAY).toInt(),
                refreshTokenValiditySeconds = (refreshTokenValidityDays * SECONDS_IN_A_DAY).toInt()
        )

    }

    fun createClientIfNotExist(
            client: String,
            secret: String,
            scopes: Set<String>,
            authorizedGrantType: Set<String>,
            resourceIds: Set<String>,
            authorities: Set<GrantedAuthority>,
            accessTokenValiditySeconds: Int,
            refreshTokenValiditySeconds: Int
    ) {
        try {
            service.loadClientByClientId(client)
        } catch (e: NoSuchClientException) {
            val clientDetails = BaseClientDetails().apply {
                clientId = client
                clientSecret = secret
            }
            clientDetails.setScope(scopes)
            clientDetails.setResourceIds(resourceIds)
            clientDetails.setAuthorizedGrantTypes(authorizedGrantType)
            clientDetails.authorities = authorities
            clientDetails.accessTokenValiditySeconds = accessTokenValiditySeconds
            clientDetails.refreshTokenValiditySeconds = refreshTokenValiditySeconds

            service.addClientDetails(clientDetails)

            logger.info("Cliente $client añadido correctamente")
        }
    }


}