package com.salesianostriana.dam.oAuth2Example.security.oauth2

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices
import org.springframework.security.oauth2.provider.expression.OAuth2MethodSecurityExpressionHandler
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.sql.DataSource


@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
class MethodSecurityConfiguration : GlobalMethodSecurityConfiguration() {
    override fun createExpressionHandler(): MethodSecurityExpressionHandler {
        return OAuth2MethodSecurityExpressionHandler()
    }
}


@Configuration
@EnableResourceServer
class OAuth2ResourceServerConfiguration(
        private val tokenStore : TokenStore
) : ResourceServerConfigurerAdapter() {

    override fun configure(resources: ResourceServerSecurityConfigurer) {
        resources
                .tokenStore(tokenStore)
                .resourceId(MY_RESOURCE_ID)
    }

    override fun configure(http: HttpSecurity) {
        // @formatter:off
        http
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/series/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/series/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/series/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/series/**").hasRole("ADMIN")
                .anyRequest()
                .authenticated()
        // @formatter:on


    }

    companion object {
        const val MY_RESOURCE_ID = "fct-dam"
    }
}

@Configuration
@EnableAuthorizationServer
class OAuth2AuthorizationServerConfiguration(
    @Qualifier("authenticationManagerBean")
    private val authenticationManager: AuthenticationManager,
    @Qualifier("dataSource")
    private val dataSource: DataSource,
    private val tokenStore: TokenStore,
    private val passwordEncoder: PasswordEncoder,
    private val authorizationCodeServices: AuthorizationCodeServices,
    @Qualifier("userDetailsService")
    private val userDetailsService: UserDetailsService
) : AuthorizationServerConfigurerAdapter() {

    override fun configure(security: AuthorizationServerSecurityConfigurer) {
        security
                .realm("fct-dam/client")
                .checkTokenAccess("isAuthenticated()")
                //.tokenKeyAccess("permitAll()")

    }

    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients.jdbc(dataSource).passwordEncoder(passwordEncoder)
    }

    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        endpoints
                .authorizationCodeServices(authorizationCodeServices)
                .tokenStore(tokenStore)
                .userDetailsService(userDetailsService)
                .authenticationManager(authenticationManager)
    }
}

@Configuration
class TokenStoreConfiguration(
        @Qualifier("dataSource")
        private val dataSource: DataSource
) {

    @Bean
    fun tokenStore() : TokenStore = JdbcTokenStore(dataSource)

    @Bean
    fun authorizationCodeServices() : AuthorizationCodeServices = JdbcAuthorizationCodeServices(dataSource)

}

class OAuth2TokenFilter : GenericFilterBean() {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val res = response as HttpServletResponse
        val req = request as HttpServletRequest

        res.setHeader("Access-Control-Allow-Origin", "*")
        res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,HEAD,PUT,DELETE")
        res.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type")
        res.setHeader("Access-Control-Allow-Credentials", true.toString())

        if ("OPTIONS".equals(req.method, ignoreCase = true)) {
            response.status = HttpServletResponse.SC_OK
        } else {
            chain.doFilter(request, response)
        }

    }


}