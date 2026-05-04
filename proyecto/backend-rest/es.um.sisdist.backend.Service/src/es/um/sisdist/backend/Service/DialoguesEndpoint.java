package es.um.sisdist.backend.Service;

import es.um.sisdist.backend.Service.auth.Secured;
import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.backend.Service.impl.AppLogicImpl.PromptSubmitStatus;
import es.um.sisdist.backend.dao.models.Dialogue;
import es.um.sisdist.backend.dao.models.Message;
import es.um.sisdist.models.DialogueDTO;
import es.um.sisdist.models.MessageDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Secured
@Path("/u/{id}/dialogue")
public class DialoguesEndpoint
{
    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @XmlRootElement
    public static class DialogueRequest
    {
        public String name;
        public DialogueRequest() {}
    }

    @XmlRootElement
    public static class PromptBody
    {
        public String prompt;
        public long timestamp;
        public PromptBody() {}
    }

    // GET /u/{id}/dialogue → lista de dialogues del usuario
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listDialogues(
            @PathParam("id") String id,
            @Context ContainerRequestContext ctx)
    {
        if (!isOwner(id, ctx))
            return Response.status(Response.Status.FORBIDDEN).build();

        List<DialogueDTO> list = impl.getDialoguesByUser(id).stream()
            .map(d -> toDTO(d, false))
            .collect(Collectors.toList());

        return Response.ok(list).build();
    }

    // POST /u/{id}/dialogue → crear nuevo dialogue
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDialogue(
            @PathParam("id") String id,
            @Context ContainerRequestContext ctx,
            @Context UriInfo uriInfo,
            DialogueRequest body)
    {
        if (!isOwner(id, ctx))
            return Response.status(Response.Status.FORBIDDEN).build();

        if (body == null || body.name == null || body.name.isBlank())
            return Response.status(Response.Status.BAD_REQUEST).build();

        return impl.createDialogue(id, body.name)
            .map(d -> {
                URI location = uriInfo.getAbsolutePathBuilder()
                    .path(d.getName()).build();
                return Response.created(location).entity(toDTO(d, false)).build();
            })
            .orElse(Response.status(Response.Status.CONFLICT).build());
    }

    // GET /u/{id}/dialogue/{dname} → detalle completo con mensajes
    @GET
    @Path("/{dname}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDialogue(
            @PathParam("id") String id,
            @PathParam("dname") String dname,
            @Context ContainerRequestContext ctx)
    {
        if (!isOwner(id, ctx))
            return Response.status(Response.Status.FORBIDDEN).build();

        return impl.getDialogueByUserAndName(id, dname)
            .map(d -> Response.ok(toDTO(d, true)).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    // PUT /u/{id}/dialogue/{dname} → idempotente: crea si no existe
    @PUT
    @Path("/{dname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putDialogue(
            @PathParam("id") String id,
            @PathParam("dname") String dname,
            @Context ContainerRequestContext ctx,
            @Context UriInfo uriInfo)
    {
        if (!isOwner(id, ctx))
            return Response.status(Response.Status.FORBIDDEN).build();

        return impl.createOrGetDialogue(id, dname)
            .map(d -> {
                URI location = uriInfo.getAbsolutePath();
                return Response.created(location).entity(toDTO(d, false)).build();
            })
            .orElse(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
    }

    // DELETE /u/{id}/dialogue/{dname} → eliminar
    @DELETE
    @Path("/{dname}")
    public Response deleteDialogue(
            @PathParam("id") String id,
            @PathParam("dname") String dname,
            @Context ContainerRequestContext ctx)
    {
        if (!isOwner(id, ctx))
            return Response.status(Response.Status.FORBIDDEN).build();

        boolean deleted = impl.deleteDialogue(id, dname);
        return deleted
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    // POST /u/{id}/dialogue/{dname}/next/{token} → enviar prompt
    @POST
    @Path("/{dname}/next/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendPrompt(
            @PathParam("id") String id,
            @PathParam("dname") String dname,
            @PathParam("token") String token,
            @Context ContainerRequestContext ctx,
            @Context UriInfo uriInfo,
            PromptBody body)
    {
        if (!isOwner(id, ctx))
            return Response.status(Response.Status.FORBIDDEN).build();

        if (body == null || body.prompt == null || body.prompt.isBlank())
            return Response.status(Response.Status.BAD_REQUEST).build();

        long ts = body.timestamp > 0 ? body.timestamp : System.currentTimeMillis();
        PromptSubmitStatus status = impl.submitPrompt(id, dname, token, body.prompt, ts);

        return switch (status)
        {
            case NOT_FOUND   -> Response.status(Response.Status.NOT_FOUND).build();
            case NOT_READY   -> Response.noContent()
                                    .header("X-Reason", "dialogue is not READY").build();
            case WRONG_TOKEN -> Response.status(Response.Status.CONFLICT).build();
            case OK          -> {
                URI location = uriInfo.getBaseUriBuilder()
                    .path("u").path(id).path("dialogue").path(dname).build();
                yield Response.created(location).build();
            }
        };
    }

    // POST /u/{id}/dialogue/{dname}/end → cerrar diálogo
    @POST
    @Path("/{dname}/end")
    public Response endDialogue(
            @PathParam("id") String id,
            @PathParam("dname") String dname,
            @Context ContainerRequestContext ctx)
    {
        if (!isOwner(id, ctx))
            return Response.status(Response.Status.FORBIDDEN).build();

        boolean ok = impl.endDialogue(id, dname);
        return ok
            ? Response.noContent().build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    // El userId del JWT debe coincidir con el {id} del path
    private boolean isOwner(String pathId, ContainerRequestContext ctx)
    {
        String tokenUserId = (String) ctx.getProperty("userId");
        return pathId.equals(tokenUserId);
    }

    private DialogueDTO toDTO(Dialogue d, boolean includeMessages)
    {
        List<MessageDTO> messages = null;
        if (includeMessages)
        {
            messages = impl.getMessagesByDialogue(d.getId()).stream()
                .map(m -> new MessageDTO(
                    m.getId(), m.getDialogueId(),
                    m.getPrompt(), m.getAnswer(), m.getTimestamp()))
                .collect(Collectors.toList());
        }
        return new DialogueDTO(
            d.getId(), d.getUserId(), d.getName(),
            d.getStatus(), d.getNextToken(), d.getCreatedAt(), messages);
    }
}
