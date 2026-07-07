package com.polli.state.cryptree

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PolliStateTest {

    private fun keys(read: String, write: String?) =
        NodeKeys(read.encodeToByteArray(), write?.encodeToByteArray())

    @Test
    fun writeThenReadRoundTrips() = runTest {
        val store = InMemoryCryptreeStore()
        val k = keys("r", "w")
        val id = NodeId("space-1")
        store.write(id, "hello".encodeToByteArray(), k)
        val node = store.read(id, k)
        assertNotNull(node)
        assertEquals("hello", node.payload.decodeToString())
    }

    @Test
    fun readWithWrongKeyIsDenied() = runTest {
        val store = InMemoryCryptreeStore()
        store.write(NodeId("n"), "secret".encodeToByteArray(), keys("r", "w"))
        assertFailsWith<CryptreeAccessException> {
            store.read(NodeId("n"), keys("wrong", null))
        }
    }

    @Test
    fun writeRequiresWriteKey() = runTest {
        val store = InMemoryCryptreeStore()
        assertFailsWith<CryptreeAccessException> {
            store.write(NodeId("n"), "x".encodeToByteArray(), keys("r", null))
        }
    }

    @Test
    fun linksResolveByPath() = runTest {
        val store = InMemoryCryptreeStore()
        val k = keys("r", "w")
        store.write(NodeId.ROOT, "root".encodeToByteArray(), k)
        store.write(NodeId("child"), "child".encodeToByteArray(), k)
        store.link(NodeId.ROOT, "posts", NodeId("child"), k)
        val resolved = store.resolvePath(NodeId.ROOT, listOf("posts"), k)
        assertNotNull(resolved)
        assertEquals("child", resolved.payload.decodeToString())
    }

    @Test
    fun revokeRotatesKeys() = runTest {
        val store = InMemoryCryptreeStore()
        val k = keys("r", "w")
        val id = NodeId("n")
        store.write(id, "v".encodeToByteArray(), k)
        store.revoke(id, Identity("mod"), k)
        // Old read key no longer works after re-keying.
        assertFailsWith<CryptreeAccessException> { store.read(id, k) }
    }

    @Test
    fun grantReaderGetsReadOnlyKeys() = runTest {
        val store = InMemoryCryptreeStore()
        val k = keys("r", "w")
        val id = NodeId("n")
        store.write(id, "v".encodeToByteArray(), k)
        val granted = store.grant(id, Moderator(Identity("bob"), ModeratorRole.READER), k)
        assertTrue(!granted.canWrite)
    }

    @Test
    fun mnsResolvesToRootNode() = runTest {
        val repo = PolliState.inMemory()
        val k = keys("r", "w")
        val root = NodeId("alice-root")
        repo.store.write(root, "alice space".encodeToByteArray(), k)
        repo.mns.publish(MnsRecord(name = "alice.polli", owner = Identity("alice-pk"), root = root))

        val opened = repo.openRoot("alice.polli", k)
        assertNotNull(opened)
        assertEquals("alice space", opened.payload.decodeToString())
        assertNull(repo.openRoot("unknown.polli", k))
        assertEquals(listOf("alice.polli"), repo.mns.namesOf(Identity("alice-pk")))
    }
}
