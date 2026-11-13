package com.huanchengfly.tieba.post.api.models

import com.huanchengfly.tieba.post.api.retrofit.NullOnEmptyConverterFactory
import com.huanchengfly.tieba.post.api.retrofit.adapter.FlowCallAdapterFactory
import com.huanchengfly.tieba.post.api.retrofit.converter.gson.GsonConverterFactory
import com.huanchengfly.tieba.post.api.retrofit.interfaces.OfficialTiebaApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException

class FansRequestTest {
    private fun api(code: Int, body: String, inspect: (Request) -> Unit = {}): OfficialTiebaApi {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            inspect(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("test").body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return Retrofit.Builder().baseUrl("https://unused.invalid/")
            .addCallAdapterFactory(FlowCallAdapterFactory.create())
            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(GsonConverterFactory.create()).client(client).build()
            .create(OfficialTiebaApi::class.java)
    }

    @Test fun actualFansServiceUsesCorrectRequestAndNestedResponse(): Unit = runBlocking {
        val service = api(200, """{"error_code":"0","user_list":[{"id":"123","name":"a","name_show":"昵称","portrait":"p"}],"page":{"current_page":"2","total_count":"114","has_more":"1"}}""") { request ->
            assertEquals("https://tiebac.baidu.com/c/u/fans/page", request.url.toString())
            val form = request.body as FormBody
            val fields = (0 until form.size).associate { form.name(it) to form.value(it) }
            assertEquals("2", fields["pn"])
            assertEquals("42", fields["uid"])
        }
        val result = service.fansListFlow(2, 42, "22.13.1.0").first().toFollowListBean()
        assertEquals(114, result.totalFollowNum)
        assertEquals(123L, result.followList.single().id)
    }
    @Test fun httpFailurePreservesStatusInsteadOfNullPointer(): Unit = runBlocking {
        try { api(403, "{}").fansListFlow(1, 42, "22.13.1.0").first(); fail("Expected HTTP failure") }
        catch (error: HttpException) { assertEquals(403, error.code()) }
    }
    @Test fun emptyBodyHasAnExplicitFailure(): Unit = runBlocking {
        try { api(200, "").fansListFlow(1, 42, "22.13.1.0").first(); fail("Expected empty body failure") }
        catch (error: IOException) { assertTrue(error.message.orEmpty().contains("空响应")) }
    }
}
