package com.huanchengfly.tieba.post.api.models

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FansListBeanTest {
    private fun parse(json: String) = Gson().fromJson(json, FansListBean::class.java).toFollowListBean()
    @Test fun unrelatedFollowMetadataCannotBreakFansParsing() {
        val user = """{"id":"123","name":"a","name_show":"昵称","portrait":"p","priv_sets":[],"business_account_info":[],"ala_info":[],"intro":[]}"""
        assertThrows(com.google.gson.JsonSyntaxException::class.java) {
            Gson().fromJson(user, FollowListBean.FollowUserBean::class.java)
        }
        val result = parse("""{"error_code":0,"user_list":[$user],"page":{"current_page":1,"total_count":1,"has_more":0}}""")
        assertEquals("昵称", result.followList.single().nameShow)
        assertEquals(123L, result.followList.single().id)
    }

    @Test fun stringNumbersAndNestedPaginationAreRead() {
        val result = parse("""{"error_code":"0","user_list":[{"id":"123","name":"test","name_show":"昵称","portrait":"abc"}],"page":{"current_page":"2","total_count":"114","has_more":"1"}}""")
        assertEquals(2, result.pageNum)
        assertEquals(114, result.totalFollowNum)
        assertEquals(1, result.hasMore)
        assertEquals(123L, result.followList.single().id)
        assertEquals("昵称", result.followList.single().nameShow)
    }
    @Test fun legitimateEmptyListIsAllowed() {
        assertTrue(parse("""{"error_code":0,"user_list":[],"page":{"current_page":1,"total_count":0,"has_more":0}}""").followList.isEmpty())
    }
    @Test fun failedResponseIsNotAnEmptyList() {
        val error = assertThrows(IOException::class.java) { parse("""{"error_code":160001,"error_msg":"请重新登录"}""") }
        assertEquals("请重新登录", error.message)
    }
    @Test fun missingDataIsNotAnEmptyList() {
        assertThrows(IOException::class.java) { parse("""{"error_code":0}""") }
    }
    @Test fun oneUnavailableAccountDoesNotBreakTheWholePage() {
        val result = parse("""{"error_code":0,"user_list":[null,{"id":0},{"id":123,"name":"normal"}],"page":{"current_page":1,"total_count":3,"has_more":0}}""")
        assertEquals(123L, result.followList.single().id)
        assertEquals(3, result.totalFollowNum)
    }
    @Test fun unavailablePageIsNotReportedAsNoFans() {
        assertThrows(IOException::class.java) {
            parse("""{"error_code":0,"user_list":[null,{"id":0}],"page":{"current_page":1,"total_count":2,"has_more":0}}""")
        }
    }
}
