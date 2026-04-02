package com.zinhao.chtholly.view

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.zinhao.chtholly.databinding.ActivityChatBinding
import com.zinhao.chtholly.view.adapter.AppChatAdapter
import com.zinhao.chtholly.viewmodel.ChatViewModel


class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private var adapter: AppChatAdapter? = null

    private val CURRENT_USER = "Me"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModel()
        initRecyclerView()
        setupObservers()
        setupListeners()
        
        viewModel.loadMessages()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    private fun initRecyclerView() {
        adapter = AppChatAdapter(CURRENT_USER)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.setStackFromEnd(true) // 从底部开始显示
        binding.recyclerView.setLayoutManager(layoutManager)
        binding.recyclerView.setAdapter(adapter)
    }

    private fun setupObservers() {
        // 观察消息列表变化
        viewModel.messages.observe(this, { messages ->
            adapter?.submitList(messages)
            if (messages != null && !messages.isEmpty()) {
                binding.recyclerView.smoothScrollToPosition(messages.size - 1)
            }
        })

        // 观察加载状态
        viewModel.isMessageDialogReady.observe(this, { isLoading ->
            binding.btnSend.setEnabled(!isLoading)
            binding.progressBar.setVisibility(if (isLoading) View.VISIBLE else View.GONE)
        })
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener { v: View? ->
            val content = binding.etInput.text.toString().trim { it <= ' ' }
            if (!content.isEmpty()) {
                viewModel.sendMessage(CURRENT_USER, content)
                binding.etInput.setText("")
            }
        }

        binding.etInput.setOnEditorActionListener(OnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.btnSend.performClick()
                return@OnEditorActionListener true
            }
            false
        })
    }
}