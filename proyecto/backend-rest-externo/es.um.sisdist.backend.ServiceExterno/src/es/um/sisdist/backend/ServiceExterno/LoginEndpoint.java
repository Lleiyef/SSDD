package es.um.sisdist.backend.ServiceExterno;

import es.um.sisdist.backend.ServiceExterno.impl.AppLogicImpl;
import es.um.sisdist.backend.dao.auth.JwtUtil;
import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.models.UserDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Optional;

@Path("/login")
public class LoginEndpoint
{
    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @XmlRootElement
    public static class TokenResponse
    {
        public String token;
        public TokenResponse() {}
        public TokenResponse(String token) { this.token = token; }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(UserDTO body)
    {
        Optional<User> user = impl.checkLogin(body.getEmail(), body.getPassword());
        if (user.isEmpty())
            return Response.status(Status.UNAUTHORIZED).build();

        String token = JwtUtil.generateToken(user.get().getId(), user.get().getEmail());
        return Response.ok(new TokenResponse(token)).build();
    }
}
