package com.salesianostriana.dam.tvseries.security.jwt

import com.salesianostriana.dam.tvseries.users.User
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


@Component
class JwtTokenProvider {

    companion object {
        const val TOKEN_HEADER = "Authorization"
        const val TOKEN_PREFIX = "Bearer "
        const val TOKEN_TYPE = "JWT"
    }

    private val jwtSecreto : String = "mJI.w|g!kCv(5bLr0A@\"wTC,N9mNM]Dd^19h0[?!KB1~I~kfA(,;T<S][_Pm_v(asdfghasdfg"

    // Lo expresamos en días
    private val jwtDuracionToken : Long = 3
    private val jwtDuracionRefreshToken : Long = 10

    private val parser = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(jwtSecreto.toByteArray())).build()

    private val logger : Logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    fun generateToken(authentication : Authentication) : String {
        val user : User = authentication.principal as User
        return generateTokens(user, false)
    }

    fun generateToken(user : User) = generateTokens(user, false)

    fun generateRefreshToken(authentication: Authentication) : String {
        val user : User = authentication.principal as User
        return generateTokens(user, true)
    }

    fun generateRefreshToken(user : User) = generateTokens(user, true)

    /*
     TODO Se puede añadir un claim al token para indicar el tipo de token: refresh o token de autenticación
     De esa forma no se podrían hacer peticiones "normales" con el token de refresco
     */

    private fun generateTokens(user : User, isRefreshToken : Boolean) : String {
        val tokenExpirationDate =
                Date.from(Instant.now().plus(if (isRefreshToken) jwtDuracionRefreshToken else jwtDuracionToken, ChronoUnit.DAYS))
        val builder = Jwts.builder()
                .signWith(Keys.hmacShaKeyFor(jwtSecreto.toByteArray()), SignatureAlgorithm.HS512)
                .setHeaderParam("typ", TOKEN_TYPE)
                .setSubject(user.id.toString())
                .setExpiration(tokenExpirationDate)
                .setIssuedAt(Date())
                .claim("refresh", isRefreshToken)

        if (!isRefreshToken) {
            builder
                    .claim("fullname", user.fullName)
                    .claim("roles", user.roles.joinToString())

        }
        return builder.compact()
    }

    fun getUserIdFromJWT(token: String): UUID = UUID.fromString(parser.parseClaimsJws(token).body.subject)

    fun validateRefreshToken(token : String) = validateToken(token, true)

    fun validateAuthToken(token : String) = validateToken(token, false)


    private fun validateToken(token : String, isRefreshToken: Boolean) : Boolean {
        try {
            val claims = parser.parseClaimsJws(token)
            if (isRefreshToken != claims.body["refresh"])
                throw UnsupportedJwtException("No se ha utilizado el token apropiado")
            return true
        } catch (ex : Exception) {
            with(logger) {
                when (ex) {
                    is SignatureException -> info("Error en la firma del token ${ex.message}")
                    is MalformedJwtException -> info("Token malformado ${ex.message}")
                    is ExpiredJwtException -> info("Token expirado ${ex.message}")
                    is UnsupportedJwtException -> info("Token no soportado ${ex.message}")
                    is IllegalArgumentException -> info("Token incompleto (claims vacío) ${ex.message}")
                    else -> info("Error indeterminado")
                }
            }

        }

        return false

    }

}