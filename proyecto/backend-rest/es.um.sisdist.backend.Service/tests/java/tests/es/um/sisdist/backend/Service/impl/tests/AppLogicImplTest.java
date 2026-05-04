package es.um.sisdist.backend.Service.impl.tests;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.backend.dao.models.Dialogue;
import es.um.sisdist.backend.dao.models.User;

class AppLogicImplTest
{
    static AppLogicImpl impl;

    @BeforeAll
    static void setup()
    {
        impl = AppLogicImpl.getInstance();
    }

    @Test
    void testDefaultUser()
    {
        Optional<User> u = impl.getUserByEmail("dsevilla@um.es");
        assertTrue(u.isPresent());
        assertEquals("dsevilla@um.es", u.get().getEmail());
    }

    @Test
    void testLoginCorrect()
    {
        Optional<User> u = impl.checkLogin("dsevilla@um.es", "admin");
        assertTrue(u.isPresent());
    }

    @Test
    void testLoginWrongPassword()
    {
        Optional<User> u = impl.checkLogin("dsevilla@um.es", "wrong");
        assertTrue(u.isEmpty());
    }

    @Test
    void testGetDialoguesEmpty()
    {
        List<Dialogue> dialogues = impl.getDialoguesByUser("dsevilla");
        assertNotNull(dialogues);
    }

    @Test
    void testCreateAndDeleteDialogue()
    {
        String userId = "dsevilla";
        String name = "test-dialogue-" + System.currentTimeMillis();

        Optional<Dialogue> created = impl.createDialogue(userId, name);
        assertTrue(created.isPresent());
        assertEquals("READY", created.get().getStatus());

        boolean deleted = impl.deleteDialogue(userId, name);
        assertTrue(deleted);
    }

    @Test
    void testCreateDialogueDuplicateFails()
    {
        String userId = "dsevilla";
        String name = "dup-dialogue-" + System.currentTimeMillis();

        impl.createDialogue(userId, name);
        Optional<Dialogue> dup = impl.createDialogue(userId, name);
        assertTrue(dup.isEmpty());

        impl.deleteDialogue(userId, name);
    }
}
