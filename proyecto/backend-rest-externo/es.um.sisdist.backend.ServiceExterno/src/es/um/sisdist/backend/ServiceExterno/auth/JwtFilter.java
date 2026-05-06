package es.um.sisdist.backend.ServiceExterno.auth;

import es.um.sisdist.backend.dao.auth.JwtUtil;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter
{
    @Override
    public void filter(ContainerRequestContext ctx) throws IOException
    {
        String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        String token = authHeader.substring(7);
        var claims = JwtUtil.getClaims(token);
        if (claims.isEmpty())
        {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        ctx.setProperty("userId", claims.get().getSubject());
        ctx.setProperty("jwtToken", token);
    }
}
