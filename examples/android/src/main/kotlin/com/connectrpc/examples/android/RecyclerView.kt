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

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class Adapter : RecyclerView.Adapter<ViewHolder>() {

    private val messages = mutableListOf<MessageData>()

    fun addAll(newMessages: List<MessageData>) {
        val previousSize = messages.size
        val newMessageSize = newMessages.size
        messages.addAll(newMessages)
        notifyItemRangeInserted(previousSize, previousSize + newMessageSize)
    }

    fun add(message: MessageData) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun clear() {
        messages.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val messageViewModel = messages[position]
        viewHolder.textView.setText(messageViewModel.message)
        val layoutParams = viewHolder.textView.layoutParams as LinearLayout.LayoutParams
        layoutParams.gravity = if (messageViewModel.isEliza) Gravity.LEFT else Gravity.RIGHT
        viewHolder.textView.layoutParams = layoutParams

        if (messageViewModel.isEliza) {
            viewHolder.senderNameTextView.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val senderNameTextView: TextView
    val textView: TextView

    init {
        textView = view.findViewById(R.id.message_text_view)
        senderNameTextView = view.findViewById(R.id.sender_name_text_view)
    }
}

data class MessageData(
    val message: String,
    val isEliza: Boolean,
)
