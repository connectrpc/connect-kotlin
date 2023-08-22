// Copyright 2022-2023 Buf Technologies, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.connectrpc.examples.android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.connectrpc.ProtocolClientConfig
import com.connectrpc.eliza.v1.ConverseRequest
import com.connectrpc.eliza.v1.ElizaServiceClient
import com.connectrpc.eliza.v1.SayRequest
import com.connectrpc.extensions.GoogleJavaLiteProtobufStrategy
import com.connectrpc.impl.ProtocolClient
import com.connectrpc.okhttp.ConnectOkHttpClient
import com.connectrpc.protocols.NetworkProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ElizaChatActivity : AppCompatActivity() {

    private lateinit var adapter: Adapter
    private lateinit var titleTextView: TextView
    private lateinit var editTextView: EditText
    private lateinit var buttonView: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eliza_chat)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        adapter = Adapter()
        recyclerView.adapter = adapter
        editTextView = findViewById(R.id.edit_text_view)
        titleTextView = findViewById(R.id.title_text_view)
        buttonView = findViewById<Button>(R.id.send_button)
        // Default question to ask as a pre-fill.
        editTextView.setText(R.string.edit_text_hint)
        adapter.clear()
        // Create an okhttp instance.
        val okhttpClient = OkHttpClient()
            .newBuilder()
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            .build()
        val host = "https://demo.connectrpc.com"
        val protocol = intent.getStringExtra(PROTOCOL_KEY)
        // Get the user selected protocol.
        val selectedNetworkProtocolOption = when (protocol) {
            Protocols.CONNECT.display -> {
                NetworkProtocol.CONNECT
            }

            Protocols.GRPC.display -> {
                NetworkProtocol.GRPC
            }

            Protocols.GRPC_WEB.display -> {
                NetworkProtocol.GRPC_WEB
            }

            else -> throw RuntimeException("unsupported protocol $protocol")
        }
        // Create a ProtocolClient.
        val client = ProtocolClient(
            httpClient = ConnectOkHttpClient(okhttpClient),
            ProtocolClientConfig(
                host = host,
                serializationStrategy = GoogleJavaLiteProtobufStrategy(),
                networkProtocol = selectedNetworkProtocolOption
            )
        )
        // Create the Eliza service client.
        val elizaServiceClient = ElizaServiceClient(client)
        // Setup the appropriate connection type.
        val connection = intent.getStringExtra(CONNECTION_KEY)
        when (connection) {
            ChatConnection.UNARY.display -> {
                setupUnaryChat(elizaServiceClient)
            }

            ChatConnection.BIDI_STREAMING.display -> {
                setupStreamingChat(elizaServiceClient)
            }

            else -> throw RuntimeException("unsupported connection type $connection")
        }
        titleTextView.setText("$protocol - $connection")
    }

    private fun setupUnaryChat(elizaServiceClient: ElizaServiceClient) {
        // Set up click listener to make a request to Eliza.
        buttonView.setOnClickListener {
            val sentence = editTextView.text.toString()
            adapter.add(MessageData(sentence, false))
            editTextView.setText("")
            // Ensure IO context for unary requests.
            lifecycleScope.launch(Dispatchers.IO) {
                // Make a unary request to Eliza.
                val response = elizaServiceClient.say(SayRequest.newBuilder().setSentence(sentence).build())
                response.success { success ->
                    // Get Eliza's reply from the response.
                    val elizaSentence = success.message.sentence
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (elizaSentence.isNotBlank()) {
                            adapter.add(MessageData(elizaSentence, true))
                        } else {
                            adapter.add(MessageData("...No response from Eliza...", true))
                        }
                    }
                }
            }
        }
    }

    private fun setupStreamingChat(elizaServiceClient: ElizaServiceClient) {
        // On stream result, this callback can be called multiple times.
        lifecycleScope.launch(Dispatchers.IO) {
            // Initialize a bidi stream with Eliza.
            val stream = elizaServiceClient.converse()

            for (streamResult in stream.resultChannel()) {
                streamResult.maybeFold(
                    onMessage = { result ->
                        // A stream message is received: Eliza has said something to us.
                        val elizaResponse = result.message.sentence
                        if (elizaResponse?.isNotBlank() == true) {
                            adapter.add(MessageData(elizaResponse, true))
                        } else {
                            // Something odd occurred.
                            adapter.add(MessageData("...No response from Eliza...", true))
                        }
                    },
                    onCompletion = {
                        // This should only be called once.
                        adapter.add(
                            MessageData(
                                "Session has ended.",
                                true
                            )
                        )
                    }
                )
            }
            lifecycleScope.launch(Dispatchers.Main) {
                buttonView.setOnClickListener {
                    val sentence = editTextView.text.toString()
                    adapter.add(MessageData(sentence, false))
                    editTextView.setText("")
                    // Send will be streaming a message to Eliza.
                    lifecycleScope.launch(Dispatchers.IO) {
                        stream.send(ConverseRequest.newBuilder().setSentence(sentence).build())
                    }
                }
            }
        }
    }
}
