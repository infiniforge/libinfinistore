package com.infinifolder.sftp

import android.net.TestUri
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SftpUriTest {

    @Before
    fun setup() {
        SftpUri.uriFactory = { TestUri(it) }
    }

    @After
    fun teardown() {
        SftpUri.uriFactory = { TestUri(it) }
    }

    @Test
    fun testBuildAndParse() {
        val uri = SftpUri.build("conn-123", "/var/log/syslog")
        assertEquals("sftp://conn-123/var/log/syslog", uri.toString())
        assertTrue(SftpUri.isSftpUri(uri))
        assertEquals("conn-123", SftpUri.getConnectionId(uri))
        assertEquals("/var/log/syslog", SftpUri.getRemotePath(uri))
    }

    @Test
    fun testSshfsSchemeSupported() {
        val uri = TestUri("sshfs://my-server/home/user/doc.txt")
        assertTrue(SftpUri.isSftpUri(uri))
        assertEquals("my-server", SftpUri.getConnectionId(uri))
        assertEquals("/home/user/doc.txt", SftpUri.getRemotePath(uri))
    }

    @Test
    fun testNormalizeRemotePath() {
        assertEquals("/", SftpUri.normalizeRemotePath(""))
        assertEquals("/", SftpUri.normalizeRemotePath("/"))
        assertEquals("/", SftpUri.normalizeRemotePath("///"))
        assertEquals("/home/user", SftpUri.normalizeRemotePath("home/user"))
        assertEquals("/home/user", SftpUri.normalizeRemotePath("/home/user/"))
        assertEquals("/home/user", SftpUri.normalizeRemotePath("/home/./user"))
        assertEquals("/home", SftpUri.normalizeRemotePath("/home/user/.."))
        assertEquals("/a/c", SftpUri.normalizeRemotePath("/a/b/../c"))
        assertEquals("/", SftpUri.normalizeRemotePath("/.."))
    }

    @Test
    fun testGetParentUri() {
        val rootUri = SftpUri.build("conn-1", "/")
        assertNull(SftpUri.getParentUri(rootUri))

        val subUri = SftpUri.build("conn-1", "/etc/nginx/sites-available")
        val parent1 = SftpUri.getParentUri(subUri)
        assertEquals("sftp://conn-1/etc/nginx", parent1?.toString())

        val parent2 = SftpUri.getParentUri(parent1!!)
        assertEquals("sftp://conn-1/etc", parent2?.toString())

        val parent3 = SftpUri.getParentUri(parent2!!)
        assertEquals("sftp://conn-1/", parent3?.toString())
    }

    @Test
    fun testNotSftpUri() {
        assertFalse(SftpUri.isSftpUri(TestUri("file:///data/local/tmp")))
        assertFalse(SftpUri.isSftpUri(TestUri("content://com.android.providers/doc")))
        assertFalse(SftpUri.isSftpUri(TestUri("apps://installed")))
    }
    private fun useDecodedPathLikeAndroid() {
        SftpUri.uriFactory = { text -> object : TestUri(text) {
            override fun getPath(): String = java.net.URI(text).path
        } }
    }

    @Test
    fun filenamesWithSpacesAreEncodedWhileConnectionAndDirectoriesStayIntact() {
        useDecodedPathLikeAndroid()
        val path = "/srv/1TB/MultiMedia/Emulation/ROMS/Console/Atari/atarilynx/LynxRen/Awesome Golf (1991).lnx"
        val uri = SftpUri.build("conn-123", path)
        assertEquals("sftp://conn-123/srv/1TB/MultiMedia/Emulation/ROMS/Console/Atari/atarilynx/LynxRen/Awesome%20Golf%20%281991%29.lnx", uri.toString())
        assertEquals("conn-123", SftpUri.getConnectionId(uri))
        assertEquals(path, SftpUri.getRemotePath(uri))
    }

    @Test
    fun reservedFilenameCharactersAndUnicodeDecodeExactlyOnce() {
        useDecodedPathLikeAndroid()
        val path = "/games/100%20 +#?[β].lnx"
        val uri = SftpUri.build("conn-123", path)
        assertFalse(uri.toString().contains(' '))
        assertEquals(path, java.net.URI(uri.toString()).path)
        assertEquals(path, SftpUri.getRemotePath(uri))
        assertTrue(uri.toString().contains("100%2520"))
        assertNull(java.net.URI(uri.toString()).query)
        assertNull(java.net.URI(uri.toString()).fragment)
    }

    @Test
    fun encodedParentDirectoriesAreNotDoubleEncodedWhenNavigatingUp() {
        useDecodedPathLikeAndroid()
        val uri = SftpUri.build("conn-123", "/rom sets/Atari/Awesome Golf.lnx")
        assertEquals("sftp://conn-123/rom%20sets/Atari", SftpUri.getParentUri(uri).toString())
        assertEquals("sftp://conn-123/rom%20sets", SftpUri.getParentUri(SftpUri.getParentUri(uri)!!).toString())
    }

    @Test
    fun filenameEncodingKeepsUnicodeSpellingAndEscapesExclamationMarks() {
        useDecodedPathLikeAndroid()
        val path = "/games/Cafe\u0301 Golf! +100%.lnx"
        val uri = SftpUri.build("conn-123", path)
        assertEquals(path, SftpUri.getRemotePath(uri))
        assertTrue(uri.toString().contains("%21"))
        assertTrue(uri.toString().contains("%2B100%25"))
    }

}
