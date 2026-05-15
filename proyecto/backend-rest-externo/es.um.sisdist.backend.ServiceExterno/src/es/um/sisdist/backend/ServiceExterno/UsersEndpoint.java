package es.um.sisdist.backend.ServiceExterno;

import es.um.sisdist.backend.ServiceExterno.auth.Secured;
import es.um.sisdist.backend.ServiceExterno.impl.AppLogicImpl;
import es.um.sisdist.models.UserDTO;
import es.um.sisdist.models.UserDTOUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Secured
@Path("/u")
public class UsersEndpoint
{
    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserInfo(
            @PathParam("id") String id,
            @Context ContainerRequestContext ctx)
    {
        String tokenUserId = (String) ctx.getProperty("userId");
        if (tokenUserId == null || !tokenUserId.equals(id))
            return Response.status(Response.Status.FORBIDDEN).build();

        return impl.getUserById(id)
            .map(user -> {
                UserDTO dto = UserDTOUtils.toDTO(user);
                dto.setDialogueIds(impl.getDialogueIdsByUser(id));
                return Response.ok(dto).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
}
