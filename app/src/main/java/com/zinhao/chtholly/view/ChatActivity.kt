package com.zinhao.chtholly.view

import android.R
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.databinding.ActivityChatBinding
import com.zinhao.chtholly.view.adapter.AppChatAdapter
import com.zinhao.chtholly.view.adapter.PairAdapter
import com.zinhao.chtholly.viewmodel.ChatViewModel


class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private var adapter: AppChatAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 在 Activity 的 onCreate 中
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)  // 显示返回箭头
        }

        initViewModel()
        initRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.loadMessages()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {  // 返回按钮的 ID
                finish()  // 或执行其他导航逻辑
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]
    }

    private fun initRecyclerView() {
        adapter = AppChatAdapter(BotApp.getInstance().adminName)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // 从底部开始显示
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
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.autoCompleteArr.observe(this, {
            Log.d("TAG", "pairs lem: ${it.size}")
            val pairs = it ?: return@observe
            val adapter = PairAdapter(this, pairs)
            binding.etInput.setAdapter(adapter)
            binding.etInput.setDropDownBackgroundResource(com.zinhao.chtholly.R.drawable.white_circle_shape)
            binding.etInput.setOnItemClickListener { parent, _, position, _ ->
                val selected = parent.getItemAtPosition(position) as Pair<*, *>
                binding.etInput.setText("/${selected.first.toString()}")
            }
        })
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
        menu?.add(0, Menu.FIRST, Menu.NONE, "run_info")
        return super.onCreateOptionsMenu(menu)
    }
}