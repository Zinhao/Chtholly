package com.zinhao.chtholly.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.databinding.ActivityChatBinding
import com.zinhao.chtholly.utils.FileLogger
import com.zinhao.chtholly.view.adapter.AppChatAdapter
import com.zinhao.chtholly.view.adapter.PairAdapter
import com.zinhao.chtholly.viewmodel.ChatViewModel


class ChatActivity : AppCompatActivity() {
    private val TAG = "ChatActivity"
    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private var adapter: AppChatAdapter? = null
    companion object {
        val IS_FIRST_TIME = "is_first_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModel()
        initRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.loadMessages()
        if(intent.hasExtra(IS_FIRST_TIME)){
            binding.etInput.setText("接下来应该怎么做？")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            Menu.FIRST -> {  // 返回按钮的 ID
                viewModel.clearMessageContext()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }  // 或执行其他导航逻辑
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    private var needScroll = true

    private fun initRecyclerView() {
        adapter = AppChatAdapter(BotApp.getInstance().adminName) { message ->
            viewModel.resendMessage(message)
        }
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // 从底部开始显示
        binding.recyclerView.setLayoutManager(layoutManager)
        binding.recyclerView.setAdapter(adapter)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                needScroll = dy > 0
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!recyclerView.canScrollVertically(1) && isNearBottom()) {
                        needScroll = true
                    }else{
                        needScroll = false
                    }
                }
            }
        })
    }

    private var streamingPosition: Int = -1

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
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.autoCompleteArr.observe(this, {
            val pairs = it ?: return@observe
            val adapter = PairAdapter(this, pairs)
            binding.etInput.setAdapter(adapter)
            binding.etInput.setDropDownBackgroundResource(com.zinhao.chtholly.R.drawable.circle_shape_white)
            binding.etInput.setOnItemClickListener { parent, _, position, _ ->
                val selected = parent.getItemAtPosition(position) as Pair<*, *>
                binding.etInput.setText("/${selected.first.toString()}")
            }
        })

        // Observe streaming state
        viewModel.isStreaming.observe(this, { isStreaming ->
            binding.btnSend.setEnabled(!isStreaming)
            adapter?.setResendEnabled(!isStreaming)
        })

        viewModel.streamingMessage.observe(this, { message ->
            if (message != null) {
                // Add streaming message to list if not already present
                val currentList = viewModel.messages.value?.toMutableList() ?: return@observe
                if (streamingPosition == -1) {
                    currentList.add(message)
                    streamingPosition = currentList.size - 1
                    adapter?.submitList(currentList) {
                        binding.recyclerView.smoothScrollToPosition(streamingPosition)
                    }
                } else {
                    // Update existing streaming message directly on ViewHolder
                    adapter?.updateStreamingText(binding.recyclerView, streamingPosition, message.message)
                    if(needScroll){
                        binding.recyclerView.scrollToPosition(streamingPosition)
                    }
                }
            } else {
                if(streamingPosition!=-1){
                    adapter?.notifyItemChanged(streamingPosition)
                }
                // Streaming complete, reset position
                streamingPosition = -1
            }
        })
    }

    /**
     * 用户是否已经贴着列表底部：不在底部时不要抢走他的滚动位置
     */
    private fun isNearBottom(): Boolean {
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return true
        if (layoutManager.itemCount == 0) return true
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        return lastVisible >= 0 && lastVisible >= layoutManager.itemCount - 1
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener { v: View? ->
            val content = binding.etInput.text.toString().trim { it <= ' ' }
            if (!content.isEmpty()) {
                viewModel.sendMessage(BotApp.getInstance().adminName, content)
                binding.etInput.setText("")
                binding.etInput.hideKeyboard()
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

    // 定义扩展函数
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, Menu.FIRST, Menu.NONE, "clear")
        return true
    }
}