package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.ConfigIrc
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigIrcTest {

    private fun fullProps(): Properties = Properties().apply {
        setProperty(FontificatorProperties.KEY_IRC_USER, "alice")
        setProperty(FontificatorProperties.KEY_IRC_HOST, "irc.example.com")
        setProperty(FontificatorProperties.KEY_IRC_PORT, "6667")
        setProperty(FontificatorProperties.KEY_IRC_AUTH, "s3cret")
        setProperty(FontificatorProperties.KEY_IRC_ANON, "false")
        setProperty(FontificatorProperties.KEY_IRC_CHAN, "#fun")
        setProperty(FontificatorProperties.KEY_IRC_AUTO_RECONNECT, "true")
    }

    @Test
    fun load_populates_every_field_and_autoReconnect_true() {
        val cfg = ConfigIrc()
        val report = cfg.load(fullProps(), LoadConfigReport())
        assertTrue(report.isErrorFree)
        assertEquals("alice", cfg.username)
        assertEquals("irc.example.com", cfg.host)
        assertEquals("6667", cfg.port)
        assertEquals("s3cret", cfg.authorization)
        assertFalse(cfg.isAnonymous)
        assertEquals("#fun", cfg.channel)
        assertEquals("fun", cfg.channelNoHash)
        assertTrue(cfg.isAutoReconnect)
    }

    @Test
    fun channel_getter_prepends_hash_when_missing() {
        val cfg = ConfigIrc()
        cfg.load(fullProps().apply {
            setProperty(FontificatorProperties.KEY_IRC_CHAN, "fun")
        }, LoadConfigReport())
        assertEquals("#fun", cfg.channel)
    }

    @Test
    fun load_with_autoReconnect_false_sets_flag() {
        val p = fullProps().apply { setProperty(FontificatorProperties.KEY_IRC_AUTO_RECONNECT, "false") }
        val cfg = ConfigIrc()
        cfg.load(p, LoadConfigReport())
        assertFalse(cfg.isAutoReconnect)
    }

    @Test
    fun load_with_missing_autoReconnect_defaults_to_true() {
        val p = fullProps().apply { remove(FontificatorProperties.KEY_IRC_AUTO_RECONNECT) }
        val cfg = ConfigIrc()
        cfg.load(p, LoadConfigReport())
        assertTrue(cfg.isAutoReconnect)
    }

    @Test
    fun load_with_bad_anon_value_flags_error() {
        val p = fullProps().apply { setProperty(FontificatorProperties.KEY_IRC_ANON, "maybe") }
        val cfg = ConfigIrc()
        val r = cfg.load(p, LoadConfigReport())
        assertFalse(r.isErrorFree)
    }

    @Test
    fun reset_nulls_all_fields() {
        val cfg = ConfigIrc()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.reset()
        assertNull(cfg.username)
        assertNull(cfg.host)
        assertNull(cfg.port)
        assertNull(cfg.authorization)
        assertNull(cfg.channel)
        assertNull(cfg.isAnonymous)
        assertNull(cfg.isAutoReconnect)
    }

    @Test
    fun load_with_empty_optional_fields_leaves_them_null() {
        val cfg = ConfigIrc()
        val p = Properties().apply {
            setProperty(FontificatorProperties.KEY_IRC_USER, "")
            setProperty(FontificatorProperties.KEY_IRC_AUTH, "")
            setProperty(FontificatorProperties.KEY_IRC_CHAN, "")
            setProperty(FontificatorProperties.KEY_IRC_HOST, "")
            setProperty(FontificatorProperties.KEY_IRC_PORT, "")
            setProperty(FontificatorProperties.KEY_IRC_ANON, "true")
        }
        cfg.load(p, LoadConfigReport())
        assertNull(cfg.username); assertNull(cfg.authorization)
        assertNull(cfg.channel); assertNull(cfg.host); assertNull(cfg.port)
        assertTrue(cfg.isAnonymous)
    }

    @Test
    fun every_setter_round_trips_and_persists_to_props() {
        val cfg = ConfigIrc()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.username = "bob"; assertEquals("bob", cfg.username)
        cfg.host = "h.example.com"; assertEquals("h.example.com", cfg.host)
        cfg.port = "1234"; assertEquals("1234", cfg.port)
        cfg.authorization = "tok"; assertEquals("tok", cfg.authorization)
        cfg.channel = "#new"; assertEquals("#new", cfg.channel)
        cfg.isAnonymous = true; assertTrue(cfg.isAnonymous)
        cfg.isAutoReconnect = false; assertFalse(cfg.isAutoReconnect)
    }

    @Test
    fun channelNoHash_handles_null_and_empty() {
        val cfg = ConfigIrc()
        // Never loaded — channel is null
        assertNull(cfg.channelNoHash)
        // Load with blank channel
        val p = Properties().apply { setProperty(FontificatorProperties.KEY_IRC_ANON, "true") }
        cfg.load(p, LoadConfigReport())
        // channel is still null after load
        assertNull(cfg.channelNoHash)
    }

    @Test
    fun channelNoHash_strips_leading_hash_and_handles_short_hash_only_string() {
        val cfg = ConfigIrc()
        cfg.load(fullProps(), LoadConfigReport())
        // Use reflection to set channel to "#" (no subchannel).
        // getChannel() returns "#" (since it starts with "#"), length 1, >= 1 so substring(1) = "".
        val f = ConfigIrc::class.java.getDeclaredField("channel")
        f.isAccessible = true
        f.set(cfg, "#")
        assertEquals("#", cfg.channel)
        assertEquals("", cfg.channelNoHash)
    }

    @Test
    fun setAutoReconnect_null_does_not_write_prop() {
        val cfg = ConfigIrc()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.isAutoReconnect = null
        assertNull(cfg.isAutoReconnect)
    }
}
