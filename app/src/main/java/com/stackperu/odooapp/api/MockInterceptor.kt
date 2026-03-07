package com.stackperu.odooapp.api

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Interceptor used to mock Odoo 19 API JSON-RPC responses.
 */
class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val uri = chain.request().url().toString()
        val responseString: String

        if (uri.endsWith("/web/session/authenticate")) {
            // Mock Odoo 19 Login JSON-RPC Response
            responseString = """
                {
                    "jsonrpc": "2.0",
                    "result": {
                        "uid": 1,
                        "session_id": "mock_session_abc123",
                        "name": "Francisco",
                        "username": "francisco@stackperu.com"
                    }
                }
            """
        } else if (uri.endsWith("/web/dataset/search_read")) {
            // Mock Odoo 19 res.partner (Contacts) JSON-RPC Response
            responseString = """
                {
                    "jsonrpc": "2.0",
                    "result": {
                        "length": 4,
                        "records": [
                            {
                                "id": 1,
                                "name": "Juan Perez",
                                "email": "juan@example.com",
                                "phone": "+51 987654321",
                                "vat": "10456789123",
                                "address": "Av. Larco 123, Miraflores",
                                "avatar": "https://i.pravatar.cc/150?u=1"
                            },
                            {
                                "id": 2,
                                "name": "Maria Garcia",
                                "email": "maria@example.com",
                                "phone": "+51 912345678",
                                "vat": "10789456123",
                                "address": "Calle Las Flores 456, San Isidro",
                                "avatar": "https://i.pravatar.cc/150?u=2"
                            },
                            {
                                "id": 3,
                                "name": "Carlos Ruiz",
                                "email": "carlos@example.com",
                                "phone": "+51 998877665",
                                "vat": "20123456789",
                                "address": "Av. Arequipa 789, Lince",
                                "avatar": "https://i.pravatar.cc/150?u=3"
                            },
                            {
                                "id": 4,
                                "name": "Ana Torres",
                                "email": "ana@example.com",
                                "phone": "+51 987123654",
                                "vat": "20987654321",
                                "address": "Jr. de la Union 101, Lima",
                                "avatar": "https://i.pravatar.cc/150?u=4"
                            }
                        ]
                    }
                }
            """
        } else {
            responseString = "{}"
        }

        return Response.Builder()
            .code(200)
            .message(responseString)
            .request(chain.request())
            .protocol(Protocol.HTTP_1_0)
            .body(ResponseBody.create(MediaType.parse("application/json"), responseString))
            .addHeader("content-type", "application/json")
            // In a real Odoo 19 environment, you'd receive a Set-Cookie: session_id=... header here
            .addHeader("Set-Cookie", "session_id=mock_session_abc123; Path=/; HttpOnly")
            .build()
    }
}