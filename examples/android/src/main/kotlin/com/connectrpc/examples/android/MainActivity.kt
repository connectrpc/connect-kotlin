// Copyright 2022-2025 The Connect Authors
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

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var radioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        radioGroup = findViewById(R.id.protocol_selection_radio_group)
        setupProtocols()
    }

    private fun setupProtocols() {
        for (protocol in Protocols.values()) {
            for (connection in ChatConnection.values()) {
                val button = RadioButton(baseContext)
                button.setText("${protocol.display} - ${connection.display}")
                button.setOnClickListener {
                    val chatIntent = Intent(baseContext, ElizaChatActivity::class.java)
                    chatIntent.putExtra(PROTOCOL_KEY, protocol.display)
                    chatIntent.putExtra(CONNECTION_KEY, connection.display)
                    startActivity(chatIntent)
                }
                radioGroup.addView(button)
            }
        }
    }
}

const val PROTOCOL_KEY = "protocol"
const val CONNECTION_KEY = "connection"
enum class Protocols(val display: String) {
    CONNECT("Connect"),
    GRPC("GRPC"),
    GRPC_WEB("GRPC-Web"),
}

enum class ChatConnection(val display: String) {
    UNARY("unary"),
    BIDI_STREAMING("bidirectional streaming"),
}
