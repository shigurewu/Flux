package com.huanchengfly.tieba.post.im

import com.huanchengfly.tieba.post.api.models.protos.im.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ImApiValidationTest {
    private fun auth(userId: Long, available: Long) = UpdateClientInfoResIdl(
        data_ = UpdateClientInfoResIdl.DataRes(
            userInfo = ImUserInfo(userId = userId), isUserAvailable = available,
            groupInfo = listOf(UpdateClientInfoResIdl.GroupInfo(groupId = 99, groupType = 6, lastMsgId = 42))
        )).encode()

    @Test fun verifiedAccountReturnsGroups() {
        assertEquals(listOf(ImGroupInfo(99, 6, 42)), ImApi.parseUpdateClientInfo(auth(123, 123), 123))
    }
    @Test fun emptyResponseIsNotSuccessfulLogin() {
        assertThrows(IOException::class.java) { ImApi.parseUpdateClientInfo(byteArrayOf(), 123) }
    }
    @Test fun anotherAccountIsNotAccepted() {
        assertThrows(IOException::class.java) { ImApi.parseUpdateClientInfo(auth(456, 456), 123) }
    }
    @Test fun unavailableAccountIsNotAccepted() {
        assertThrows(IOException::class.java) { ImApi.parseUpdateClientInfo(auth(123, 0), 123) }
    }
    @Test fun zeroMessageIdDoesNotClearDraftAsSuccess() {
        assertThrows(IOException::class.java) { ImApi.parseCommitPersonalMsg(CommitPersonalMsgResIdl().encode()) }
    }
    @Test fun validMessageIdIsAccepted() {
        assertEquals(42L, ImApi.parseCommitPersonalMsg(CommitPersonalMsgResIdl(
            data_ = CommitPersonalMsgResIdl.DataRes(msgId = 42)).encode()))
    }
}
