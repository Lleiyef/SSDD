package es.um.sisdist.backend.Service;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.models.UserDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/signup")
public class SignupEndpoint
{
    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response signup(UserDTO body)
    {
        if (body.getEmail() == null || body.getPassword() == null || body.getName() == null)
            return Response.status(Status.BAD_REQUEST).build();

        boolean created = impl.createUser(body.getEmail(), body.getName(), body.getPassword());
        if (!created)
            return Response.status(Status.CONFLICT).build();

        return Response.status(Status.CREATED).build();
    }
}
