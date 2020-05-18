package com.salesianostriana.dam.oAuth2Example.security

import com.salesianostriana.dam.oAuth2Example.security.oauth2.OAuth2TokenFilter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import javax.servlet.FilterRegistration

@Configuration
@EnableWebSecurity
@Order(1)
class WebSecurityConfiguration(
        @Qualifier("userDetailsService")
        private val userDetailsService: UserDetailsService
) : WebSecurityConfigurerAdapter() {

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService)
    }

    override fun configure(http: HttpSecurity) {
        // @formatter:off
        http
                .cors().and().csrf().disable()
                .authorizeRequests()
                .antMatchers("/h2-console/**").permitAll()
                .antMatchers(HttpMethod.POST, "/user/").permitAll()
                .anyRequest()
                .authenticated()

        http.headers().frameOptions().disable()
        // @formatter:on
    }

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    @Bean
    fun oauth2TokenFilterRegistrationBean() : FilterRegistrationBean<OAuth2TokenFilter> {
        return FilterRegistrationBean<OAuth2TokenFilter>().apply {
            filter = OAuth2TokenFilter()
            order = Ordered.HIGHEST_PRECEDENCE
            urlPatterns = listOf("/oauth/token")
        }
    }
}


@Configuration
class ConfigurePasswordEncoder() {

    @Bean
    fun passwordEncoder() : PasswordEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder()

}